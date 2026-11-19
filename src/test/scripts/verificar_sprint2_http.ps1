param(
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 55440,
    [string]$DatabaseUser = 'indumotos',
    [string]$DatabasePassword = '',
    [string]$DatabaseName = 's2_http_verify',
    [string]$JavaExe = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.3\jbr\bin\java.exe',
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..\..'))
if ($DatabaseName -notmatch '^[a-z][a-z0-9_]*$') { throw 'Nombre de base de prueba inválido' }
$env:PGPASSWORD = $DatabasePassword
$exists = & (Join-Path $PostgresBin 'psql.exe') -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='$DatabaseName'"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar PostgreSQL' }
if (!$exists) {
    & (Join-Path $PostgresBin 'createdb.exe') -h $DatabaseHost -p $DatabasePort -U $DatabaseUser $DatabaseName
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de prueba' }
}

$xmlFile = Get-ChildItem target\surefire-reports -Filter 'TEST-*.xml' | Select-Object -First 1
if (!$xmlFile) { throw 'Ejecute mvn test antes de esta prueba para crear el classpath' }
[xml]$xml = Get-Content $xmlFile.FullName
$classpath = $xml.testsuite.properties.property | Where-Object name -eq 'java.class.path' | Select-Object -ExpandProperty value
if (!$classpath) { throw 'No se encontró el classpath de Maven' }

$suffix = [guid]::NewGuid().ToString('N').Substring(0, 8)
$admin = "s2admin_$suffix"
$commercial = "s2com_$suffix"
    $warehouse = "s2alm_$suffix"
    $inventory = "s2inv_$suffix"
    $assembly = "s2ens_$suffix"
$password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(18))
$env:DB_URL = "jdbc:postgresql://${DatabaseHost}:${DatabasePort}/${DatabaseName}"
$env:DB_USER = $DatabaseUser
$env:DB_PASSWORD = $DatabasePassword
$env:ADMIN_USERNAME = $admin
$env:ADMIN_PASSWORD = $password
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:PORT = '18080'
$process = $null
try {
    $process = Start-Process -FilePath $JavaExe -ArgumentList @('-cp', ('"' + $classpath + '"'), 'pe.indumotos.BackendIndumotosApplication') -PassThru -WindowStyle Hidden
    $base = 'http://127.0.0.1:18080'
    $ready = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try { $ready = (Invoke-WebRequest "$base/api/health" -SkipHttpErrorCheck -TimeoutSec 2).StatusCode -eq 200 } catch { }
        if ($ready) { break }
        if ($process.HasExited) { throw "El backend terminó con código $($process.ExitCode)" }
    }
    if (!$ready) { throw 'El backend no respondió en el puerto de pruebas' }

    function Request($method, $path, $token, $body) {
        $headers = @{}
        if ($token) { $headers.Authorization = "Bearer $token" }
        $options = @{ Uri = "$base$path"; Method = $method; Headers = $headers; SkipHttpErrorCheck = $true; TimeoutSec = 8 }
        if ($null -ne $body) { $options.ContentType = 'application/json'; $options.Body = ($body | ConvertTo-Json -Depth 8 -Compress) }
        $response = Invoke-WebRequest @options
        return @{ status = [int]$response.StatusCode; body = $(if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }) }
    }
    function Expect($actual, $expected, $step) {
        if ($actual -ne $expected) { throw "$step devolvió $actual; se esperaba $expected" }
    }

    Expect (Request 'GET' '/api/v1/solicitudes' $null $null).status 401 'Consulta sin JWT'
    $login = Request 'POST' '/api/v1/auth/login' $null @{ nombreUsuario = $admin; clave = $password }
    Expect $login.status 200 'Login administrador'
    $adminToken = $login.body.accessToken
    $sede = Request 'POST' '/api/v1/ubicaciones' $adminToken @{
        codigo = "S2_HTTP_$suffix"; nombre = 'Tienda de integración'; tipo = 'TIENDA'
    }
    Expect $sede.status 201 'Crear sede'
    $created = Request 'POST' '/api/v1/usuarios' $adminToken @{
        nombreCompleto = 'Comercial de integración'; nombreUsuario = $commercial;
        clave = $password; rolCodigo = 'COMERCIAL'
    }
    Expect $created.status 201 'Crear usuario comercial'
    $commercialLogin = Request 'POST' '/api/v1/auth/login' $null @{ nombreUsuario = $commercial; clave = $password }
    Expect $commercialLogin.status 200 'Login comercial'
    $commercialToken = $commercialLogin.body.accessToken
    foreach ($actor in @(@{ name=$warehouse; role='ALMACEN' },
                        @{ name=$inventory; role='INVENTARIOS' },
                        @{ name=$assembly; role='ENSAMBLAJE' })) {
        Expect (Request 'POST' '/api/v1/usuarios' $adminToken @{
            nombreCompleto = 'Usuario de integración'; nombreUsuario = $actor.name;
            clave = $password; rolCodigo = $actor.role
        }).status 201 "Crear $($actor.role)"
    }
    $warehouseToken = (Request 'POST' '/api/v1/auth/login' $null @{
        nombreUsuario=$warehouse; clave=$password
    }).body.accessToken
    $inventoryToken = (Request 'POST' '/api/v1/auth/login' $null @{
        nombreUsuario=$inventory; clave=$password
    }).body.accessToken
    $assemblyToken = (Request 'POST' '/api/v1/auth/login' $null @{
        nombreUsuario=$assembly; clave=$password
    }).body.accessToken
    Expect (Request 'POST' '/api/v1/ubicaciones' $commercialToken @{
        codigo = "S2_DENY_$suffix"; nombre = 'Sede prohibida'; tipo = 'TIENDA'
    }).status 403 'Comercial sin permiso de crear sedes'

    $newRequest = @{ destinoId = $sede.body.id; lineas = @(@{
        marca = 'Marca ensayo'; modelo = 'Modelo ensayo'; color = 'Rojo'; cantidad = 1
    }) }
    $solicitud = Request 'POST' '/api/v1/solicitudes' $commercialToken $newRequest
    Expect $solicitud.status 201 'Crear solicitud'
    $id = $solicitud.body.id
    Expect (Request 'GET' "/api/v1/solicitudes/$id/seguimiento" $commercialToken $null).status 200 'Consultar historial'
    Expect (Request 'POST' "/api/v1/solicitudes/$id/seguimiento" $commercialToken @{
        estado = 'EN_PREPARACION'
    }).status 403 'Comercial sin permiso de avanzar'
    Expect (Request 'POST' "/api/v1/solicitudes/$id/seguimiento" $adminToken @{
        estado = 'EN_PREPARACION'
    }).status 200 'Iniciar preparación'
    $arrival = Request 'POST' '/api/v1/llegadas' $warehouseToken @{
        numeroGuia="S2_HTTP_$suffix"; cantidadDeclarada=1; cantidadRecibida=1
    }
    Expect $arrival.status 201 'Registrar llegada del Sprint 1'
    $motorcycle = Request 'POST' '/api/v1/motocicletas' $inventoryToken @{
        llegadaId=$arrival.body.id; marca='Marca ensayo'; modelo='Modelo ensayo'; color='Rojo';
        chasisCompleto="S2_CHASIS_$suffix"; numeroMotor="S2_MOTOR_$suffix";
        dua="S2_DUA_$suffix"; itemDua='1'
    }
    Expect $motorcycle.status 201 'Registrar ficha del Sprint 1'
    $motorcycleId = $motorcycle.body.id
    $original = Request 'POST' "/api/v1/motocicletas/$motorcycleId/ubicacion" $warehouseToken @{
        destinoId=$sede.body.id
    }
    Expect $original.status 201 'Registrar ubicación conocida'
    $dispatch = Request 'POST' '/api/v1/ubicaciones' $adminToken @{
        codigo="S2_DES_$suffix"; nombre='Despacho de integración'; tipo='DESPACHO'
    }
    Expect $dispatch.status 201 'Crear destino de despacho'
    Expect (Request 'POST' "/api/v1/motocicletas/$motorcycleId/traslados" $warehouseToken @{
        destinoId=$dispatch.body.id
    }).status 201 'Trasladar unidad a despacho'
    $history = Request 'GET' "/api/v1/motocicletas/$motorcycleId/ubicacion/historial" $warehouseToken $null
    Expect $history.body.Count 2 'Historial de ubicaciones conservado'
    $lineId = $solicitud.body.lineas[0].id
    $assigned = Request 'POST' "/api/v1/solicitudes/$id/lineas/$lineId/unidades" $assemblyToken @{
        motocicletaId=$motorcycleId
    }
    Expect $assigned.status 201 'Asociar unidad a solicitud'
    Expect (Request 'POST' "/api/v1/solicitudes/$id/lineas/$lineId/unidades" $assemblyToken @{
        motocicletaId=$motorcycleId
    }).status 409 'No duplicar asignación'
    Expect (Request 'POST' "/api/v1/solicitudes/$id/seguimiento" $adminToken @{
        estado='RECHAZADA'; motivo='Sin stock adicional'
    }).status 200 'Rechazar y liberar asignaciones'
    $remaining = Request 'GET' "/api/v1/solicitudes/$id/lineas/$lineId/unidades" $assemblyToken $null
    Expect $remaining.body.Count 0 'Asignaciones activas liberadas'
    Expect (Request 'POST' "/api/v1/solicitudes/$id/anulacion" $commercialToken @{
        motivo = 'Ya no se necesita'
    }).status 409 'Anulación fuera de estado pendiente'

    $segunda = Request 'POST' '/api/v1/solicitudes' $commercialToken $newRequest
    Expect $segunda.status 201 'Crear segunda solicitud'
    Expect (Request 'POST' "/api/v1/solicitudes/$($segunda.body.id)/anulacion" $commercialToken @{
        motivo = 'Pedido duplicado'
    }).status 200 'Anular solicitud pendiente'
    $detail = Request 'GET' "/api/v1/solicitudes/$($segunda.body.id)" $commercialToken $null
    Expect $detail.body.estado 'ANULADA' 'Estado persistido de la anulación'
    Expect (Request 'POST' "/api/v1/solicitudes/$($segunda.body.id)/anulacion" $commercialToken @{
        motivo = 'Segundo intento'
    }).status 409 'Bloqueo de doble anulación'
    Write-Output 'PASS: Flyway, JWT 401, permisos 403, llegadas/fichas Sprint 1, ubicación, traslado, solicitud, asignación exclusiva, rechazo con liberación y anulación persistida.'
    Write-Output "Base aislada: $DatabaseName. Los datos creados son exclusivamente de prueba."
}
finally {
    if ($process -and !$process.HasExited) { Stop-Process -Id $process.Id -Force }
    Remove-Item Env:JWT_SECRET_BASE64, Env:ADMIN_PASSWORD, Env:DB_PASSWORD, Env:PGPASSWORD -ErrorAction SilentlyContinue
}
