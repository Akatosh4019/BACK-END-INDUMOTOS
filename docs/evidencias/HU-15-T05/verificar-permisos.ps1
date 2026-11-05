$ErrorActionPreference = 'Stop'
$envValues = @{}
Get-Content -LiteralPath '.env' | ForEach-Object {
    if ($_ -match '^([A-Z_]+)=(.*)$') { $envValues[$Matches[1]] = $Matches[2].Trim('"', "'") }
}
$base = 'http://127.0.0.1:8080'
$results = [System.Collections.Generic.List[string]]::new()
function Send($method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    $args = @{ Uri="$base$path"; Method=$method; Headers=$headers; SkipHttpErrorCheck=$true }
    if ($body) { $args.ContentType = 'application/json'; $args.Body = ($body | ConvertTo-Json -Compress) }
    $response = Invoke-WebRequest @args
    $data = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
    return @{ status=[int]$response.StatusCode; data=$data }
}
function Check($name, $actual, $expected) {
    if ($actual -ne $expected) { throw "$name`: HTTP $actual, esperado $expected" }
    $results.Add("$name`: $actual")
}
function Effective($response, $code) {
    return [bool](($response.data.permisos | Where-Object codigo -eq $code).permitido)
}
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$almacenName = "hu15_almacen_$suffix"
$inventarioName = "hu15_inventarios_$suffix"
$password = 'ClaveFicticia_HU15_2026!'
$almacenId = $null; $inventarioId = $null; $llegadaId = $null
try {
    Check 'API activa' (Send GET '/api/health' $null $null).status 200
    $login = Send POST '/api/v1/auth/login' $null @{nombreUsuario=$envValues.ADMIN_USERNAME;clave=$envValues.ADMIN_PASSWORD}
    Check 'Login administrador' $login.status 200
    $adminToken = $login.data.accessToken
    $llegada = @{numeroGuia="HU15-PRUEBA-$suffix";cantidadDeclarada=1;cantidadRecibida=1;observacion='Datos ficticios'}
    Check 'ADMIN sin permiso de llegada' (Send POST '/api/v1/llegadas' $adminToken $llegada).status 403
    $almacen = Send POST '/api/v1/usuarios' $adminToken @{nombreCompleto='Almacén HU15';nombreUsuario=$almacenName;clave=$password;rolCodigo='ALMACEN'}
    Check 'Crear ALMACEN' $almacen.status 201
    $almacenId = $almacen.data.id
    $inventario = Send POST '/api/v1/usuarios' $adminToken @{nombreCompleto='Inventarios HU15';nombreUsuario=$inventarioName;clave=$password;rolCodigo='INVENTARIOS'}
    Check 'Crear INVENTARIOS' $inventario.status 201
    $inventarioId = $inventario.data.id
    $almacenLogin = Send POST '/api/v1/auth/login' $null @{nombreUsuario=$almacenName;clave=$password}
    Check 'Login ALMACEN' $almacenLogin.status 200
    $inventarioLogin = Send POST '/api/v1/auth/login' $null @{nombreUsuario=$inventarioName;clave=$password}
    Check 'Login INVENTARIOS' $inventarioLogin.status 200
    $almacenToken = $almacenLogin.data.accessToken
    $inventarioToken = $inventarioLogin.data.accessToken
    $ruta = "/api/v1/usuarios/$inventarioId/permisos"
    Check 'Consulta permisos sin sesión' (Send GET $ruta $null $null).status 401
    Check 'Consulta permisos sin autorización' (Send GET $ruta $almacenToken $null).status 403
    $actual = Send GET $ruta $adminToken $null
    Check 'Consulta permisos ADMIN' $actual.status 200
    if (-not (Effective $actual 'FICHAS_REGISTRAR')) { throw 'Rol INVENTARIOS no concede FICHAS_REGISTRAR' }
    Check 'Consulta ficha con rol autorizado' (Send GET '/api/v1/motocicletas/99999999' $inventarioToken $null).status 404
    $denegar = Send PUT "$ruta/FICHAS_REGISTRAR" $adminToken @{decision='DENEGAR'}
    Check 'Guardar DENEGAR' $denegar.status 200
    if (Effective $denegar 'FICHAS_REGISTRAR') { throw 'DENEGAR no prevaleció' }
    Check 'Ficha rechazada tras DENEGAR' (Send POST '/api/v1/motocicletas' $inventarioToken @{}).status 403
    $permitir = Send PUT "$ruta/LLEGADAS_REGISTRAR" $adminToken @{decision='PERMITIR'}
    Check 'Guardar PERMITIR' $permitir.status 200
    if (-not (Effective $permitir 'LLEGADAS_REGISTRAR')) { throw 'PERMITIR no habilitó la acción' }
    $nuevaLlegada = Send POST '/api/v1/llegadas' $inventarioToken $llegada
    Check 'Llegada permitida por excepción' $nuevaLlegada.status 201
    $llegadaId = $nuevaLlegada.data.id
    $rol = Send PUT "/api/v1/usuarios/$inventarioId/rol" $adminToken @{rolCodigo='ALMACEN'}
    Check 'Cambio de rol' $rol.status 200
    if ($rol.data.rolCodigo -ne 'ALMACEN') { throw 'El rol no cambió' }
    Check 'Ficha rechazada tras cambio de rol' (Send POST '/api/v1/motocicletas' $inventarioToken @{}).status 403
    Check 'Retirar DENEGAR' (Send DELETE "$ruta/FICHAS_REGISTRAR" $adminToken $null).status 200
    $restaurado = Send PUT "/api/v1/usuarios/$inventarioId/rol" $adminToken @{rolCodigo='INVENTARIOS'}
    Check 'Restaurar rol' $restaurado.status 200
    if (-not (Effective $restaurado 'FICHAS_REGISTRAR')) { throw 'El permiso del rol no se restauró' }
    $retirado = Send DELETE "$ruta/LLEGADAS_REGISTRAR" $adminToken $null
    Check 'Retirar PERMITIR' $retirado.status 200
    if (Effective $retirado 'LLEGADAS_REGISTRAR') { throw 'El permiso individual persiste' }
    Check 'Llegada rechazada tras retirar PERMITIR' (Send POST '/api/v1/llegadas' $inventarioToken $llegada).status 403
    Check 'No conceder creación de cuentas a INVENTARIOS' (Send PUT "$ruta/USUARIOS_CREAR" $adminToken @{decision='PERMITIR'}).status 403
    $results | ForEach-Object { $_ }
    'RESULTADO: TODAS LAS COMPROBACIONES PASARON'
} finally {
    if ($almacenId -or $inventarioId) {
        $ids = @($almacenId, $inventarioId) | Where-Object { $_ } | ForEach-Object { [long]$_ }
        $sql = "DELETE FROM usuario_permiso WHERE id_usuario IN ($($ids -join ',')); "
        if ($llegadaId) { $sql += "DELETE FROM llegada WHERE id_llegada=$([long]$llegadaId); " }
        $sql += "DELETE FROM usuario WHERE id_usuario IN ($($ids -join ','));"
        docker compose exec -T db psql -q -U $envValues.DB_USER -d indumotos -c $sql | Out-Null
    }
}
