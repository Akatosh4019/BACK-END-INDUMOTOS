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
        String ubicacion,String estado,String motivo,OffsetDateTime actualizadoEn) {}
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
        var lista=jdbc.query("SELECT m.id_motocicleta,m.chasis_completo,m.marca,m.modelo,m.color,u.nombre,"+
            "CASE WHEN v.id_venta IS NOT NULL THEN 'VENDIDA' " +
            "WHEN e.id_envio IS NOT NULL THEN 'EN_TRANSITO' " +
            "WHEN d.estado='BLOQUEADA' THEN 'BLOQUEADA' " +
            "WHEN s.id_motocicleta IS NOT NULL THEN 'ASIGNADA' " +
            "WHEN a.id_ubicacion IS NULL THEN 'SIN_UBICACION' " +
            "ELSE 'DISPONIBLE' END,"+
            "CASE WHEN v.id_venta IS NOT NULL THEN 'Pago y comprobante registrados' " +
            "WHEN e.id_envio IS NOT NULL THEN 'Envío pendiente de recepción' " +
            "WHEN d.estado='BLOQUEADA' THEN d.motivo " +
            "WHEN s.id_motocicleta IS NOT NULL THEN 'Asignada a una solicitud' " +
            "WHEN a.id_ubicacion IS NULL THEN 'No hay ubicación confirmada' " +
            "ELSE 'Sin bloqueo registrado' END," +
            "CASE WHEN v.id_venta IS NOT NULL THEN v.vendida_en " +
            "WHEN e.id_envio IS NOT NULL THEN e.despachado_en " +
            "WHEN d.estado='BLOQUEADA' THEN d.registrado_en " +
            "WHEN s.id_motocicleta IS NOT NULL THEN s.asociada_en " +
            "ELSE coalesce(d.registrado_en,a.ocurrido_en) END " +
            "FROM motocicleta m LEFT JOIN motocicleta_ubicacion_actual a ON a.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN ubicacion u ON u.id_ubicacion=a.id_ubicacion " +
            "LEFT JOIN venta_motocicleta v ON v.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN envio_solicitud_motocicleta e ON e.id_motocicleta=m.id_motocicleta AND e.recibido_en IS NULL " +
            "LEFT JOIN LATERAL (SELECT estado,motivo,registrado_en FROM disponibilidad_evento " +
            "WHERE id_motocicleta=m.id_motocicleta ORDER BY id_evento DESC LIMIT 1) d ON true " +
            "LEFT JOIN LATERAL (SELECT x.id_motocicleta,x.asociada_en FROM solicitud_ensamblaje_unidad x " +
            "WHERE x.id_motocicleta=m.id_motocicleta AND x.retirada_en IS NULL " +
            "AND NOT EXISTS(SELECT 1 FROM envio_solicitud_motocicleta z WHERE z.id_motocicleta=x.id_motocicleta " +
            "AND z.recibido_en IS NOT NULL) LIMIT 1) s ON true " +
            "WHERE m.id_motocicleta=?",
            (rs,n)->new Unidad(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),
                rs.getObject(9,OffsetDateTime.class)),id);
        if(lista.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Moto no encontrada");
        return lista.getFirst();
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
        jdbc.update("INSERT INTO disponibilidad_evento(id_motocicleta,estado,motivo,id_registrador) " +
            "SELECT ?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
            id,estado,motivo.trim(),actor);
        return consultar(id);
    }
}
