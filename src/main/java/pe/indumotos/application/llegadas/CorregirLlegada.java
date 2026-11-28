package pe.indumotos.application.llegadas;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CorregirLlegada {
    public record EnPapelera(long id, String numeroGuia, OffsetDateTime eliminadoEn,
        String eliminadoPor, String motivo, OffsetDateTime venceEn) {}
    public record Evento(String accion, String usuario, OffsetDateTime ocurridoEn, String motivo) {}
    public record RetiroHistorico(long llegadaId, String numeroGuia, String usuario,
        OffsetDateTime ocurridoEn, String motivo) {}

    private final JdbcTemplate jdbc;
    private final RegistrarGuiaLlegada validador;

    public CorregirLlegada(JdbcTemplate jdbc, RegistrarGuiaLlegada validador) {
        this.jdbc = jdbc;
        this.validador = validador;
    }

    @Transactional
    public void retirar(long id, String motivo, String usuario) {
        if (motivo == null || motivo.trim().length() < 10 || motivo.trim().length() > 500)
            throw error(HttpStatus.BAD_REQUEST, "Explica el motivo en 10 a 500 caracteres.");
        String numero = bloquearPendiente(id);
        if (dependencias(id)) throw error(HttpStatus.CONFLICT,
            "Esta guía tiene motos o borradores vinculados y no se puede retirar.");
        int filas = jdbc.update("UPDATE llegada SET visible_en_listado=false, eliminado_en=CURRENT_TIMESTAMP, " +
            "eliminado_por=(SELECT id_usuario FROM usuario WHERE nombre_usuario=?), motivo_eliminacion=? " +
            "WHERE id_llegada=? AND eliminado_en IS NULL", usuario, motivo.trim(), id);
        if (filas != 1) throw error(HttpStatus.CONFLICT, "La guía ya fue retirada.");
        evento(id, numero, "RETIRADA", usuario, motivo.trim());
    }

    @Transactional
    public void restaurar(long id, String usuario) {
        var datos = jdbc.query("SELECT numero_guia, eliminado_en FROM llegada WHERE id_llegada=? FOR UPDATE",
            rs -> rs.next() ? new Object[]{rs.getString(1), rs.getObject(2, OffsetDateTime.class)} : null, id);
        if (datos == null) throw error(HttpStatus.NOT_FOUND, "Guía no encontrada.");
        OffsetDateTime eliminado = (OffsetDateTime) datos[1];
        if (eliminado == null) throw error(HttpStatus.CONFLICT, "La guía no está en papelera.");
        if (!eliminado.plusDays(30).isAfter(OffsetDateTime.now()))
            throw error(HttpStatus.CONFLICT, "La guía superó los 30 días de recuperación.");
        int filas = jdbc.update("UPDATE llegada SET visible_en_listado=true, eliminado_en=NULL, " +
            "eliminado_por=NULL, motivo_eliminacion=NULL WHERE id_llegada=? " +
            "AND NOT EXISTS (SELECT 1 FROM llegada otra WHERE otra.id_llegada<>? " +
            "AND otra.visible_en_listado AND upper(btrim(otra.numero_guia))=upper(btrim(?)))",
            id, id, datos[0]);
        if (filas != 1) throw error(HttpStatus.CONFLICT,
            "Ya existe una guía activa con ese número. Resuelve el duplicado antes de restaurar.");
        evento(id, (String) datos[0], "RESTAURADA", usuario, null);
    }

    @Transactional
    public void reemplazarFotos(long id, List<RegistrarGuiaLlegada.ImagenNueva> fotos, String usuario) {
        if (fotos == null || fotos.isEmpty() || fotos.size() > RegistrarGuiaLlegada.MAX_IMAGENES)
            throw error(HttpStatus.BAD_REQUEST, "Adjunta entre una y tres fotos de la guía.");
        for (var foto : fotos) validador.validar(foto);
        String numero = bloquearPendiente(id);
        if (dependencias(id)) throw error(HttpStatus.CONFLICT,
            "La guía ya tiene trabajo de inventario; resuélvelo antes de cambiar sus fotos.");
        jdbc.update("DELETE FROM ocr_lectura_guia WHERE id_llegada=?", id);
        jdbc.update("DELETE FROM llegada_guia_imagen WHERE id_llegada=?", id);
        for (int i = 0; i < fotos.size(); i++) {
            var foto = fotos.get(i);
            jdbc.update("INSERT INTO llegada_guia_imagen(id_llegada,orden,nombre_archivo,tipo_mime,tamano,contenido) " +
                "VALUES (?,?,?,?,?,?)", id, i + 1, validador.nombreSeguro(foto, i + 1),
                foto.tipoMime(), foto.contenido().length, foto.contenido());
        }
        evento(id, numero, "FOTOS_REEMPLAZADAS", usuario, "Fotos corregidas: " + fotos.size());
    }

    public List<EnPapelera> papelera() {
        return jdbc.query("SELECT l.id_llegada,l.numero_guia,l.eliminado_en,u.nombre_usuario,l.motivo_eliminacion " +
            "FROM llegada l LEFT JOIN usuario u ON u.id_usuario=l.eliminado_por " +
            "WHERE l.eliminado_en IS NOT NULL AND l.eliminado_en > CURRENT_TIMESTAMP - INTERVAL '30 days' " +
            "ORDER BY l.eliminado_en DESC", (rs, row) -> {
                OffsetDateTime fecha = rs.getObject(3, OffsetDateTime.class);
                return new EnPapelera(rs.getLong(1), rs.getString(2), fecha,
                    rs.getString(4), rs.getString(5), fecha.plusDays(30));
            });
    }

    public List<Evento> historial(long id) {
        return jdbc.query("SELECT e.accion,u.nombre_usuario,e.ocurrido_en,e.motivo FROM llegada_evento e " +
            "LEFT JOIN usuario u ON u.id_usuario=e.usuario_id WHERE e.id_llegada=? " +
            "ORDER BY e.ocurrido_en DESC,e.id_evento DESC", (rs, row) ->
            new Evento(rs.getString(1), rs.getString(2),
                rs.getObject(3, OffsetDateTime.class), rs.getString(4)), id);
    }

    public List<RetiroHistorico> retiros(int pagina) {
        if (pagina < 0 || pagina > 100000)
            throw error(HttpStatus.BAD_REQUEST, "Página no válida.");
        return jdbc.query("SELECT e.id_llegada,e.numero_guia,u.nombre_usuario,e.ocurrido_en,e.motivo " +
            "FROM llegada_evento e LEFT JOIN usuario u ON u.id_usuario=e.usuario_id " +
            "WHERE e.accion='RETIRADA' ORDER BY e.ocurrido_en DESC,e.id_evento DESC LIMIT 10 OFFSET ?",
            (rs, row) -> new RetiroHistorico(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getObject(4, OffsetDateTime.class), rs.getString(5)), pagina * 10);
    }

    @Transactional
    public int purgarVencidas() {
        var vencidas = jdbc.query("SELECT l.id_llegada,l.numero_guia FROM llegada l " +
            "WHERE l.eliminado_en <= CURRENT_TIMESTAMP - INTERVAL '30 days' " +
            "AND EXISTS(SELECT 1 FROM llegada_guia_imagen i WHERE i.id_llegada=l.id_llegada) FOR UPDATE OF l",
            (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2)});
        for (var guia : vencidas) {
            jdbc.update("DELETE FROM ocr_lectura_guia WHERE id_llegada=?", guia[0]);
            jdbc.update("DELETE FROM llegada_guia_imagen WHERE id_llegada=?", guia[0]);
            evento((long) guia[0], (String) guia[1], "FOTOS_PURGADAS", null,
                "Finalizó el plazo de recuperación de 30 días.");
        }
        return vencidas.size();
    }

    private String bloquearPendiente(long id) {
        var numero = jdbc.query("SELECT numero_guia FROM llegada WHERE id_llegada=? " +
            "AND visible_en_listado AND eliminado_en IS NULL AND estado_registro='PENDIENTE' FOR UPDATE",
            rs -> rs.next() ? rs.getString(1) : null, id);
        if (numero == null) throw error(HttpStatus.CONFLICT,
            "Solo se puede corregir una guía activa y pendiente.");
        return numero;
    }

    private boolean dependencias(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT " +
            "EXISTS(SELECT 1 FROM motocicleta WHERE id_llegada=?) OR " +
            "EXISTS(SELECT 1 FROM borrador_motocicleta WHERE id_llegada=?)", Boolean.class, id, id));
    }

    private void evento(long id, String numero, String accion, String usuario, String motivo) {
        jdbc.update("INSERT INTO llegada_evento(id_llegada,accion,usuario_id,motivo,numero_guia) " +
            "VALUES (?,?,(SELECT id_usuario FROM usuario WHERE nombre_usuario=?),?,?)",
            id, accion, usuario, motivo, numero);
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
