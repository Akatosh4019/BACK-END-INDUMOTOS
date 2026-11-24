package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DisponibilidadService {
    public record Unidad(long id,String chasis,String marca,String modelo,String color,
        String ubicacion,String estado,String motivo,OffsetDateTime actualizadoEn,
        String registradoPor,boolean puedeOfrecerse) {}
    public record Evento(String estado,String motivo,String registrador,OffsetDateTime fecha) {}
    public record Pagina(List<Unidad> items,int pagina,long totalElementos) {}
    private final JdbcTemplate jdbc;
    public DisponibilidadService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    public Pagina buscar(String chasis,int pagina){
        if(pagina<0 || chasis==null || chasis.trim().length()<4 || chasis.trim().length()>120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escribe el chasis o al menos cuatro caracteres");
        String filtro="%"+chasis.trim().toUpperCase().replace("\\","\\\\")
            .replace("%","\\%").replace("_","\\_")+"%";
        long total=jdbc.queryForObject("SELECT count(*) FROM motocicleta WHERE upper(chasis_completo) LIKE ?",Long.class,filtro);
        var ids=jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE upper(chasis_completo) LIKE ? " +
            "ORDER BY chasis_completo LIMIT 10 OFFSET ?",(rs,n)->rs.getLong(1),filtro,(long)pagina*10);
        return new Pagina(ids.stream().map(this::consultar).toList(),pagina,total);
    }

    public Unidad consultar(long id){
        var lista=jdbc.query("SELECT m.id_motocicleta,m.chasis_completo,m.marca,m.modelo,m.color,u.nombre," +
            "a.ocurrido_en,ua.nombre_usuario," +
            "v.vendida_en,uv.nombre_usuario," +
            "e.despachado_en,ue.nombre_usuario," +
            "d.estado,d.motivo,d.registrado_en,ud.nombre_usuario," +
            "f.campo,f.reportada_en,uf.nombre_usuario," +
            "s.asociada_en,us.nombre_usuario " +
            "FROM motocicleta m LEFT JOIN motocicleta_ubicacion_actual a ON a.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN ubicacion u ON u.id_ubicacion=a.id_ubicacion " +
            "LEFT JOIN movimiento_motocicleta mm ON mm.id_movimiento=a.id_movimiento " +
            "LEFT JOIN usuario ua ON ua.id_usuario=mm.id_registrador " +
            "LEFT JOIN venta_motocicleta v ON v.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN usuario uv ON uv.id_usuario=v.id_registrador " +
            "LEFT JOIN envio_solicitud_motocicleta e ON e.id_motocicleta=m.id_motocicleta AND e.recibido_en IS NULL " +
            "LEFT JOIN usuario ue ON ue.id_usuario=e.despachado_por " +
            "LEFT JOIN LATERAL (SELECT estado,motivo,registrado_en,id_registrador FROM disponibilidad_evento " +
            "WHERE id_motocicleta=m.id_motocicleta ORDER BY id_evento DESC LIMIT 1) d ON true " +
            "LEFT JOIN usuario ud ON ud.id_usuario=d.id_registrador " +
            "LEFT JOIN LATERAL (SELECT campo,reportada_en,id_reportante FROM diferencia_datos " +
            "WHERE id_motocicleta=m.id_motocicleta AND estado='PENDIENTE' ORDER BY id_diferencia DESC LIMIT 1) f ON true " +
            "LEFT JOIN usuario uf ON uf.id_usuario=f.id_reportante " +
            "LEFT JOIN LATERAL (SELECT x.asociada_en,x.id_asignador FROM solicitud_ensamblaje_unidad x " +
            "WHERE x.id_motocicleta=m.id_motocicleta AND x.retirada_en IS NULL " +
            "AND NOT EXISTS(SELECT 1 FROM envio_solicitud_motocicleta z WHERE z.id_motocicleta=x.id_motocicleta " +
            "AND z.recibido_en IS NOT NULL) LIMIT 1) s ON true " +
            "LEFT JOIN usuario us ON us.id_usuario=s.id_asignador " +
            "WHERE m.id_motocicleta=?",
            (rs,n)->new Hechos(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
                rs.getString(6),rs.getObject(7,OffsetDateTime.class),rs.getString(8),
                rs.getObject(9,OffsetDateTime.class),rs.getString(10),
                rs.getObject(11,OffsetDateTime.class),rs.getString(12),
                rs.getString(13),rs.getString(14),rs.getObject(15,OffsetDateTime.class),rs.getString(16),
                rs.getString(17),rs.getObject(18,OffsetDateTime.class),rs.getString(19),
                rs.getObject(20,OffsetDateTime.class),rs.getString(21)),id);
        if(lista.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Moto no encontrada");
        return derivar(lista.getFirst());
    }

    /**
     * HU-22: hechos registrados de una moto. Cada par fecha/usuario es null cuando el hecho no existe.
     * {@code ubicacion} es null si nunca se confirmó una ubicación.
     */
    public record Hechos(long id,String chasis,String marca,String modelo,String color,
        String ubicacion,OffsetDateTime ubicacionEn,String ubicacionPor,
        OffsetDateTime ventaEn,String ventaPor,
        OffsetDateTime envioEn,String envioPor,
        String bloqueoEstado,String bloqueoMotivo,OffsetDateTime bloqueoEn,String bloqueoPor,
        String diferenciaCampo,OffsetDateTime diferenciaEn,String diferenciaPor,
        OffsetDateTime asignadaEn,String asignadaPor) {}

    /**
     * HU-22: decide un único estado con prioridad VENDIDA, EN_TRANSITO, BLOQUEADA, EN_REVISION,
     * ASIGNADA, SIN_UBICACION y DISPONIBLE. Solo una moto DISPONIBLE puede ofrecerse al cliente.
     */
    public static Unidad derivar(Hechos h){
        if(h.ventaEn()!=null)
            return unidad(h,"VENDIDA","Pago y comprobante registrados",h.ventaEn(),h.ventaPor());
        if(h.envioEn()!=null)
            return unidad(h,"EN_TRANSITO","Envío pendiente de recepción",h.envioEn(),h.envioPor());
        if("BLOQUEADA".equals(h.bloqueoEstado()))
            return unidad(h,"BLOQUEADA",h.bloqueoMotivo(),h.bloqueoEn(),h.bloqueoPor());
        if(h.diferenciaCampo()!=null)
            return unidad(h,"EN_REVISION","Diferencia de datos en revisión: "+campo(h.diferenciaCampo()),
                h.diferenciaEn(),h.diferenciaPor());
        if(h.asignadaEn()!=null)
            return unidad(h,"ASIGNADA","Asignada a una solicitud",h.asignadaEn(),h.asignadaPor());
        boolean habilitadaDespues=h.bloqueoEn()!=null;
        OffsetDateTime fecha=habilitadaDespues?h.bloqueoEn():h.ubicacionEn();
        String por=habilitadaDespues?h.bloqueoPor():h.ubicacionPor();
        if(h.ubicacion()==null)
            return unidad(h,"SIN_UBICACION","No hay ubicación confirmada",fecha,por);
        return unidad(h,"DISPONIBLE",habilitadaDespues?h.bloqueoMotivo():"Sin bloqueo registrado",fecha,por);
    }

    private static Unidad unidad(Hechos h,String estado,String motivo,OffsetDateTime fecha,String por){
        return new Unidad(h.id(),h.chasis(),h.marca(),h.modelo(),h.color(),h.ubicacion(),estado,motivo,
            fecha,por,"DISPONIBLE".equals(estado));
    }

    static String campo(String campo){
        return switch(campo){
            case "marca"->"marca";
            case "modelo"->"modelo";
            case "color"->"color";
            case "chasisCompleto"->"chasis";
            case "numeroMotor"->"número de motor";
            case "dua"->"DUA";
            case "itemDua"->"ítem del DUA";
            default->campo;
        };
    }

    public List<Evento> historial(long id){
        consultar(id);
        return jdbc.query("SELECT d.estado,d.motivo,u.nombre_usuario,d.registrado_en " +
            "FROM disponibilidad_evento d JOIN usuario u ON u.id_usuario=d.id_registrador " +
            "WHERE d.id_motocicleta=? ORDER BY d.id_evento DESC",
            (rs,n)->new Evento(rs.getString(1),rs.getString(2),rs.getString(3),
                rs.getObject(4,OffsetDateTime.class)),id);
    }

    @Transactional
    public Unidad cambiar(long id,String estado,String motivo,String actor){
        if(!"BLOQUEADA".equals(estado) && !"HABILITADA".equals(estado))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado no válido");
        if(motivo==null || motivo.isBlank() || motivo.trim().length()>500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escribe un motivo de hasta 500 caracteres");
        if(jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",
            (rs,n)->rs.getLong(1),id).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Moto no encontrada");
        var actual=consultar(id);
        if("VENDIDA".equals(actual.estado()) || "EN_TRANSITO".equals(actual.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"No se puede cambiar una moto vendida o en tránsito");
        if("HABILITADA".equals(estado) && "SIN_UBICACION".equals(actual.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Confirma primero su ubicación");
        if("HABILITADA".equals(estado) && "EN_REVISION".equals(actual.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Resuelve primero la diferencia de datos pendiente");
        jdbc.update("INSERT INTO disponibilidad_evento(id_motocicleta,estado,motivo,id_registrador) " +
            "SELECT ?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
            id,estado,motivo.trim(),actor);
        return consultar(id);
    }
}
