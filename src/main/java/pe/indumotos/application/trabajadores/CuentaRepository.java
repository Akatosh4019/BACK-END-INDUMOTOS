package pe.indumotos.application.trabajadores;

import java.time.OffsetDateTime;
import java.util.List;

/** Puerto de persistencia para consultar cuentas y registrar sus cambios de estado (HU-16 y HU-17). */
public interface CuentaRepository {
    record Cuenta(long id, String nombreCompleto, String nombreUsuario, String rolCodigo, String rolNombre,
        String estado, String tiendaNombre, OffsetDateTime creadoEn, OffsetDateTime actualizadoEn) {}

    record EventoEstado(String estadoAnterior, String estadoNuevo, String motivo, String actor,
        OffsetDateTime fecha) {}

    Cuenta buscar(long id);

    /** Bloquea la fila de la cuenta hasta terminar la transacción para evitar cambios simultáneos. */
    Cuenta bloquear(long id);

    /** Id de una cuenta activa por su nombre de usuario, o {@code null} si no existe o está inactiva. */
    Long idActivoPorNombre(String nombreUsuario);

    /** Cuenta los administradores activos bloqueando sus filas durante la transacción. */
    long administradoresActivos();

    /** Cambia el estado e invalida los tokens emitidos antes de este momento. */
    void cambiarEstado(long id, String estado);

    void registrarEvento(long id, String estadoAnterior, String estadoNuevo, String motivo, long actorId);

    List<EventoEstado> historial(long id);
}
