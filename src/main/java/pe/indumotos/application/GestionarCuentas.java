package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-16: desactivar o reactivar cuentas sin borrar su historial.
 * HU-17: consultar una cuenta con su estado, rol, permisos efectivos y cambios de estado.
 */
@Service
public class GestionarCuentas {
    public static final String ACTIVO = "ACTIVO";
    public static final String INACTIVO = "INACTIVO";
    private static final int MAX_MOTIVO = 500;

    public record Detalle(long id, String nombreCompleto, String nombreUsuario, String rolCodigo,
        String rolNombre, String estado, String tiendaNombre, OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn, List<String> permisosEfectivos,
        List<PermisoService.PermisoEfectivo> excepciones, CuentaRepository.EventoEstado ultimoCambioEstado,
        String nombres, String apellidos, String correo, String telefono, String dni) {}

    private final CuentaRepository cuentas;
    private final PermisoService permisos;

    public GestionarCuentas(CuentaRepository cuentas, PermisoService permisos) {
        this.cuentas = cuentas;
        this.permisos = permisos;
    }

    public Detalle consultar(long id) {
        var cuenta = cuentas.buscar(id);
        if (cuenta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        var accesos = permisos.consultar(id).permisos();
        var historial = cuentas.historial(id);
        return new Detalle(cuenta.id(), cuenta.nombreCompleto(), cuenta.nombreUsuario(), cuenta.rolCodigo(),
            cuenta.rolNombre(), cuenta.estado(), cuenta.tiendaNombre(), cuenta.creadoEn(), cuenta.actualizadoEn(),
            accesos.stream().filter(PermisoService.PermisoEfectivo::permitido)
                .map(PermisoService.PermisoEfectivo::codigo).toList(),
            accesos.stream().filter(p -> p.excepcion() != null).toList(),
            historial.isEmpty() ? null : historial.getFirst(), cuenta.nombres(), cuenta.apellidos(),
            cuenta.correo(), cuenta.telefono(), cuenta.dni());
    }

    public List<CuentaRepository.EventoEstado> historial(long id) {
        if (cuentas.buscar(id) == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        return cuentas.historial(id);
    }

    @Transactional
    public Detalle cambiarEstado(long id, String estadoSolicitado, String motivo, String actor) {
        String estado = estado(estadoSolicitado);
        String razon = motivo(motivo);
        Long actorId = cuentas.idActivoPorNombre(actor);
        if (actorId == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta del administrador inactiva");
        var cuenta = cuentas.bloquear(id);
        if (cuenta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada");
        if (cuenta.estado().equals(estado))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                estado.equals(ACTIVO) ? "La cuenta ya está activa" : "La cuenta ya está inactiva");
        if (estado.equals(INACTIVO)) {
            if (actorId == cuenta.id())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta");
            if ("ADMIN".equals(cuenta.rolCodigo()) && cuentas.administradoresActivos() <= 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar al menos un administrador activo");
        }
        cuentas.cambiarEstado(id, estado);
        cuentas.registrarEvento(id, cuenta.estado(), estado, razon, actorId);
        return consultar(id);
    }

    private String estado(String valor) {
        String estado = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!estado.equals(ACTIVO) && !estado.equals(INACTIVO))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: usa ACTIVO o INACTIVO");
        return estado;
    }

    private String motivo(String valor) {
        if (valor == null || valor.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el motivo del cambio");
        String motivo = valor.trim();
        if (motivo.length() > MAX_MOTIVO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo admite hasta 500 caracteres");
        return motivo;
    }
}
