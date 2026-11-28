package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Motocicleta;

@Service
public class DiferenciasService {
    private static final Set<String> CAMPOS=Set.of("marca","modelo","color","chasisCompleto",
        "numeroMotor","dua","itemDua");
    public record Caso(long id,long motocicletaId,String chasis,String campo,String valorObservado,
        String descripcion,String estado,String reportante,OffsetDateTime reportadaEn,
        String valorAnterior,String valorCorregido,String fuente,String resolucion,
        String resolutor,OffsetDateTime resueltaEn) {}
    public record Pagina(List<Caso> items,int pagina,long totalElementos) {}
    private final JdbcTemplate jdbc;
    private final MotocicletaRepository motos;
    private final EditarMotocicleta editor;
    public DiferenciasService(JdbcTemplate jdbc,MotocicletaRepository motos,EditarMotocicleta editor){
        this.jdbc=jdbc;this.motos=motos;this.editor=editor;
    }

    public Pagina listar(String actor,String estado,int pagina){
        if(pagina<0 || !Set.of("PENDIENTE","RESUELTA","TODAS").contains(estado))
            throw error(HttpStatus.BAD_REQUEST,"Filtro o página no válidos");
        boolean inventarios=puedeVerTodos(actor);
        String condicion=" FROM diferencia_datos d JOIN motocicleta m ON m.id_motocicleta=d.id_motocicleta " +
            "JOIN usuario r ON r.id_usuario=d.id_reportante LEFT JOIN usuario x ON x.id_usuario=d.id_resolutor " +
            "WHERE (?='TODAS' OR d.estado=?) AND (? OR r.nombre_usuario=?)";
        long total=jdbc.queryForObject("SELECT count(*)"+condicion,Long.class,
            estado,estado,inventarios,actor);
        var items=jdbc.query("SELECT d.id_diferencia,d.id_motocicleta,m.chasis_completo,d.campo," +
            "d.valor_observado,d.descripcion,d.estado,r.nombre_usuario,d.reportada_en," +
            "d.valor_anterior,d.valor_corregido,d.fuente,d.resolucion,x.nombre_usuario,d.resuelta_en" +
            condicion+" ORDER BY d.reportada_en DESC,d.id_diferencia DESC LIMIT 10 OFFSET ?",
            (rs,n)->new Caso(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),
                rs.getObject(9,OffsetDateTime.class),rs.getString(10),rs.getString(11),
                rs.getString(12),rs.getString(13),rs.getString(14),
                rs.getObject(15,OffsetDateTime.class)),estado,estado,inventarios,actor,(long)pagina*10);
        return new Pagina(items,pagina,total);
    }

    @Transactional
    public void reportar(long motoId,String campo,String observado,String descripcion,String actor){
        if(!CAMPOS.contains(campo) || !texto(observado,300) || !texto(descripcion,1000))
            throw error(HttpStatus.BAD_REQUEST,"Completa el campo y la diferencia observada");
        if(jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=?",
            (rs,n)->rs.getLong(1),motoId).isEmpty()) throw error(HttpStatus.NOT_FOUND,"Moto no encontrada");
        jdbc.update("INSERT INTO diferencia_datos(id_motocicleta,campo,valor_observado,descripcion,id_reportante) " +
            "SELECT ?,?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
            motoId,campo,observado.trim(),descripcion.trim(),actor);
    }

    @Transactional
    public void resolver(long casoId,String corregido,String fuente,String resolucion,String actor){
        if(!texto(corregido,300) || !texto(fuente,500) || !texto(resolucion,1000))
            throw error(HttpStatus.BAD_REQUEST,"Indica valor, fuente y resolución");
        var caso=jdbc.query("SELECT id_motocicleta,campo FROM diferencia_datos " +
            "WHERE id_diferencia=? AND estado='PENDIENTE' FOR UPDATE",
            (rs,n)->new Object[]{rs.getLong(1),rs.getString(2)},casoId);
        if(caso.isEmpty())throw error(HttpStatus.CONFLICT,"Caso inexistente o ya resuelto");
        long motoId=(long)caso.getFirst()[0];String campo=(String)caso.getFirst()[1];
        jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",
            (rs,n)->rs.getLong(1),motoId);
        Motocicleta anterior=motos.buscarPorId(motoId);
        String previo=valor(anterior,campo);
        String nuevo=corregido.trim();
        try {
            editor.ejecutar(motoId,valorPara("marca",campo,nuevo,anterior.marca()),
                valorPara("modelo",campo,nuevo,anterior.modelo()),
                valorPara("color",campo,nuevo,anterior.color()),
                valorPara("chasisCompleto",campo,nuevo,anterior.chasisCompleto()),
                valorPara("numeroMotor",campo,nuevo,anterior.numeroMotor()),
                valorPara("dua",campo,nuevo,anterior.dua()),
                valorPara("itemDua",campo,nuevo,anterior.itemDua()));
        } catch (RegistrarMotocicleta.DatosInvalidos ex) {
            throw error(HttpStatus.BAD_REQUEST,"El valor corregido no es válido para la ficha o el catálogo");
        } catch (RegistrarMotocicleta.ChasisDuplicado ex) {
            throw error(HttpStatus.CONFLICT,"El chasis corregido ya pertenece a otra moto");
        }
        jdbc.update("UPDATE diferencia_datos SET estado='RESUELTA',valor_anterior=?," +
            "valor_corregido=?,fuente=?,resolucion=?,id_resolutor=" +
            "(SELECT id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO')," +
            "resuelta_en=CURRENT_TIMESTAMP WHERE id_diferencia=?",
            previo,nuevo,fuente.trim(),resolucion.trim(),actor,casoId);
    }

    private boolean puedeVerTodos(String actor){
        String rol=jdbc.queryForObject("SELECT r.codigo FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "WHERE u.nombre_usuario=?",String.class,actor);
        return Set.of("GERENTE","INVENTARIOS").contains(rol);
    }
    private String valor(Motocicleta m,String campo){return switch(campo){
        case "marca"->m.marca();case "modelo"->m.modelo();case "color"->m.color();
        case "chasisCompleto"->m.chasisCompleto();case "numeroMotor"->m.numeroMotor();
        case "dua"->m.dua();default->m.itemDua();};}
    private String valorPara(String nombre,String campo,String nuevo,String actual){
        return nombre.equals(campo)?nuevo:actual;
    }
    private boolean texto(String valor,int max){return valor!=null && !valor.isBlank() && valor.trim().length()<=max;}
    private ResponseStatusException error(HttpStatus status,String mensaje){return new ResponseStatusException(status,mensaje);}
}
