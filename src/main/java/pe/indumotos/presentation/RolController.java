package pe.indumotos.presentation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/roles")
public class RolController {
    public record Rol(long id, String codigo, String nombre, String descripcion, String estado,
        int trabajadores, int permisos) {}
    public record Permiso(String codigo, String nombre, String descripcion, boolean asignado) {}
    public record Detalle(Rol rol, List<Permiso> permisos) {}
    public record CambioPermisos(List<String> codigos) {}

    private static final Set<String> RESERVADOS = Set.of("USUARIOS_CREAR", "USUARIOS_CONSULTAR",
        "USUARIOS_DESACTIVAR", "PERMISOS_ASIGNAR", "CATALOGO_GESTIONAR", "COMPANIA_CONSULTAR");
    private final JdbcTemplate jdbc;

    public RolController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Rol> listar(Authentication actor) {
        exigirAdministrador(actor);
        return listarSinAutorizacion();
    }

    @GetMapping("/{codigo}")
    public Detalle consultar(@PathVariable String codigo, Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        var permisos = jdbc.query("SELECT p.codigo,p.nombre,p.descripcion," +
            "EXISTS(SELECT 1 FROM rol_permiso rp WHERE rp.id_rol=? AND rp.id_permiso=p.id_permiso) " +
            "FROM permiso p ORDER BY p.codigo", (rs,n) -> new Permiso(rs.getString(1),
            rs.getString(2),rs.getString(3),rs.getBoolean(4)), rol.id());
        return new Detalle(rol, permisos);
    }

    @PutMapping("/{codigo}/permisos")
    @Transactional
    public Detalle actualizar(@PathVariable String codigo, @RequestBody CambioPermisos cambio,
        Authentication actor) {
        exigirAdministrador(actor);
        Rol rol = buscar(codigo);
        if (!"ACTIVO".equals(rol.estado()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede editar un rol inactivo");
        if (cambio == null || cambio.codigos() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica los permisos del rol");
        Set<String> elegidos = new HashSet<>(cambio.codigos());
        if (elegidos.size() != cambio.codigos().size() || elegidos.stream().anyMatch(c -> c == null || c.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay permisos repetidos o vacíos");
        Set<String> disponibles = new HashSet<>(jdbc.query("SELECT codigo FROM permiso", (rs,n) -> rs.getString(1)));
        if (!disponibles.containsAll(elegidos))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay permisos desconocidos");
        if ("ADMIN".equals(rol.codigo()) && !elegidos.containsAll(disponibles))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administración conserva todos los permisos");
        if (!"ADMIN".equals(rol.codigo()) && (!java.util.Collections.disjoint(elegidos, RESERVADOS)
            || elegidos.contains("VENTAS_REGISTRAR") && !"FACTURACION".equals(rol.codigo())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol incluye permisos reservados");
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
        for (Permiso permiso : consultar(codigo, actor).permisos()) {
            if (permiso.asignado() && !elegidos.contains(permiso.codigo()))
                jdbc.update("DELETE FROM rol_permiso WHERE id_rol=? AND id_permiso=" +
                    "(SELECT id_permiso FROM permiso WHERE codigo=?)", rol.id(), permiso.codigo());
        }
        for (String permiso : elegidos) jdbc.update("INSERT INTO rol_permiso(id_rol,id_permiso) " +
            "SELECT ?,id_permiso FROM permiso WHERE codigo=? ON CONFLICT DO NOTHING", rol.id(), permiso);
        return consultar(codigo, actor);
    }

    private Rol buscar(String codigo) {
        return listarSinAutorizacion().stream().filter(r -> r.codigo().equalsIgnoreCase(codigo))
            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    }

    private List<Rol> listarSinAutorizacion() {
        return jdbc.query("SELECT r.id_rol,r.codigo,r.nombre,r.descripcion,r.estado," +
            "(SELECT count(*) FROM usuario u WHERE u.id_rol=r.id_rol)," +
            "(SELECT count(*) FROM rol_permiso rp WHERE rp.id_rol=r.id_rol) FROM rol r ORDER BY r.id_rol",
            (rs,n) -> new Rol(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getInt(6),rs.getInt(7)));
    }

    private void exigirAdministrador(Authentication actor) {
        if (actor == null || !Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "WHERE u.nombre_usuario=? AND u.estado='ACTIVO' AND r.codigo='ADMIN' AND r.estado='ACTIVO')",
            Boolean.class, actor.getName())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo Administración puede editar roles");
    }
}
