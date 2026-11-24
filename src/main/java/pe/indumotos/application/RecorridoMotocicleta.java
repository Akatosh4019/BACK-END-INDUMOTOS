package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reúne en una sola línea de tiempo todo lo que le pasó a una moto: llegada, registro,
 * ubicaciones, asignaciones, salidas, recepciones, cambios de disponibilidad y venta.
 * Solo lee tablas existentes; no cambia ninguna regla del sistema.
 */
@Service
public class RecorridoMotocicleta {
    private final JdbcTemplate jdbc;

    public RecorridoMotocicleta(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Moto(long id, String marca, String modelo, String color, String chasis) {}
    public record Evento(String tipo, String titulo, String detalle, String origen, String destino,
        String responsable, OffsetDateTime fecha) {}
    public record EnCamino(String origen, String destino, OffsetDateTime desde) {}
    public record Recorrido(Moto moto, String ubicacionActual, EnCamino enCamino, boolean vendida,
        List<Evento> eventos) {}

    public Recorrido consultar(long id) {
        var moto = jdbc.query("SELECT id_motocicleta,marca,modelo,color,chasis_completo FROM motocicleta " +
            "WHERE id_motocicleta=?", (rs, n) -> new Moto(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5)), id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motocicleta no encontrada"));

        List<Evento> eventos = new ArrayList<>();
        eventos.addAll(jdbc.query("SELECT l.numero_guia,l.fecha_llegada,u.nombre_usuario,a.nombre " +
            "FROM motocicleta m JOIN llegada l ON l.id_llegada=m.id_llegada " +
            "JOIN usuario u ON u.id_usuario=l.id_receptor LEFT JOIN ubicacion a ON a.id_ubicacion=l.id_almacen_recepcion " +
            "WHERE m.id_motocicleta=?", (rs, n) -> new Evento("LLEGADA", "Llegó al almacén",
                "Guía de remisión " + rs.getString(1), null, rs.getString(4), rs.getString(3),
                rs.getObject(2, OffsetDateTime.class)), id));
        eventos.addAll(jdbc.query("SELECT m.creado_en,u.nombre_usuario FROM motocicleta m " +
            "JOIN usuario u ON u.id_usuario=m.id_creador WHERE m.id_motocicleta=?",
            (rs, n) -> new Evento("REGISTRO", "Ficha registrada", "Se registraron chasis, motor y DUA",
                null, null, rs.getString(2), rs.getObject(1, OffsetDateTime.class)), id));
        // Un traslado que coincide con un envío recibido es la confirmación de esa recepción.
        eventos.addAll(jdbc.query("SELECT mv.tipo,o.nombre,d.nombre,u.nombre_usuario,mv.ocurrido_en,mv.observacion," +
            "EXISTS(SELECT 1 FROM envio_solicitud_motocicleta e WHERE e.id_motocicleta=mv.id_motocicleta " +
            "AND e.id_origen=mv.id_origen AND e.id_destino=mv.id_destino AND e.recibido_en IS NOT NULL " +
            "AND abs(extract(epoch FROM e.recibido_en-mv.ocurrido_en))<60) AS recepcion " +
            "FROM movimiento_motocicleta mv LEFT JOIN ubicacion o ON o.id_ubicacion=mv.id_origen " +
            "JOIN ubicacion d ON d.id_ubicacion=mv.id_destino JOIN usuario u ON u.id_usuario=mv.id_registrador " +
            "WHERE mv.id_motocicleta=?", (rs, n) -> {
                boolean recepcion = rs.getBoolean(7);
                String tipo = recepcion ? "RECEPCION" : rs.getString(1);
                String titulo = recepcion ? "Recibida en destino"
                    : "UBICACION".equals(tipo) ? "Ubicación registrada" : "Traslado registrado";
                return new Evento(tipo, titulo, rs.getString(6), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getObject(5, OffsetDateTime.class));
            }, id));
        eventos.addAll(jdbc.query("SELECT a.asociada_en,u.nombre_usuario,d.id_solicitud,t.nombre,a.retirada_en," +
            "r.nombre_usuario,a.motivo_retiro FROM solicitud_ensamblaje_unidad a " +
            "JOIN solicitud_ensamblaje_detalle d ON d.id_detalle=a.id_detalle " +
            "JOIN solicitud_ensamblaje s ON s.id_solicitud=d.id_solicitud JOIN ubicacion t ON t.id_ubicacion=s.id_destino " +
            "JOIN usuario u ON u.id_usuario=a.id_asignador LEFT JOIN usuario r ON r.id_usuario=a.id_retirador " +
            "WHERE a.id_motocicleta=?", (rs, n) -> {
                var lista = new ArrayList<Evento>();
                lista.add(new Evento("ASIGNACION", "Asignada a la solicitud #" + rs.getLong(3),
                    "Preparada para " + rs.getString(4), null, rs.getString(4), rs.getString(2),
                    rs.getObject(1, OffsetDateTime.class)));
                if (rs.getObject(5) != null)
                    lista.add(new Evento("RETIRO", "Retirada de la solicitud #" + rs.getLong(3), rs.getString(7),
                        null, null, rs.getString(6), rs.getObject(5, OffsetDateTime.class)));
                return lista;
            }, id).stream().flatMap(List::stream).toList());
        eventos.addAll(jdbc.query("SELECT e.id_solicitud,o.nombre,d.nombre,u.nombre_usuario,e.despachado_en " +
            "FROM envio_solicitud_motocicleta e JOIN ubicacion o ON o.id_ubicacion=e.id_origen " +
            "JOIN ubicacion d ON d.id_ubicacion=e.id_destino JOIN usuario u ON u.id_usuario=e.despachado_por " +
            "WHERE e.id_motocicleta=?", (rs, n) -> new Evento("SALIDA", "Salió del almacén (en camino)",
                "Envío de la solicitud #" + rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, OffsetDateTime.class)), id));
        eventos.addAll(jdbc.query("SELECT e.estado,e.motivo,u.nombre_usuario,e.registrado_en FROM disponibilidad_evento e " +
            "JOIN usuario u ON u.id_usuario=e.id_registrador WHERE e.id_motocicleta=?",
            (rs, n) -> new Evento("DISPONIBILIDAD", "BLOQUEADA".equals(rs.getString(1))
                ? "Bloqueada para la venta" : "Habilitada para la venta", rs.getString(2), null, null,
                rs.getString(3), rs.getObject(4, OffsetDateTime.class)), id));
        eventos.addAll(jdbc.query("SELECT t.nombre,u.nombre_usuario,v.vendida_en FROM venta_motocicleta v " +
            "JOIN ubicacion t ON t.id_ubicacion=v.id_tienda JOIN usuario u ON u.id_usuario=v.id_registrador " +
            "WHERE v.id_motocicleta=?", (rs, n) -> new Evento("VENTA", "Vendida", "Venta registrada en " + rs.getString(1),
                null, rs.getString(1), rs.getString(2), rs.getObject(3, OffsetDateTime.class)), id));
        eventos.sort(Comparator.comparing(Evento::fecha, Comparator.nullsFirst(Comparator.naturalOrder())));

        String actual = jdbc.query("SELECT u.nombre FROM motocicleta_ubicacion_actual a " +
            "JOIN ubicacion u ON u.id_ubicacion=a.id_ubicacion WHERE a.id_motocicleta=?",
            (rs, n) -> rs.getString(1), id).stream().findFirst().orElse(null);
        EnCamino enCamino = jdbc.query("SELECT o.nombre,d.nombre,e.despachado_en FROM envio_solicitud_motocicleta e " +
            "JOIN ubicacion o ON o.id_ubicacion=e.id_origen JOIN ubicacion d ON d.id_ubicacion=e.id_destino " +
            "WHERE e.id_motocicleta=? AND e.recibido_en IS NULL",
            (rs, n) -> new EnCamino(rs.getString(1), rs.getString(2), rs.getObject(3, OffsetDateTime.class)), id)
            .stream().findFirst().orElse(null);
        boolean vendida = eventos.stream().anyMatch(e -> "VENTA".equals(e.tipo()));
        return new Recorrido(moto, actual, enCamino, vendida, eventos);
    }
}
