package pe.indumotos.application;
import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class FichaAmpliada {
 public record Datos(Integer anio,Integer cilindrada,String transmision,String combustible,String descripcion,long version){}
 public record Foto(long id,String mime,OffsetDateTime fecha){}
 public record Ficha(Datos datos,List<Foto> fotos){}
 public record Binario(String mime,byte[] contenido){}
 private final JdbcTemplate jdbc;
 public FichaAmpliada(JdbcTemplate jdbc){this.jdbc=jdbc;}
 private void existe(long id,boolean lock){if(jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=?"+(lock?" FOR UPDATE":""),(rs,n)->rs.getLong(1),id).isEmpty())throw error(HttpStatus.NOT_FOUND,"Motocicleta no encontrada");}
 public Ficha consultar(long id){
  existe(id,false);
  var rows=jdbc.query("SELECT anio,cilindrada,transmision,combustible,descripcion,version FROM motocicleta_ficha WHERE id_motocicleta=?",(rs,n)->new Datos(rs.getObject(1,Integer.class),rs.getObject(2,Integer.class),rs.getString(3),rs.getString(4),rs.getString(5),rs.getLong(6)),id);
  var fotos=jdbc.query("SELECT id_foto,mime,creada_en FROM motocicleta_foto WHERE id_motocicleta=? ORDER BY id_foto",(rs,n)->new Foto(rs.getLong(1),rs.getString(2),rs.getObject(3,OffsetDateTime.class)),id);
  return new Ficha(rows.isEmpty()?new Datos(null,null,null,null,null,0):rows.getFirst(),fotos);
 }
 @Transactional public Ficha guardar(long id,Datos d,String actor){
  existe(id,true);validar(d);var current=consultar(id).datos();if(current.version()!=d.version())throw error(HttpStatus.CONFLICT,"La ficha cambió. Actualiza antes de guardar");
  Long autor=jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE nombre_usuario=?",Long.class,actor);
  jdbc.update("INSERT INTO motocicleta_ficha(id_motocicleta,anio,cilindrada,transmision,combustible,descripcion,version,id_editor) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id_motocicleta) DO UPDATE SET anio=EXCLUDED.anio,cilindrada=EXCLUDED.cilindrada,transmision=EXCLUDED.transmision,combustible=EXCLUDED.combustible,descripcion=EXCLUDED.descripcion,version=EXCLUDED.version,id_editor=EXCLUDED.id_editor,actualizada_en=CURRENT_TIMESTAMP",id,d.anio(),d.cilindrada(),limpiar(d.transmision()),limpiar(d.combustible()),limpiar(d.descripcion()),d.version()+1,autor);
  jdbc.update("INSERT INTO motocicleta_ficha_evento(id_motocicleta,datos,id_actor) SELECT id_motocicleta,to_jsonb(motocicleta_ficha),? FROM motocicleta_ficha WHERE id_motocicleta=?",autor,id);
  return consultar(id);
 }
 public static void validar(Datos d){if(d==null||d.version()<0||(d.anio()!=null&&(d.anio()<1900||d.anio()>2100))||(d.cilindrada()!=null&&(d.cilindrada()<1||d.cilindrada()>5000))||large(d.transmision(),80)||large(d.combustible(),80)||large(d.descripcion(),2000))throw error(HttpStatus.BAD_REQUEST,"Revisa año, cilindrada y longitud de los datos");}
 private static boolean large(String s,int max){return s!=null&&s.length()>max;}
 private static String limpiar(String s){return s==null?null:s.trim();}
 @Transactional public Ficha foto(long id,byte[] bytes,String actor){
  existe(id,true);String mime=ImagenAdjunta.validar(bytes);Long count=jdbc.queryForObject("SELECT count(*) FROM motocicleta_foto WHERE id_motocicleta=?",Long.class,id);
  if(count>=3)throw error(HttpStatus.CONFLICT,"La ficha ya tiene tres fotos");
  try{jdbc.update("INSERT INTO motocicleta_foto(id_motocicleta,contenido,mime,sha256,id_autor) SELECT ?,?,?,?,id_usuario FROM usuario WHERE nombre_usuario=?",id,bytes,mime,huella(bytes),actor);}catch(org.springframework.dao.DuplicateKeyException ex){throw error(HttpStatus.CONFLICT,"Esta foto ya está adjunta");}
  return consultar(id);
 }
 public Binario foto(long id,long foto){var rows=jdbc.query("SELECT mime,contenido FROM motocicleta_foto WHERE id_motocicleta=? AND id_foto=?",(rs,n)->new Binario(rs.getString(1),rs.getBytes(2)),id,foto);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Foto no encontrada");return rows.getFirst();}
 public static String huella(byte[] b){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception ex){throw new IllegalStateException(ex);}}
 private static ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
