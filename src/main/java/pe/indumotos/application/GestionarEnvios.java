package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarEnvios {
    public record Envio(long motocicletaId, String chasis, String origen, String destino,
        String estado, OffsetDateTime despachadoEn, OffsetDateTime recibidoEn) {}
    private record Unidad(long id, String chasis, long origenId, String origenTipo) {}

    private final JdbcTemplate jdbc;
    private final UbicacionRepository ubicaciones;

    public GestionarEnvios(JdbcTemplate jdbc, UbicacionRepository ubicaciones) {
        this.jdbc = jdbc;
        this.ubicaciones = ubicaciones;
    }

    public List<Envio> listar(long solicitudId) {
        verificarSolicitud(solicitudId);
        return jdbc.query("SELECT e.id_motocicleta,m.chasis_completo,o.nombre,d.nombre," +
            "CASE WHEN e.recibido_en IS NULL THEN 'EN_TRANSITO' ELSE 'RECIBIDA' END," +
            "e.despachado_en,e.recibido_en FROM envio_solicitud_motocicleta e " +
            "JOIN motocicleta m ON m.id_motocicleta=e.id_motocicleta " +
            "JOIN ubicacion o ON o.id_ubicacion=e.id_origen " +
            "JOIN ubicacion d ON d.id_ubicacion=e.id_destino " +
            "WHERE e.id_solicitud=? ORDER BY e.id_envio",
            (rs, n) -> new Envio(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getObject(6,OffsetDateTime.class),rs.getObject(7,OffsetDateTime.class)),
            solicitudId);
    }

    public Envio activoDeMotocicleta(long motocicletaId) {
        return jdbc.query("SELECT e.id_motocicleta,m.chasis_completo,o.nombre,d.nombre," +
            "'EN_TRANSITO',e.despachado_en,e.recibido_en FROM envio_solicitud_motocicleta e " +
            "JOIN motocicleta m ON m.id_motocicleta=e.id_motocicleta " +
            "JOIN ubicacion o ON o.id_ubicacion=e.id_origen " +
            "JOIN ubicacion d ON d.id_ubicacion=e.id_destino " +
            "WHERE e.id_motocicleta=? AND e.recibido_en IS NULL",
            (rs, n) -> new Envio(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getObject(6,OffsetDateTime.class),rs.getObject(7,OffsetDateTime.class)),
            motocicletaId).stream().findFirst().orElse(null);
    }

    @Transactional
    public List<Envio> despachar(long solicitudId, String usuario) {
        long destinoId = bloquearSolicitud(solicitudId);
        String estado = jdbc.queryForObject("SELECT estado FROM solicitud_ensamblaje_estado_actual WHERE id_solicitud=?",
            String.class, solicitudId);
        if (!"ATENDIDA".equals(estado)) throw conflicto("Confirma primero la preparación completa.");
        var destino = ubicaciones.sede(destinoId);
        if (destino == null || !"TIENDA".equals(destino.tipo()))
            throw conflicto("La solicitud debe tener una tienda de destino activa.");
        if (!listar(solicitudId).isEmpty()) throw conflicto("La salida ya fue registrada.");
        List<Unidad> unidades = jdbc.query("SELECT a.id_motocicleta,m.chasis_completo," +
            "v.id_ubicacion,u.tipo FROM solicitud_ensamblaje_unidad a " +
            "JOIN solicitud_ensamblaje_detalle d ON d.id_detalle=a.id_detalle " +
            "JOIN motocicleta m ON m.id_motocicleta=a.id_motocicleta " +
            "LEFT JOIN motocicleta_ubicacion_actual v ON v.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN ubicacion u ON u.id_ubicacion=v.id_ubicacion " +
            "WHERE d.id_solicitud=? AND a.retirada_en IS NULL ORDER BY a.id_motocicleta",
            (rs, n) -> new Unidad(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4)),
            solicitudId);
        int solicitadas = jdbc.queryForObject("SELECT coalesce(sum(cantidad),0) FROM solicitud_ensamblaje_detalle " +
            "WHERE id_solicitud=?",Integer.class,solicitudId);
        if (unidades.size() != solicitadas || unidades.isEmpty())
            throw conflicto("La solicitud no tiene todas las unidades asignadas.");
        for (var unidad : unidades) {
            ubicaciones.bloquearMotocicleta(unidad.id());
            if (ubicaciones.enTransito(unidad.id()))
                throw conflicto("La moto " + unidad.chasis() + " ya está en tránsito.");
            if (!"ALMACEN".equals(unidad.origenTipo()))
                throw conflicto("La moto " + unidad.chasis() + " no tiene ubicación confirmada en almacén.");
            jdbc.update("INSERT INTO envio_solicitud_motocicleta " +
                "(id_solicitud,id_motocicleta,id_origen,id_destino,despachado_por) " +
                "SELECT ?,?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
                solicitudId,unidad.id(),unidad.origenId(),destinoId,usuario);
        }
        return listar(solicitudId);
    }

    @Transactional
    public List<Envio> recibir(long solicitudId, long motocicletaId, String usuario) {
        bloquearSolicitud(solicitudId);
        boolean puedeRecibir = Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM solicitud_ensamblaje s JOIN usuario u " +
            "ON u.nombre_usuario=? AND u.estado='ACTIVO' JOIN rol r ON r.id_rol=u.id_rol " +
            "WHERE s.id_solicitud=? AND (s.id_solicitante=u.id_usuario OR r.codigo='ADMIN'))",
            Boolean.class,usuario,solicitudId));
        if (!puedeRecibir) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
            "Solo quien solicitó las motos o Administración puede confirmar su recepción.");
        var pendientes = jdbc.query("SELECT id_motocicleta,id_origen,id_destino FROM envio_solicitud_motocicleta " +
            "WHERE id_solicitud=? AND id_motocicleta=? AND recibido_en IS NULL FOR UPDATE",
            (rs, n) -> new long[]{rs.getLong(1),rs.getLong(2),rs.getLong(3)},solicitudId,motocicletaId);
        if (pendientes.isEmpty()) throw conflicto("Esa motocicleta no está pendiente de recepción.");
        for (long[] envio : pendientes) {
            ubicaciones.bloquearMotocicleta(envio[0]);
            var actual = ubicaciones.actual(envio[0]);
            if (actual == null || actual.destinoId() != envio[1])
                throw conflicto("La ubicación de una moto cambió durante el envío; revisa el caso.");
            ubicaciones.trasladar(envio[0],envio[1],envio[2],usuario);
            jdbc.update("UPDATE envio_solicitud_motocicleta SET recibido_en=CURRENT_TIMESTAMP," +
                "recibido_por=(SELECT id_usuario FROM usuario WHERE nombre_usuario=?) " +
                "WHERE id_solicitud=? AND id_motocicleta=? AND recibido_en IS NULL",
                usuario,solicitudId,envio[0]);
        }
        return listar(solicitudId);
    }

    private long bloquearSolicitud(long id) {
        return jdbc.query("SELECT id_destino FROM solicitud_ensamblaje WHERE id_solicitud=? FOR UPDATE",
            (rs, n) -> rs.getLong(1),id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada."));
    }
    private void verificarSolicitud(long id) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM solicitud_ensamblaje " +
            "WHERE id_solicitud=?)",Boolean.class,id)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada.");
    }
    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);
    }
}
