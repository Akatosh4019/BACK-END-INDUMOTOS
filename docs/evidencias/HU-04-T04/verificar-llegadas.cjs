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
const username = `t04_almacen_${runId}`;
const password = `T04!${Math.random().toString(36).slice(2)}Ab`;
const guide = `T04-VALID-${runId}`;
const results = [];
let userId;
let errorExample;

async function request(method, route, token, body, raw = false) {
  const response = await fetch(base + route, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body === undefined ? undefined : raw ? body : JSON.stringify(body),
  });
  const data = await response.json().catch(() => null);
  return { status: response.status, data };
}

function check(name, actual, expected, field) {
  const hasField = !field || (actual.data?.campos?.includes(field) && actual.data?.errores?.[field]);
  const passed = actual.status === expected && Boolean(hasField);
  results.push({ caso: name, esperado: expected, obtenido: actual.status, campo: field || null, paso: passed });
  if (!passed) throw new Error(`${name}: esperado ${expected} / ${field || '-'}, obtenido ${actual.status}`);
}

function countArrivals() {
  const sql = `SELECT count(*) FROM llegada WHERE id_receptor = ${Number(userId)};`;
  const output = execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-t', '-A',
    '-v', 'ON_ERROR_STOP=1', '-U', config.DB_USER, '-d', 'indumotos', '-c', sql],
    { cwd: repo, encoding: 'utf8' });
  return Number(output.trim());
}

async function main() {
  const admin = await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: config.ADMIN_USERNAME, clave: config.ADMIN_PASSWORD });
  if (admin.status !== 200) throw new Error(`Login ADMIN: ${admin.status}`);
  const created = await request('POST', '/api/v1/usuarios', admin.data.accessToken,
    { nombreCompleto: 'Prueba validación T04', nombreUsuario: username,
      clave: password, rolCodigo: 'ALMACEN' });
  if (created.status !== 201) throw new Error(`Crear ALMACEN: ${created.status}`);
  userId = created.data.id;
  const login = await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: username, clave: password });
  if (login.status !== 200) throw new Error(`Login ALMACEN: ${login.status}`);
  const token = login.data.accessToken;
  if (countArrivals() !== 0) throw new Error('La cuenta ficticia ya tiene llegadas');

  const valid = { numeroGuia: guide, cantidadDeclarada: 2, cantidadRecibida: 1, observacion: '' };
  check('Caso válido', await request('POST', '/api/v1/llegadas', token, valid), 201);
  const cases = [
    ['Guía omitida', { cantidadDeclarada: 2, cantidadRecibida: 1 }, 'numeroGuia'],
    ['Guía nula', { ...valid, numeroGuia: null }, 'numeroGuia'],
    ['Guía en blanco', { ...valid, numeroGuia: '   ' }, 'numeroGuia'],
    ['Cantidad declarada omitida', { numeroGuia: guide, cantidadRecibida: 1 }, 'cantidadDeclarada'],
    ['Cantidad declarada nula', { ...valid, cantidadDeclarada: null }, 'cantidadDeclarada'],
    ['Cantidad declarada cero', { ...valid, cantidadDeclarada: 0 }, 'cantidadDeclarada'],
    ['Cantidad declarada negativa', { ...valid, cantidadDeclarada: -1 }, 'cantidadDeclarada'],
    ['Cantidad declarada fraccionaria', { ...valid, cantidadDeclarada: 1.5 }, 'cantidadDeclarada'],
    ['Cantidad recibida omitida', { numeroGuia: guide, cantidadDeclarada: 2 }, 'cantidadRecibida'],
    ['Cantidad recibida cero', { ...valid, cantidadRecibida: 0 }, 'cantidadRecibida'],
    ['Cantidad recibida no numérica', { ...valid, cantidadRecibida: 'abc' }, 'cantidadRecibida'],
  ];
  for (const [name, body, field] of cases) {
    const actual = await request('POST', '/api/v1/llegadas', token, body);
    check(name, actual, 400, field);
    if (name === 'Guía en blanco') errorExample = { solicitud: 'POST /api/v1/llegadas',
      codigo: actual.status, cuerpo: actual.data };
  }
  check('Sin autenticación', await request('POST', '/api/v1/llegadas', null, valid), 401);
  check('JSON malformado', await request('POST', '/api/v1/llegadas', token, '{', true), 400);
  const persisted = countArrivals();
  if (persisted !== 1) throw new Error(`Se esperó 1 llegada válida, encontradas ${persisted}`);
  const summary = { gateway: base, solicitudes: results.length, aprobadas: results.filter(r => r.paso).length,
    fallidas: results.filter(r => !r.paso).length, llegadasValidasGuardadas: persisted,
    llegadasInvalidasGuardadas: 0, ejemploRespuesta400: errorExample, resultados: results };
  fs.writeFileSync(path.join(__dirname, 'resultado-http.json'), JSON.stringify(summary, null, 2) + '\n');
  console.log(`HU-04-T04: ${summary.aprobadas}/${summary.solicitudes} casos; 0 llegadas inválidas guardadas.`);
}

main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => {
  if (!userId) return;
  const sql = `BEGIN; DELETE FROM llegada WHERE id_receptor = ${Number(userId)}; ` +
    `DELETE FROM usuario_permiso WHERE id_usuario = ${Number(userId)}; ` +
    `DELETE FROM usuario WHERE id_usuario = ${Number(userId)}; COMMIT;`;
  try {
    execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-v', 'ON_ERROR_STOP=1',
      '-U', config.DB_USER, '-d', 'indumotos', '-c', sql], { cwd: repo, stdio: 'ignore' });
    console.log('Datos ficticios retirados.');
  } catch (error) { console.error('No se pudieron retirar los datos ficticios:', error.message); process.exitCode = 1; }
});
