package pe.indumotos.application;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermisoService {
    private final PermisoRepository repository;

    public PermisoService(PermisoRepository repository) { this.repository = repository; }

    public record PermisoEfectivo(String codigo, boolean porRol, String excepcion, boolean permitido) {}
    public record Accesos(long usuarioId, String nombreUsuario, String rolCodigo,
        List<PermisoEfectivo> permisos) {}

    public boolean permitido(String nombreUsuario, String codigo) {
        var cuenta = repository.buscarPorNombre(nombreUsuario);
        if (cuenta == null || !activa(cuenta)) return false;
        return repository.asignaciones(cuenta.id()).stream()
            .filter(p -> p.codigo().equals(codigo))
            .findFirst().map(p -> efectivo(cuenta, p)).orElse(false);
    }

    public Accesos consultar(long usuarioId) {
        var cuenta = cuenta(usuarioId);
        return new Accesos(cuenta.id(), cuenta.nombreUsuario(), cuenta.rolCodigo(),
            repository.asignaciones(usuarioId).stream()
                .map(p -> new PermisoEfectivo(p.codigo(), p.porRol(), p.excepcion(),
                    efectivo(cuenta, p))).toList());
    }

    public Accesos consultarPropios(String nombreUsuario) {
        var cuenta = repository.buscarPorNombre(nombreUsuario);
        if (cuenta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        return consultar(cuenta.id());
    }

    @Transactional
    public Accesos cambiarRol(long usuarioId, String rolCodigo) {
        cuenta(usuarioId);
        String codigo = codigo(rolCodigo);
        if (!repository.existeRolActivo(codigo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido o inactivo");
        repository.cambiarRol(usuarioId, codigo);
        return consultar(usuarioId);
    }

    @Transactional
    public Accesos guardarExcepcion(long usuarioId, String permisoCodigo, String decision) {
        var cuenta = cuenta(usuarioId);
        String codigo = permiso(permisoCodigo);
        String valor = codigo(decision);
        if (!valor.equals("PERMITIR") && !valor.equals("DENEGAR"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decisión no válida");
        if (valor.equals("PERMITIR") && sensible(codigo) && !cuenta.rolCodigo().equals("ADMIN"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Este permiso está reservado al rol administrador");
        if (valor.equals("PERMITIR") && codigo.equals("VENTAS_REGISTRAR")
            && !java.util.Set.of("ADMIN", "FACTURACION").contains(cuenta.rolCodigo()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo Facturación o Administración puede registrar ventas");
        repository.guardarExcepcion(usuarioId, codigo, valor);
        return consultar(usuarioId);
    }

    @Transactional
    public Accesos retirarExcepcion(long usuarioId, String permisoCodigo) {
        cuenta(usuarioId);
        repository.retirarExcepcion(usuarioId, permiso(permisoCodigo));
        return consultar(usuarioId);
    }

    private PermisoRepository.Cuenta cuenta(long id) {
        var cuenta = repository.buscarPorId(id);
        if (cuenta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        return cuenta;
    }

    private String permiso(String valor) {
        String codigo = codigo(valor);
        if (!repository.existePermiso(codigo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso no válido");
        return codigo;
    }

    private String codigo(String valor) {
        if (valor == null || valor.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código obligatorio");
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private boolean efectivo(PermisoRepository.Cuenta cuenta, PermisoRepository.Asignacion p) {
        if (!activa(cuenta)) return false;
        if ("DENEGAR".equals(p.excepcion())) return false;
        if (sensible(p.codigo()) && !"ADMIN".equals(cuenta.rolCodigo())) return false;
        if ("VENTAS_REGISTRAR".equals(p.codigo())
            && !java.util.Set.of("ADMIN", "FACTURACION").contains(cuenta.rolCodigo())) return false;
        return "PERMITIR".equals(p.excepcion()) || p.porRol();
    }

    private boolean activa(PermisoRepository.Cuenta cuenta) {
        return "ACTIVO".equals(cuenta.estado()) && "ACTIVO".equals(cuenta.rolEstado());
    }

    private boolean sensible(String codigo) {
        return "USUARIOS_CREAR".equals(codigo) || "PERMISOS_ASIGNAR".equals(codigo)
            || "CATALOGO_GESTIONAR".equals(codigo)
            || "COMPANIA_CONSULTAR".equals(codigo)
            || "USUARIOS_CONSULTAR".equals(codigo) || "USUARIOS_DESACTIVAR".equals(codigo);
    }
}
