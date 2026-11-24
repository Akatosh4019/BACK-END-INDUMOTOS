package pe.indumotos.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.SolicitudRepository;

@Repository
public class JdbcSolicitudRepository implements SolicitudRepository {
    private final JdbcTemplate jdbc;
    public JdbcSolicitudRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean destinoActivo(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM ubicacion WHERE id_ubicacion = ? AND activo AND tipo='TIENDA')", Boolean.class, id));
    }

    @Override
    public List<OpcionMotocicleta> opcionesMotocicleta() {
        return jdbc.query("SELECT marca,modelo,color FROM catalogo_motocicleta WHERE activo " +
            "ORDER BY marca,modelo,color", (rs,n) -> new OpcionMotocicleta(
                rs.getString(1),rs.getString(2),rs.getString(3)));
    }

    @Override
    public boolean combinacionRegistrada(String marca, String modelo, String color) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM catalogo_motocicleta WHERE lower(btrim(marca))=lower(?) " +
            "AND lower(btrim(modelo))=lower(?) AND lower(btrim(color))=lower(?) AND activo)",
            Boolean.class, marca, modelo, color));
    }

    @Override
    public Solicitud guardar(long destinoId, String observacion, String usuario, List<NuevaLinea> lineas) {
        Long id = jdbc.queryForObject("INSERT INTO solicitud_ensamblaje(id_solicitante,id_destino,observacion) " +
            "SELECT id_usuario,?,? FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO' RETURNING id_solicitud",
            Long.class, destinoId, observacion, usuario);
        for (var linea : lineas)
            jdbc.update("INSERT INTO solicitud_ensamblaje_detalle(id_solicitud,marca,modelo,color,cantidad) " +
                "VALUES(?,?,?,?,?)", id, linea.marca(), linea.modelo(), linea.color(), linea.cantidad());
        jdbc.update("INSERT INTO solicitud_ensamblaje_evento(id_solicitud,estado,id_registrador) " +
            "SELECT ?, 'PENDIENTE', id_usuario FROM usuario WHERE nombre_usuario=?", id, usuario);
        return buscar(id);
    }

    @Override
    public Solicitud buscar(long id) {
        return jdbc.query("SELECT s.id_solicitud,u.nombre_usuario,s.id_destino,d.nombre AS destino_nombre,s.creada_en,s.observacion,e.estado " +
            "FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante " +
            "JOIN ubicacion d ON d.id_ubicacion=s.id_destino " +
            "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud " +
            "WHERE s.id_solicitud=?", (rs,n) -> mapear(rs), id).stream().findFirst().orElse(null);
    }

    @Override
    public List<Solicitud> listar(int limite, int desplazamiento) {
        return jdbc.query("SELECT s.id_solicitud,u.nombre_usuario,s.id_destino,d.nombre AS destino_nombre,s.creada_en,s.observacion,e.estado " +
            "FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante " +
            "JOIN ubicacion d ON d.id_ubicacion=s.id_destino " +
            "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud " +
            "ORDER BY s.id_solicitud DESC LIMIT ? OFFSET ?", (rs,n) -> mapear(rs), limite, desplazamiento);
    }

    private static final String FILTRO = " WHERE (?::text IS NULL OR e.estado=?) AND (?::text IS NULL " +
        "OR CAST(s.id_solicitud AS TEXT)=? OR d.nombre ILIKE ? OR u.nombre_usuario ILIKE ? " +
        "OR EXISTS(SELECT 1 FROM solicitud_ensamblaje_detalle x WHERE x.id_solicitud=s.id_solicitud " +
        "AND (x.marca ILIKE ? OR x.modelo ILIKE ? OR x.color ILIKE ?)))";
    private static final String BASE = "FROM solicitud_ensamblaje s JOIN usuario u ON u.id_usuario=s.id_solicitante " +
        "JOIN ubicacion d ON d.id_ubicacion=s.id_destino " +
        "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud";

    private Object[] parametros(String texto, String estado) {
        String limpio = texto == null || texto.isBlank() ? null : texto.trim();
        String patron = limpio == null ? null : "%" + limpio.replace("\\", "\\\\").replace("%", "\\%")
            .replace("_", "\\_") + "%";
        return new Object[]{estado, estado, limpio, limpio, patron, patron, patron, patron, patron};
    }

    @Override
    public List<Solicitud> filtrar(String texto, String estado, int limite, int desplazamiento) {
        Object[] filtro = parametros(texto, estado);
        Object[] todos = java.util.Arrays.copyOf(filtro, filtro.length + 2);
        todos[filtro.length] = limite;
        todos[filtro.length + 1] = desplazamiento;
        return jdbc.query("SELECT s.id_solicitud,u.nombre_usuario,s.id_destino,d.nombre AS destino_nombre," +
            "s.creada_en,s.observacion,e.estado " + BASE + FILTRO +
            " ORDER BY s.id_solicitud DESC LIMIT ? OFFSET ?", (rs,n) -> mapear(rs), todos);
    }

    @Override
    public long contar(String texto, String estado) {
        Long total = jdbc.queryForObject("SELECT count(*) " + BASE + FILTRO, Long.class, parametros(texto, estado));
        return total == null ? 0 : total;
    }

    @Override
    public Bloqueada bloquear(long id) {
        return jdbc.query("SELECT e.estado,u.nombre_usuario,s.id_destino FROM solicitud_ensamblaje s " +
            "JOIN usuario u ON u.id_usuario=s.id_solicitante " +
            "JOIN solicitud_ensamblaje_estado_actual e ON e.id_solicitud=s.id_solicitud " +
            "WHERE s.id_solicitud=? FOR UPDATE OF s",
            (rs,n) -> new Bloqueada(rs.getString(1), rs.getString(2), rs.getLong(3)), id)
            .stream().findFirst().orElse(null);
    }

    @Override
    public boolean esAdministrador(String usuario) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM usuario u JOIN rol r " +
            "ON r.id_rol=u.id_rol WHERE u.nombre_usuario=? AND u.estado='ACTIVO' AND r.codigo='ADMIN')",
            Boolean.class, usuario));
    }

    @Override
    public boolean tieneUnidadesAsignadas(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM solicitud_ensamblaje_unidad a " +
            "JOIN solicitud_ensamblaje_detalle d ON d.id_detalle=a.id_detalle WHERE d.id_solicitud=?)",
            Boolean.class, id));
    }

    @Override
    public void registrarEdicion(long id, String usuario) {
        jdbc.update("INSERT INTO solicitud_ensamblaje_edicion(id_solicitud,id_editor,id_destino_anterior," +
            "observacion_anterior,lineas_anteriores) " +
            "SELECT s.id_solicitud,u.id_usuario,s.id_destino,s.observacion," +
            "(SELECT string_agg(x.marca || ' ' || x.modelo || ' · ' || x.color || ' · ' || x.cantidad || ' unidad(es)', " +
            "'; ' ORDER BY x.id_detalle) FROM solicitud_ensamblaje_detalle x WHERE x.id_solicitud=s.id_solicitud) " +
            "FROM solicitud_ensamblaje s JOIN usuario u ON u.nombre_usuario=? AND u.estado='ACTIVO' " +
            "WHERE s.id_solicitud=?", usuario, id);
    }

    @Override
    public Solicitud reemplazar(long id, long destinoId, String observacion, List<NuevaLinea> lineas) {
        jdbc.update("UPDATE solicitud_ensamblaje SET id_destino=?, observacion=?, version=version+1 " +
            "WHERE id_solicitud=?", destinoId, observacion, id);
        jdbc.update("DELETE FROM solicitud_ensamblaje_detalle WHERE id_solicitud=?", id);
        for (var linea : lineas)
            jdbc.update("INSERT INTO solicitud_ensamblaje_detalle(id_solicitud,marca,modelo,color,cantidad) " +
                "VALUES(?,?,?,?,?)", id, linea.marca(), linea.modelo(), linea.color(), linea.cantidad());
        return buscar(id);
    }

    @Override
    public List<Edicion> ediciones(long id) {
        return jdbc.query("SELECT x.id_edicion,u.nombre_usuario,x.editada_en,d.nombre,x.observacion_anterior," +
            "x.lineas_anteriores FROM solicitud_ensamblaje_edicion x JOIN usuario u ON u.id_usuario=x.id_editor " +
            "JOIN ubicacion d ON d.id_ubicacion=x.id_destino_anterior WHERE x.id_solicitud=? " +
            "ORDER BY x.id_edicion DESC",
            (rs,n) -> new Edicion(rs.getLong(1), rs.getString(2), rs.getObject(3, OffsetDateTime.class),
                rs.getString(4), rs.getString(5), rs.getString(6)), id);
    }

    private Solicitud mapear(ResultSet rs) throws SQLException {
        long id = rs.getLong("id_solicitud");
        var lineas = jdbc.query("SELECT id_detalle,marca,modelo,color,cantidad " +
            "FROM solicitud_ensamblaje_detalle WHERE id_solicitud=? ORDER BY id_detalle",
            (r,n) -> new Linea(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5)), id);
        return new Solicitud(id, rs.getString("nombre_usuario"),rs.getLong("id_destino"),rs.getString("destino_nombre"),
            rs.getObject("creada_en", OffsetDateTime.class), rs.getString("observacion"),rs.getString("estado"),lineas);
    }
}
