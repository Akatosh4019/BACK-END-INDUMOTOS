package pe.indumotos.infrastructure.inventario;

import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.inventario.MotocicletaRepository;
import pe.indumotos.application.inventario.FiltroMotocicletas;
import pe.indumotos.application.inventario.MotocicletaResumen;
import pe.indumotos.application.inventario.PaginaMotocicletas;
import pe.indumotos.domain.Motocicleta;

@Repository
public class JdbcMotocicletaRepository implements MotocicletaRepository {
    private final JdbcTemplate jdbc;

    public JdbcMotocicletaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean combinacionRegistrada(String marca, String modelo, String color) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM catalogo_motocicleta c JOIN catalogo_modelo m " +
            "ON lower(btrim(m.marca))=lower(btrim(c.marca)) AND lower(btrim(m.modelo))=lower(btrim(c.modelo)) " +
            "WHERE lower(btrim(c.marca))=lower(?) AND lower(btrim(c.modelo))=lower(?) " +
            "AND lower(btrim(c.color))=lower(?) AND m.activo)",
            Boolean.class, marca, modelo, color));
    }

    @Override
    public boolean existeLlegada(long llegadaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM llegada WHERE id_llegada = ?)", Boolean.class, llegadaId));
    }

    @Override
    public void bloquearLlegada(long llegadaId) {
        jdbc.query("SELECT id_llegada FROM llegada WHERE id_llegada=? FOR UPDATE",
            rs -> { while (rs.next()) { /* se mantiene el bloqueo hasta cerrar la transacción */ } }, llegadaId);
    }

    @Override
    public boolean registroTerminado(long llegadaId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM llegada " +
            "WHERE id_llegada=? AND estado_registro='CONFIRMADA')",Boolean.class,llegadaId));
    }

    @Override
    public Optional<Long> buscarIdPorChasis(String chasisCompleto) {
        return jdbc.query("SELECT id_motocicleta FROM motocicleta " +
            "WHERE upper(btrim(chasis_completo)) = upper(btrim(?)) ORDER BY id_motocicleta LIMIT 1",
            rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(), chasisCompleto);
    }

    @Override
    public Motocicleta guardar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario) {
        return jdbc.queryForObject(
            "WITH nueva AS (INSERT INTO motocicleta " +
            "(id_llegada, marca, modelo, color, chasis_completo, numero_motor, dua, item_dua, id_creador) " +
            "SELECT ?, ?, ?, ?, ?, ?, ?, ?, u.id_usuario FROM usuario u " +
            "WHERE u.nombre_usuario = ? AND u.estado = 'ACTIVO' " +
            "RETURNING *) SELECT n.*, u.nombre_usuario FROM nueva n " +
            "JOIN usuario u ON u.id_usuario = n.id_creador",
            this::mapear, llegadaId, marca, modelo, color, chasisCompleto, numeroMotor,
            dua, itemDua, creadorUsuario);
    }

    @Override
    public Motocicleta buscarPorId(long id) {
        return jdbc.queryForObject(
            "SELECT m.*, u.nombre_usuario FROM motocicleta m " +
            "JOIN usuario u ON u.id_usuario = m.id_creador WHERE m.id_motocicleta = ?",
            this::mapear, id);
    }

    @Override
    public Motocicleta actualizar(long id, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua) {
        return jdbc.queryForObject(
            "WITH modificada AS (UPDATE motocicleta SET marca=?,modelo=?,color=?,"
                + "chasis_completo=?,numero_motor=?,dua=?,item_dua=? "
                + "WHERE id_motocicleta=? RETURNING *) "
                + "SELECT m.*,u.nombre_usuario FROM modificada m "
                + "JOIN usuario u ON u.id_usuario=m.id_creador",
            this::mapear, marca, modelo, color, chasisCompleto, numeroMotor, dua, itemDua, id);
    }

    @Override
    public List<Motocicleta> listarPorLlegada(long llegadaId) {
        return jdbc.query(
            "SELECT m.*, u.nombre_usuario FROM motocicleta m " +
            "JOIN usuario u ON u.id_usuario = m.id_creador " +
            "WHERE m.id_llegada = ? ORDER BY m.id_motocicleta",
            this::mapear, llegadaId);
    }

    @Override
    public PaginaMotocicletas buscarPorFiltros(FiltroMotocicletas filtro) {
        List<Object> parametros = new ArrayList<>();
        String where = construirWhere(filtro, parametros);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM motocicleta m" + where,
            Long.class, parametros.toArray());
        long totalElementos = total == null ? 0 : total;
        List<Object> paginaParametros = new ArrayList<>(parametros);
        paginaParametros.add(filtro.tamano());
        paginaParametros.add((long) filtro.pagina() * filtro.tamano());
        List<MotocicletaResumen> items = jdbc.query(
            "SELECT m.id_motocicleta, m.marca, m.modelo, m.color, m.chasis_completo " +
            "FROM motocicleta m" + where + " ORDER BY m.id_motocicleta ASC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new MotocicletaResumen(rs.getLong("id_motocicleta"),
                rs.getString("marca"), rs.getString("modelo"), rs.getString("color"),
                ocultarChasis(rs.getString("chasis_completo"))), paginaParametros.toArray());
        int totalPaginas = totalElementos == 0 ? 0
            : (int) Math.ceil((double) totalElementos / filtro.tamano());
        return new PaginaMotocicletas(items, filtro.pagina(), filtro.tamano(), totalElementos,
            totalPaginas);
    }

    private String construirWhere(FiltroMotocicletas filtro, List<Object> parametros) {
        List<String> condiciones = new ArrayList<>();
        agregarFiltro(condiciones, parametros, "marca", filtro.marca());
        agregarFiltro(condiciones, parametros, "modelo", filtro.modelo());
        agregarFiltro(condiciones, parametros, "color", filtro.color());
        if (filtro.chasis() != null) {
            if (filtro.chasis().length() == 4) {
                condiciones.add("RIGHT(UPPER(BTRIM(m.chasis_completo)), 4) = UPPER(?)");
            } else {
                condiciones.add("UPPER(BTRIM(m.chasis_completo)) = UPPER(?)");
            }
            parametros.add(filtro.chasis());
        }
        return condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
    }

    private void agregarFiltro(List<String> condiciones, List<Object> parametros, String columna,
        String valor) {
        if (valor == null) return;
        condiciones.add("LOWER(m." + columna + ") = LOWER(?)");
        parametros.add(valor);
    }

    private String ocultarChasis(String chasisCompleto) {
        if (chasisCompleto == null || chasisCompleto.length() <= 4) return "****";
        return "**** " + chasisCompleto.substring(chasisCompleto.length() - 4);
    }

    private Motocicleta mapear(ResultSet rs, int rowNum) throws SQLException {
        return new Motocicleta(rs.getLong("id_motocicleta"), rs.getLong("id_llegada"),
            rs.getString("marca"), rs.getString("modelo"), rs.getString("color"),
            rs.getString("chasis_completo"), rs.getString("numero_motor"),
            rs.getString("dua"), rs.getString("item_dua"), rs.getString("nombre_usuario"),
            rs.getObject("creado_en", OffsetDateTime.class));
    }
}
