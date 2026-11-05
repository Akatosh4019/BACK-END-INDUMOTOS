const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const repo = path.resolve(__dirname, '../../..');
const config = Object.fromEntries(fs.readFileSync(path.join(repo, '.env'), 'utf8').split(/\r?\n/)
  .filter(line => /^[A-Z_]+=/.test(line)).map(line => {
    const i = line.indexOf('=');
    return [line.slice(0, i), line.slice(i + 1).replace(/^["']|["']$/g, '')];
  }));
const base = process.env.BASE_URL || 'http://127.0.0.1:8030';
const runId = Date.now();
const password = `T04!${Math.random().toString(36).slice(2)}Ab`;
const users = [];
let arrivalId;
const results = [];
let errorExample;

async function request(method, route, token, body) {
  const response = await fetch(base + route, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: response.status, data: await response.json().catch(() => null) };
}

function check(name, actual, expected, field) {
  const hasField = !field || (actual.data?.campos?.includes(field) && actual.data?.errores?.[field]);
  const passed = actual.status === expected && Boolean(hasField);
  results.push({ caso: name, esperado: expected, obtenido: actual.status,
    campo: field || null, paso: passed });
  if (!passed) throw new Error(`${name}: esperado ${expected} / ${field || '-'}, obtenido ${actual.status} ${JSON.stringify(actual.data)}`);
}

function countFichas() {
  const sql = `SELECT count(*) FROM motocicleta WHERE id_llegada = ${Number(arrivalId)};`;
  const output = execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-t', '-A',
    '-v', 'ON_ERROR_STOP=1', '-U', config.DB_USER, '-d', 'indumotos', '-c', sql],
    { cwd: repo, encoding: 'utf8' });
  return Number(output.trim());
}

async function main() {
  const admin = await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: config.ADMIN_USERNAME, clave: config.ADMIN_PASSWORD });
  if (admin.status !== 200) throw new Error(`Login ADMIN: ${admin.status}`);
  async function makeUser(role) {
    const name = `t04_${role.toLowerCase()}_${runId}`;
    const created = await request('POST', '/api/v1/usuarios', admin.data.accessToken,
      { nombreCompleto: `Prueba HU-02-T04 ${role}`, nombreUsuario: name,
        clave: password, rolCodigo: role });
    if (created.status !== 201) throw new Error(`Crear ${role}: ${created.status}`);
    users.push(created.data.id);
    const login = await request('POST', '/api/v1/auth/login', null,
      { nombreUsuario: name, clave: password });
    if (login.status !== 200) throw new Error(`Login ${role}: ${login.status}`);
    return login.data.accessToken;
  }
  const storeToken = await makeUser('ALMACEN');
  const inventoryToken = await makeUser('INVENTARIOS');
  const arrival = await request('POST', '/api/v1/llegadas', storeToken,
    { numeroGuia: `T04-FICHA-${runId}`, cantidadDeclarada: 1,
      cantidadRecibida: 1, observacion: 'Datos ficticios HU-02-T04' });
  if (arrival.status !== 201) throw new Error(`Crear llegada ficticia: ${arrival.status}`);
  arrivalId = arrival.data.id;
  const valid = { llegadaId: arrivalId, marca: 'RONCO', modelo: 'RZ 250', color: 'ROJO',
    chasisCompleto: `T04-CHASIS-${runId}`, numeroMotor: `T04-MOTOR-${runId}`,
    dua: '000123', itemDua: '004' };
  check('Ficha completa', await request('POST', '/api/v1/motocicletas', inventoryToken, valid), 201);
  const fields = ['marca', 'modelo', 'color', 'chasisCompleto', 'numeroMotor', 'dua', 'itemDua'];
  for (const field of fields) {
    const omitted = { ...valid };
    delete omitted[field];
    const actual = await request('POST', '/api/v1/motocicletas', inventoryToken, omitted);
    check(`${field} omitido`, actual, 400, field);
    if (field === 'marca') errorExample = { solicitud: 'POST /api/v1/motocicletas',
      codigo: actual.status, cuerpo: actual.data };
    check(`${field} nulo`, await request('POST', '/api/v1/motocicletas', inventoryToken,
      { ...valid, [field]: null }), 400, field);
    check(`${field} en blanco`, await request('POST', '/api/v1/motocicletas', inventoryToken,
      { ...valid, [field]: '   ' }), 400, field);
  }
  check('Llegada omitida', await request('POST', '/api/v1/motocicletas', inventoryToken,
    { ...valid, llegadaId: null }), 400, 'llegadaId');
  check('Sin autenticación', await request('POST', '/api/v1/motocicletas', null, valid), 401);
  const count = countFichas();
  if (count !== 1) throw new Error(`Se esperó 1 ficha válida, encontradas ${count}`);
  const summary = { gateway: base, solicitudes: results.length,
    aprobadas: results.filter(r => r.paso).length, fallidas: results.filter(r => !r.paso).length,
    fichasValidasGuardadas: count, fichasInvalidasGuardadas: 0,
    ejemploRespuesta400: errorExample, resultados: results };
  fs.writeFileSync(path.join(__dirname, 'resultado-http.json'), JSON.stringify(summary, null, 2) + '\n');
  console.log(`HU-02-T04: ${summary.aprobadas}/${summary.solicitudes} casos; 0 fichas inválidas guardadas.`);
}

main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => {
  if (!users.length) return;
  const sql = `BEGIN; DELETE FROM motocicleta WHERE id_llegada = ${Number(arrivalId || 0)}; ` +
    `DELETE FROM llegada WHERE id_llegada = ${Number(arrivalId || 0)}; ` +
    `DELETE FROM usuario_permiso WHERE id_usuario IN (${users.map(Number).join(',')}); ` +
    `DELETE FROM usuario WHERE id_usuario IN (${users.map(Number).join(',')}); COMMIT;`;
  try {
    execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-v', 'ON_ERROR_STOP=1',
      '-U', config.DB_USER, '-d', 'indumotos', '-c', sql], { cwd: repo, stdio: 'ignore' });
    console.log('Datos ficticios retirados.');
  } catch (error) { console.error('Error al retirar datos ficticios:', error.message); process.exitCode = 1; }
});
