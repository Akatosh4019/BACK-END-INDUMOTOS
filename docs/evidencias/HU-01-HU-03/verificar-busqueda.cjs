const crypto = require('node:crypto');

const base = process.env.BASE_URL || 'http://127.0.0.1:8080';
const adminUsername = process.env.ADMIN_USERNAME;
const adminPassword = process.env.ADMIN_PASSWORD;
if (!adminUsername || !adminPassword) throw new Error('Define ADMIN_USERNAME y ADMIN_PASSWORD');

const runId = Date.now();
const password = `Prueba!${crypto.randomBytes(12).toString('hex')}Aa`;
const results = [];

async function request(method, path, token, body) {
  const response = await fetch(base + path, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  return { status: response.status, body: await response.json().catch(() => null) };
}

function check(name, response, status, predicate = () => true) {
  const pass = response.status === status && predicate(response.body);
  results.push({ caso: name, esperado: status, obtenido: response.status, paso: pass });
  if (!pass) throw new Error(`${name}: ${response.status} ${JSON.stringify(response.body)}`);
  return response.body;
}

async function login(username, clave) {
  return check(`Login ${username}`, await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: username, clave }), 200, body => Boolean(body?.accessToken)).accessToken;
}

async function main() {
  const admin = await login(adminUsername, adminPassword);
  async function user(role) {
    const username = `web_${role.toLowerCase()}_${runId}`;
    check(`Crear ${role}`, await request('POST', '/api/v1/usuarios', admin,
      { nombreCompleto: `Prueba web ${role}`, nombreUsuario: username,
        clave: password, rolCodigo: role }), 201);
    return login(username, password);
  }
  const almacen = await user('ALMACEN');
  const inventarios = await user('INVENTARIOS');
  const comercial = await user('COMERCIAL');
  const llegada = check('Registrar llegada', await request('POST', '/api/v1/llegadas', almacen,
    { numeroGuia: `WEB-${runId}`, cantidadDeclarada: 2, cantidadRecibida: 2 }), 201,
  body => Number.isInteger(body?.id));

  for (const [prefix, color] of [['A', 'ROJO'], ['B', 'AZUL']]) {
    check(`Ficha ${prefix}`, await request('POST', '/api/v1/motocicletas', inventarios,
      { llegadaId: llegada.id, marca: 'RONCO', modelo: 'RZ 250', color,
        chasisCompleto: `WEB-${prefix}-${runId}-0042`, numeroMotor: `M-${prefix}-${runId}`,
        dua: '000123', itemDua: '004' }), 201);
  }

  const sufijo = check('Dos coincidencias por sufijo',
    await request('GET', '/api/v1/motocicletas?chasis=0042&pagina=0&tamano=10', inventarios),
    200, body => body?.totalElementos === 2 && body.items?.length === 2);
  check('Abrir ficha desde resultados', await request('GET',
    `/api/v1/motocicletas/${sufijo.items[0].id}`, inventarios), 200,
  body => body?.chasisCompleto?.endsWith('0042'));
  check('Chasis completo', await request('GET',
    `/api/v1/motocicletas?chasis=WEB-A-${runId}-0042`, inventarios), 200,
  body => body?.totalElementos === 1);
  check('Filtros combinados', await request('GET',
    '/api/v1/motocicletas?chasis=0042&marca=RONCO&color=ROJO', inventarios), 200,
  body => body?.totalElementos === 1 && body.items[0].color === 'ROJO');
  check('Sin coincidencias', await request('GET',
    '/api/v1/motocicletas?marca=INEXISTENTE', inventarios), 200,
  body => body?.totalElementos === 0 && body.items?.length === 0);
  check('Sufijo inválido', await request('GET',
    '/api/v1/motocicletas?chasis=042', inventarios), 400);
  check('Sin sesión', await request('GET', '/api/v1/motocicletas?chasis=0042'), 401);
  check('Sin permiso', await request('GET',
    '/api/v1/motocicletas?chasis=0042', comercial), 403);
  console.log(JSON.stringify({ casos: results.length, aprobados: results.filter(r => r.paso).length,
    resultados: results }, null, 2));
}

main().catch(error => { console.error(error); process.exitCode = 1; });
