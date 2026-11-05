const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const repo = path.resolve(__dirname, '../../..');
const env = Object.fromEntries(fs.readFileSync(path.join(repo, '.env'), 'utf8').split(/\r?\n/)
  .filter(line => /^[A-Z0-9_]+=/.test(line)).map(line => {
    const i = line.indexOf('=');
    return [line.slice(0, i), line.slice(i + 1).replace(/^["']|["']$/g, '')];
  }));
const base = process.env.BASE_URL || 'http://127.0.0.1:8030';
const runId = Date.now();
const password = `Integral!${crypto.randomBytes(10).toString('hex')}Aa`;
const users = [];
const arrivals = [];
const results = [];
let inventoryToken;

async function request(method, route, token, body) {
  const response = await fetch(base + route, {
    method,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  return { status: response.status, data: await response.json().catch(() => null) };
}
function check(area, name, actual, expected, assertion = () => true) {
  const passed = actual.status === expected && assertion(actual.data);
  results.push({ area, caso: name, esperado: expected, obtenido: actual.status, paso: passed });
  if (!passed) throw new Error(`${area} ${name}: esperado ${expected}, obtenido ${actual.status} ${JSON.stringify(actual.data)}`);
  return actual.data;
}
function sql(query) {
  return execFileSync('docker', ['compose', 'exec', '-T', 'db', 'psql', '-t', '-A',
    '-v', 'ON_ERROR_STOP=1', '-U', env.DB_USER, '-d', 'indumotos', '-c', query],
    { cwd: repo, encoding: 'utf8' }).trim();
}
function signedExpiredToken(username) {
  const now = Math.floor(Date.now() / 1000);
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  const unsigned = `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode({ iss: 'https://indumotos.local', sub: username, iat: now - 120, exp: now - 60 })}`;
  const signature = crypto.createHmac('sha256', Buffer.from(env.JWT_SECRET_BASE64, 'base64'))
    .update(unsigned).digest('base64url');
  return `${unsigned}.${signature}`;
}
const ficha = (llegadaId, chasis) => ({ llegadaId, marca: 'RONCO', modelo: 'RZ 250',
  color: 'ROJO', chasisCompleto: chasis, numeroMotor: `M-${chasis}`,
  dua: '000123', itemDua: '004' });

async function main() {
  const admin = check('HU-26-T06', 'Login válido ADMIN', await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: env.ADMIN_USERNAME, clave: env.ADMIN_PASSWORD }), 200,
  data => Boolean(data?.accessToken));
  async function makeUser(role) {
    const username = `t06_${role.toLowerCase()}_${runId}`;
    const created = await request('POST', '/api/v1/usuarios', admin.accessToken,
      { nombreCompleto: `Prueba integral ${role}`, nombreUsuario: username,
        clave: password, rolCodigo: role });
    if (created.status !== 201) throw new Error(`No se pudo crear ${role}: ${created.status}`);
    users.push({ id: created.data.id, username });
    const login = check('HU-26-T06', `Login válido ${role}`,
      await request('POST', '/api/v1/auth/login', null,
        { nombreUsuario: username, clave: password }), 200,
      data => Boolean(data?.accessToken));
    return login.accessToken;
  }
  const storeToken = await makeUser('ALMACEN');
  inventoryToken = await makeUser('INVENTARIOS');
  const arrivalBody = suffix => ({ numeroGuia: `T06-${runId}-${suffix}`,
    cantidadDeclarada: 2, cantidadRecibida: 2, observacion: 'Datos ficticios T06' });
  const arrival = check('HU-04-T06', 'Registrar llegada',
    await request('POST', '/api/v1/llegadas', storeToken, arrivalBody('DOS')), 201,
    data => Boolean(data?.id && data?.fechaLlegada));
  arrivals.push(arrival.id);
  const detailEmpty = check('HU-04-T06', 'Detalle inicial sin fichas',
    await request('GET', `/api/v1/llegadas/${arrival.id}/detalle`, inventoryToken), 200,
    data => data?.cantidadFichas === 0 && data?.llegada?.id === arrival.id);
  check('HU-04-T06', 'Guía incompleta',
    await request('POST', '/api/v1/llegadas', storeToken,
      { cantidadDeclarada: 2, cantidadRecibida: 2 }), 400,
    data => data?.campos?.includes('numeroGuia'));
  check('HU-04-T06', 'INVENTARIOS no registra llegada',
    await request('POST', '/api/v1/llegadas', inventoryToken, arrivalBody('DENEGADA')), 403);

  const chassisA = `T06-A-${runId}-0042`;
  const chassisB = `T06-B-${runId}-0042`;
  const first = check('HU-02-T06', 'Primera ficha válida',
    await request('POST', '/api/v1/motocicletas', inventoryToken, ficha(arrival.id, chassisA)), 201,
    data => data?.chasisCompleto === chassisA && data?.llegadaId === arrival.id);
  const second = check('HU-02-T06', 'Segundo chasis con mismo sufijo',
    await request('POST', '/api/v1/motocicletas', inventoryToken, ficha(arrival.id, chassisB)), 201,
    data => data?.chasisCompleto === chassisB && data?.id !== first.id);
  const duplicate = check('HU-02-T06', 'Chasis completo duplicado sin distinguir mayúsculas',
    await request('POST', '/api/v1/motocicletas', inventoryToken,
      ficha(arrival.id, chassisA.toLowerCase())), 409,
    data => data?.fichaExistenteId === first.id);
  check('HU-02-T06', 'Consultar ficha existente',
    await request('GET', `/api/v1/motocicletas/${duplicate.fichaExistenteId}`, inventoryToken), 200,
    data => data?.id === first.id);
  for (const field of ['marca', 'modelo', 'color', 'chasisCompleto', 'numeroMotor', 'dua', 'itemDua']) {
    const invalid = ficha(arrival.id, `T06-INVALID-${field}-${runId}`);
    delete invalid[field];
    check('HU-02-T06', `Falta ${field}`,
      await request('POST', '/api/v1/motocicletas', inventoryToken, invalid), 400,
      data => data?.campos?.includes(field));
  }
  check('HU-02-T06', 'Llegada inexistente',
    await request('POST', '/api/v1/motocicletas', inventoryToken,
      ficha(999999999, `T06-NOLLEGADA-${runId}`)), 404);
  check('HU-02-T06', 'ALMACEN no registra ficha',
    await request('POST', '/api/v1/motocicletas', storeToken,
      ficha(arrival.id, `T06-DENEGADA-${runId}`)), 403);
  const detail = check('HU-04-T06', 'Detalle con dos fichas',
    await request('GET', `/api/v1/llegadas/${arrival.id}/detalle`, inventoryToken), 200,
    data => data?.cantidadFichas === 2 && data?.diferenciaInformativa === 0 &&
      data?.fichas?.some(f => f.id === first.id) && data?.fichas?.some(f => f.id === second.id));
  if (!detailEmpty.llegada.fechaLlegada || !detail.llegada.fechaLlegada ||
      detail.llegada.receptorUsuario !== users[0].username)
    throw new Error('No se conservó fecha o responsable de la llegada');
  const count = Number(sql(`SELECT count(*) FROM motocicleta WHERE id_llegada = ${arrival.id}`));
  if (count !== 2) throw new Error(`Se esperaban 2 fichas en la BD, encontradas ${count}`);
  results.push({ area: 'HU-04/HU-02-T06', caso: 'Persistencia sin unidades fantasma', esperado: 2, obtenido: count, paso: true });

  const concurrentArrival = check('HU-04-T06', 'Llegada para concurrencia',
    await request('POST', '/api/v1/llegadas', storeToken, arrivalBody('CONCURRENT')), 201);
  arrivals.push(concurrentArrival.id);
  const concurrentBody = ficha(concurrentArrival.id, `T06-CONCURRENT-${runId}`);
  const concurrent = await Promise.all([
    request('POST', '/api/v1/motocicletas', inventoryToken, concurrentBody),
    request('POST', '/api/v1/motocicletas', inventoryToken, concurrentBody)
  ]);
  const statuses = concurrent.map(x => x.status).sort().join(',');
  const concurrentCount = Number(sql(`SELECT count(*) FROM motocicleta WHERE id_llegada = ${concurrentArrival.id}`));
  if (statuses !== '201,409' || concurrentCount !== 1)
    throw new Error(`Concurrencia: estados ${statuses}, fichas ${concurrentCount}`);
  results.push({ area: 'HU-02-T06', caso: 'Dos solicitudes simultáneas mismo chasis',
    esperado: '201,409 y 1 ficha', obtenido: `${statuses} y ${concurrentCount} ficha`, paso: true });

  execFileSync('docker', ['compose', 'restart', 'api'], { cwd: repo, stdio: 'ignore' });
  let postRestart;
  for (let i = 0; i < 30; i++) {
    try {
      postRestart = await request('GET', `/api/v1/llegadas/${arrival.id}/detalle`, inventoryToken);
      if (postRestart.status === 200) break;
    } catch (_) {}
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  check('HU-04-T06', 'Detalle conservado tras reiniciar API', postRestart || { status: 0 }, 200,
    data => data?.cantidadFichas === 2 && data?.llegada?.id === arrival.id);

  check('HU-02-T06', 'Retirar permiso de consulta para prueba',
    await request('PUT', `/api/v1/usuarios/${users[1].id}/permisos/INVENTARIO_CONSULTAR`,
      admin.accessToken, { decision: 'DENEGAR' }), 200);
  check('HU-02-T06', 'Conflicto sin revelar ficha a quien no puede consultarla',
    await request('POST', '/api/v1/motocicletas', inventoryToken,
      ficha(arrival.id, chassisA)), 409,
    data => data?.fichaExistenteId === undefined);
  check('HU-02-T06', 'Ficha existente protegida',
    await request('GET', `/api/v1/motocicletas/${first.id}`, inventoryToken), 403);
  check('HU-02-T06', 'Restaurar permiso de consulta',
    await request('DELETE', `/api/v1/usuarios/${users[1].id}/permisos/INVENTARIO_CONSULTAR`,
      admin.accessToken), 200);

  check('HU-26-T06', 'Clave incorrecta', await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: users[1].username, clave: 'clave-equivocada' }), 401);
  check('HU-26-T06', 'Campos vacíos', await request('POST', '/api/v1/auth/login', null,
    { nombreUsuario: '', clave: '' }), 400);
  check('HU-26-T06', 'Sin token', await request('GET', '/api/v1/auth/me'), 401);
  check('HU-26-T06', 'Token falso', await request('GET', '/api/v1/auth/me', 'token-falso'), 401);
  check('HU-26-T06', 'Token vencido', await request('GET', '/api/v1/auth/me',
    signedExpiredToken(users[1].username)), 401);
  check('HU-26-T06', 'Token vigente antes de salir',
    await request('GET', '/api/v1/auth/me', inventoryToken), 200);
  check('HU-26-T06', 'Cerrar sesión',
    await request('POST', '/api/v1/auth/logout', inventoryToken, {}), 204);
  check('HU-26-T06', 'Mismo token revocado',
    await request('GET', '/api/v1/auth/me', inventoryToken), 401);
  check('HU-26-T06', 'Mismo token no registra ficha',
    await request('POST', '/api/v1/motocicletas', inventoryToken,
      ficha(arrival.id, `T06-REVOKED-${runId}`)), 401);
  execFileSync('docker', ['compose', 'restart', 'api'], { cwd: repo, stdio: 'ignore' });
  let revokedAfterRestart;
  for (let i = 0; i < 30; i++) {
    try {
      revokedAfterRestart = await request('GET', '/api/v1/auth/me', inventoryToken);
      if (revokedAfterRestart.status === 401) break;
    } catch (_) {}
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  check('HU-26-T06', 'Token revocado sigue inválido tras reiniciar API',
    revokedAfterRestart || { status: 0 }, 401);
  check('HU-26-T06', 'Logout sin token',
    await request('POST', '/api/v1/auth/logout', null, {}), 401);
  check('HU-26-T06', 'Nuevo login tras salir',
    await request('POST', '/api/v1/auth/login', null,
      { nombreUsuario: users[1].username, clave: password }), 200,
    data => Boolean(data?.accessToken));
  sql(`UPDATE usuario SET estado = 'INACTIVO' WHERE id_usuario = ${users[1].id}`);
  check('HU-26-T06', 'Cuenta inactiva',
    await request('POST', '/api/v1/auth/login', null,
      { nombreUsuario: users[1].username, clave: password }), 401);
  const finalCount = Number(sql(`SELECT count(*) FROM motocicleta WHERE id_llegada IN (${arrivals.join(',')})`));
  if (finalCount !== 3) throw new Error(`Persistencia final: ${finalCount} fichas, esperadas 3`);
  const summary = { fecha: new Date().toISOString(), gateway: base,
    casos: results.length, aprobados: results.filter(x => x.paso).length,
    fallidos: results.filter(x => !x.paso).length, fichasValidas: 3,
    fichasInvalidas: 0, duplicadosDemoArchivados: Number(sql('SELECT count(*) FROM motocicleta_duplicado_archivo')),
    resultados: results };
  fs.writeFileSync(path.join(__dirname, 'resultado-integral.json'), JSON.stringify(summary, null, 2) + '\n');
  console.log(`Sprint 1 T06: ${summary.aprobados}/${summary.casos} casos; 3 fichas válidas, 0 inválidas.`);
}

main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => {
  if (!users.length) return;
  const userIds = users.map(u => Number(u.id)).join(',');
  const arrivalIds = arrivals.map(Number).join(',') || '0';
  const tokenHash = inventoryToken ? crypto.createHash('sha256').update(inventoryToken).digest('hex') : '';
  const cleanup = `BEGIN; DELETE FROM motocicleta WHERE id_llegada IN (${arrivalIds}); ` +
    `DELETE FROM llegada WHERE id_llegada IN (${arrivalIds}); ` +
    `DELETE FROM usuario_permiso WHERE id_usuario IN (${userIds}); ` +
    `DELETE FROM usuario WHERE id_usuario IN (${userIds}); ` +
    (tokenHash ? `DELETE FROM token_revocado WHERE token_hash = '${tokenHash}'; ` : '') + 'COMMIT;';
  try {
    sql(cleanup);
    console.log('Datos ficticios retirados.');
  } catch (error) { console.error('No se pudieron retirar los datos ficticios:', error.message); process.exitCode = 1; }
});
