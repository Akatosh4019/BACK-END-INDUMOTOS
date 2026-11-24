package pe.indumotos.application;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PagosManuales {
 public record Pago(long id,long motocicletaId,long tiendaId,String chasis,String metodo,String entidad,String referencia,BigDecimal importe,String estado,String motivo,String referenciaVenta,Long ventaId,String registrador,String revisor,OffsetDateTime fecha){}
 public record Pagina(List<Pago> items,int pagina,long totalElementos){}
 private final JdbcTemplate jdbc;private final VentasService ventas;private final DisponibilidadService disponibilidad;
 public PagosManuales(JdbcTemplate jdbc,VentasService ventas,DisponibilidadService disponibilidad){this.jdbc=jdbc;this.ventas=ventas;this.disponibilidad=disponibilidad;}
 private static ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
 private long actor(String user){return jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",Long.class,user);}
 private List<Pago> query(String where,Object... args){return jdbc.query("SELECT p.id_pago,p.id_motocicleta,p.id_tienda,m.chasis_completo,p.metodo,p.entidad,p.referencia,p.importe,p.estado,p.motivo,p.referencia_venta,p.id_venta,u.nombre_usuario,r.nombre_usuario,p.registrado_en FROM pago_venta p JOIN motocicleta m ON m.id_motocicleta=p.id_motocicleta JOIN usuario u ON u.id_usuario=p.id_registrador LEFT JOIN usuario r ON r.id_usuario=p.id_revisor WHERE "+where,(rs,n)->new Pago(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getBigDecimal(8),rs.getString(9),rs.getString(10),rs.getString(11),rs.getObject(12,Long.class),rs.getString(13),rs.getString(14),rs.getObject(15,OffsetDateTime.class)),args);}
 public Pago consultar(String user,long id){var rows=query("p.id_pago=?",id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Pago no encontrado");var p=rows.getFirst();ventas.tienda(user,p.tiendaId());return p;}
 public Pagina listar(String user,Long tienda,int pagina){long t=ventas.tienda(user,tienda);if(pagina<0)throw error(HttpStatus.BAD_REQUEST,"Página no válida");return new Pagina(query("p.id_tienda=? ORDER BY p.id_pago DESC LIMIT 10 OFFSET ?",t,(long)pagina*10),pagina,jdbc.queryForObject("SELECT count(*) FROM pago_venta WHERE id_tienda=?",Long.class,t));}
 public static void validar(String metodo,String entidad,String referencia,BigDecimal importe){if(metodo==null||!Set.of("YAPE","PLIN","TRANSFERENCIA").contains(metodo)||entidad==null||entidad.isBlank()||entidad.length()>80||referencia==null||referencia.isBlank()||referencia.length()>100||importe==null||importe.signum()<=0||importe.scale()>2||importe.precision()>12||importe.compareTo(new BigDecimal("9999999999.99"))>0)throw error(HttpStatus.BAD_REQUEST,"Revisa método, entidad, referencia e importe (hasta dos decimales)");}
 @Transactional public Pago registrar(String user,Long tienda,UUID solicitud,long moto,String metodo,String entidad,String referencia,BigDecimal importe,byte[] imagen){
  long t=ventas.tienda(user,tienda);validar(metodo,entidad,referencia,importe);String mime=ImagenAdjunta.validar(imagen),hash=FichaAmpliada.huella(imagen);
  if(!metodo.equals("TRANSFERENCIA"))entidad=metodo;
  if(jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",(rs,n)->rs.getLong(1),moto).isEmpty())throw error(HttpStatus.NOT_FOUND,"Moto no encontrada");
  var prev=query("p.solicitud=?",solicitud);
  if(!prev.isEmpty()){
   var p=prev.getFirst();String saved=jdbc.queryForObject("SELECT sha256 FROM pago_venta WHERE id_pago=?",String.class,p.id());
   if(p.tiendaId()!=t||p.motocicletaId()!=moto||!p.registrador().equals(user)||p.importe().compareTo(importe)!=0||!p.metodo().equals(metodo)||!p.entidad().equals(entidad.trim())||!p.referencia().equals(referencia.trim())||!hash.equals(saved))throw error(HttpStatus.CONFLICT,"Esta solicitud ya fue usada con otros datos");return p;
  }
  if(!disponibilidad.consultar(moto).puedeOfrecerse())throw error(HttpStatus.CONFLICT,"La moto no está disponible para venta");
  if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM motocicleta_ubicacion_actual WHERE id_motocicleta=? AND id_ubicacion=?)",Boolean.class,moto,t)))throw error(HttpStatus.CONFLICT,"La moto no pertenece a esta tienda");
  try{
   Long id=jdbc.queryForObject("INSERT INTO pago_venta(solicitud,id_motocicleta,id_tienda,metodo,entidad,referencia,importe,comprobante,mime,sha256,id_registrador) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id_pago",Long.class,solicitud,moto,t,metodo,entidad.trim(),referencia.trim(),importe,imagen,mime,hash,actor(user));
   evento(id,"PENDIENTE",user,"Comprobante recibido para revisión");return consultar(user,id);
  }catch(org.springframework.dao.DuplicateKeyException ex){throw error(HttpStatus.CONFLICT,"La referencia ya existe o hay un pago pendiente para esta moto");}
 }
 private Pago bloquear(String user,long id){var p=consultar(user,id);jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",(rs,n)->rs.getLong(1),p.motocicletaId());jdbc.query("SELECT id_pago FROM pago_venta WHERE id_pago=? FOR UPDATE",(rs,n)->rs.getLong(1),id);return consultar(user,id);}
 @Transactional public Pago confirmar(String user,long id,String comprobante,boolean verificado){
  var p=bloquear(user,id);if(comprobante==null||comprobante.isBlank()||comprobante.length()>100||!verificado)throw error(HttpStatus.BAD_REQUEST,"Verifica el abono real e indica el comprobante de venta");
  if(p.estado().equals("CONFIRMADO")){if(!comprobante.trim().equals(p.referenciaVenta()))throw error(HttpStatus.CONFLICT,"El pago ya fue confirmado con otro comprobante");return p;}
  if(!p.estado().equals("PENDIENTE"))throw error(HttpStatus.CONFLICT,"El pago fue rechazado");
  ventas.registrar(user,p.tiendaId(),p.motocicletaId(),p.referencia(),comprobante.trim(),p.importe(),p.id());
  Long venta=jdbc.queryForObject("SELECT id_venta FROM venta_motocicleta WHERE id_motocicleta=?",Long.class,p.motocicletaId());
  jdbc.update("UPDATE pago_venta SET estado='CONFIRMADO',id_revisor=?,revisado_en=CURRENT_TIMESTAMP,referencia_venta=?,id_venta=? WHERE id_pago=?",actor(user),comprobante.trim(),venta,id);evento(id,"CONFIRMADO",user,"Abono verificado manualmente");return consultar(user,id);
 }
 @Transactional public Pago rechazar(String user,long id,String motivo){var p=bloquear(user,id);if(motivo==null||motivo.isBlank()||motivo.length()>500)throw error(HttpStatus.BAD_REQUEST,"Indica un motivo de hasta500 caracteres");if(!p.estado().equals("PENDIENTE"))throw error(HttpStatus.CONFLICT,"El pago ya fue revisado");jdbc.update("UPDATE pago_venta SET estado='RECHAZADO',motivo=?,id_revisor=?,revisado_en=CURRENT_TIMESTAMP WHERE id_pago=?",motivo.trim(),actor(user),id);evento(id,"RECHAZADO",user,motivo.trim());return consultar(user,id);}
 public FichaAmpliada.Binario comprobante(String user,long id){consultar(user,id);return jdbc.queryForObject("SELECT mime,comprobante FROM pago_venta WHERE id_pago=?",(rs,n)->new FichaAmpliada.Binario(rs.getString(1),rs.getBytes(2)),id);}
 private void evento(long id,String estado,String user,String motivo){jdbc.update("INSERT INTO pago_evento(id_pago,estado,id_actor,motivo) VALUES(?,?,?,?)",id,estado,actor(user),motivo);}
}
