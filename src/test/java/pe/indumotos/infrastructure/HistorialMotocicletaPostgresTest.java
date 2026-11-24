package pe.indumotos.infrastructure;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;
class HistorialMotocicletaPostgresTest {
 private Connection connection;private JdbcTemplate jdbc;private DataSourceTransactionManager manager;private TransactionStatus transaction;private JdbcHistorialMotocicletaRepository repo;
 @BeforeEach void abrir()throws Exception{
  Assumptions.assumeTrue(System.getenv("INDUMOTOS_TEST_DB_URL")!=null,"Configurar INDUMOTOS_TEST_DB_URL para prueba PostgreSQL");
  connection=DriverManager.getConnection(System.getenv("INDUMOTOS_TEST_DB_URL"),System.getenv("INDUMOTOS_TEST_DB_USER"),System.getenv("INDUMOTOS_TEST_DB_PASSWORD"));
  var source=new SingleConnectionDataSource(connection,true);manager=new DataSourceTransactionManager(source);transaction=manager.getTransaction(new DefaultTransactionDefinition());jdbc=new JdbcTemplate(source);jdbc.execute("SET LOCAL search_path TO pg_temp");
  jdbc.execute("CREATE TEMP TABLE usuario(id_usuario bigint PRIMARY KEY,nombre_usuario text)");
  jdbc.execute("CREATE TEMP TABLE llegada(id_llegada bigint PRIMARY KEY,fecha_llegada timestamptz,numero_guia text,id_receptor bigint)");
  jdbc.execute("CREATE TEMP TABLE motocicleta(id_motocicleta bigint PRIMARY KEY,id_llegada bigint,chasis_completo text,marca text,modelo text,color text,creado_en timestamptz,id_creador bigint)");
  jdbc.execute("CREATE TEMP TABLE ubicacion(id_ubicacion bigint PRIMARY KEY,nombre text)");
  jdbc.execute("CREATE TEMP TABLE movimiento_motocicleta(id_movimiento bigint,id_motocicleta bigint,ocurrido_en timestamptz,tipo text,id_registrador bigint,id_origen bigint,id_destino bigint)");
  jdbc.execute("CREATE TEMP TABLE solicitud_ensamblaje_detalle(id_detalle bigint,id_solicitud bigint)");
  jdbc.execute("CREATE TEMP TABLE solicitud_ensamblaje_unidad(id_asociacion bigint,id_detalle bigint,id_motocicleta bigint,id_asignador bigint,asociada_en timestamptz,retirada_en timestamptz,id_retirador bigint)");
  jdbc.execute("CREATE TEMP TABLE solicitud_ensamblaje_evento(id_evento bigint,id_solicitud bigint,estado text,registrado_en timestamptz,id_registrador bigint)");
  jdbc.update("INSERT INTO usuario VALUES(1,'moises'),(2,'miguel'),(3,'cristian')");
  jdbc.update("INSERT INTO llegada VALUES(1,'2026-11-23T04:59Z','GUIA-DEMO',1)");
  jdbc.update("INSERT INTO motocicleta VALUES(7,1,'DEMO0042','Marca','Modelo','Azul','2026-11-23T05:00Z',2),(8,1,'OTRA0042','Marca','Modelo','Azul','2026-11-23T05:01Z',2)");
  jdbc.update("INSERT INTO ubicacion VALUES(1,'Almacén'),(2,'Tienda')");repo=new JdbcHistorialMotocicletaRepository(jdbc);
 }
 @AfterEach void cerrar()throws Exception{if(connection!=null){try{manager.rollback(transaction);}finally{connection.close();}}}
 private pe.indumotos.application.HistorialMotocicletaRepository.Pagina leer(int pagina,int tamano){return repo.consultar(repo.buscarUnidad(7),null,null,pagina,tamano);}
 @Test void uneHechosOrdenadosConActoresYNoIncluyeOtraMoto(){
  jdbc.update("INSERT INTO movimiento_motocicleta VALUES(1,7,'2026-11-23T06:00Z','UBICACION',1,NULL,1),(2,8,'2026-11-23T06:00Z','TRASLADO',1,1,2)");
  var resultado=leer(0,20);assertEquals(3,resultado.totalElementos());assertEquals(java.util.List.of("LLEGADA_REGISTRADA","FICHA_REGISTRADA","UBICACION"),resultado.items().stream().map(e->e.tipo()).toList());
  assertEquals("miguel",resultado.items().get(1).registradoPor());assertEquals("Almacén",resultado.items().get(2).destino());assertEquals(5,resultado.fuentesDisponibles().size());
 }
 @Test void paginaConOrdenEstableYNoEscribeAlConsultar(){
  var a=leer(0,1);var b=leer(1,1);assertEquals(2,a.totalPaginas());assertNotEquals(a.items().getFirst().clave(),b.items().getFirst().clave());assertTrue(leer(2,1).items().isEmpty());
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM motocicleta",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM llegada",Integer.class));
 }
 @Test void incluyeSoloSeguimientoDuranteLaAsignacionSinDuplicarEventos(){
  jdbc.update("INSERT INTO solicitud_ensamblaje_detalle VALUES(1,10),(2,20)");
  jdbc.update("INSERT INTO solicitud_ensamblaje_unidad VALUES(1,1,7,1,'2026-11-23T06:00Z','2026-11-23T08:00Z',1),(2,2,8,1,'2026-11-23T06:00Z',NULL,NULL)");
  jdbc.update("INSERT INTO solicitud_ensamblaje_evento VALUES(1,10,'PENDIENTE','2026-11-23T05:30Z',3),(2,10,'EN_PREPARACION','2026-11-23T07:00Z',1),(3,10,'ATENDIDA','2026-11-23T09:00Z',1),(4,20,'ATENDIDA','2026-11-23T07:00Z',1)");
  var eventos=leer(0,20).items();assertEquals(5,eventos.size());assertEquals(1,eventos.stream().filter(e->e.tipo().startsWith("SOLICITUD_")).count());assertEquals(10,eventos.stream().filter(e->e.tipo().startsWith("SOLICITUD_")).findFirst().orElseThrow().solicitudId());
 }
 @Test void fechasIncluyenHastaElFinalDelDiaPeru(){
  jdbc.update("INSERT INTO movimiento_motocicleta VALUES(1,7,'2026-11-24T04:59:59Z','UBICACION',1,NULL,1),(2,7,'2026-11-24T05:00Z','TRASLADO',1,1,2)");
  var resultado=repo.consultar(repo.buscarUnidad(7),OffsetDateTime.parse("2026-11-23T00:00-05:00"),OffsetDateTime.parse("2026-11-24T00:00-05:00"),0,20);
  assertEquals(2,resultado.totalElementos());assertFalse(resultado.items().stream().anyMatch(e->e.tipo().equals("LLEGADA_REGISTRADA")));assertTrue(resultado.items().stream().anyMatch(e->e.clave().equals("movimiento:1")));
 }
 @Test void incorporaModulosDeOtrasRamasSinExponerImportesNiReferencias(){
  jdbc.execute("CREATE TEMP TABLE envio_solicitud_motocicleta(id_envio bigint,id_motocicleta bigint,id_solicitud bigint,despachado_en timestamptz,despachado_por bigint,id_origen bigint,id_destino bigint,recibido_en timestamptz,recibido_por bigint)");
  jdbc.execute("CREATE TEMP TABLE venta_motocicleta(id_venta bigint,id_motocicleta bigint,id_tienda bigint,id_registrador bigint,vendida_en timestamptz,importe numeric,referencia_comprobante text)");
  jdbc.execute("CREATE TEMP TABLE disponibilidad_evento(id_evento bigint,id_motocicleta bigint,registrado_en timestamptz,estado text,id_registrador bigint)");
  jdbc.execute("CREATE TEMP TABLE diferencia_datos(id_diferencia bigint,id_motocicleta bigint,reportada_en timestamptz,campo text,id_reportante bigint,resuelta_en timestamptz,id_resolutor bigint)");
  jdbc.update("INSERT INTO envio_solicitud_motocicleta VALUES(1,7,10,'2026-11-23T07:00Z',1,1,2,'2026-11-23T08:00Z',3)");
  jdbc.update("INSERT INTO venta_motocicleta VALUES(1,7,2,3,'2026-11-23T10:00Z',99999,'COMPROBANTE PRIVADO')");
  jdbc.update("INSERT INTO disponibilidad_evento VALUES(1,7,'2026-11-23T09:00Z','BLOQUEADA',3)");
  jdbc.update("INSERT INTO diferencia_datos VALUES(1,7,'2026-11-23T08:30Z','color',3,'2026-11-23T09:30Z',2)");
  var resultado=leer(0,20);assertEquals(8,resultado.totalElementos());assertEquals(9,resultado.fuentesDisponibles().size());assertEquals("VENTA_REGISTRADA",resultado.items().getLast().tipo());
  assertFalse(resultado.toString().contains("COMPROBANTE PRIVADO"));assertFalse(resultado.toString().contains("99999"));
 }
 @Test void soportaLaColumnaOriginalDeVentasAntesDeV28(){
  jdbc.execute("CREATE TEMP TABLE venta_motocicleta(id_venta bigint,id_motocicleta bigint,id_tienda bigint,id_vendedor bigint,vendida_en timestamptz)");jdbc.update("INSERT INTO venta_motocicleta VALUES(1,7,2,3,'2026-11-23T10:00Z')");assertEquals("cristian",leer(0,20).items().getLast().registradoPor());
 }
}
