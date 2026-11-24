package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface UbicacionRepository {
    record Sede(long id, String codigo, String nombre, String tipo) {}
    record Movimiento(long id, long motocicletaId, Long origenId, long destinoId,
        String destinoNombre, String tipo, String registrador, OffsetDateTime fecha) {}

    record SedeDetalle(long id, String codigo, String nombre, String tipo, String referencia, boolean activo) {}

    List<Sede> sedes();
    List<SedeDetalle> todasLasSedes();
    SedeDetalle bloquearSede(long id);
    Sede crearSede(String codigo, String nombre, String tipo, String referencia);
    SedeDetalle actualizarSede(long id, String nombre, String tipo, String referencia);
    SedeDetalle cambiarEstadoSede(long id, boolean activo);
    /** Motivo por el que la sede no puede desactivarse todavía, o null si está libre. */
    String usoVigente(long id);
    boolean tieneHistorial(long id);
    boolean existeMotocicleta(long id);
    Sede sede(long id);
    void bloquearMotocicleta(long id);
    boolean enTransito(long id);
    boolean vendida(long id);
    boolean reservadaSinEntrega(long id);
    Movimiento actual(long motocicletaId);
    List<Movimiento> historial(long motocicletaId);
    Movimiento registrar(long motocicletaId, long destinoId, String usuario);
    Movimiento trasladar(long motocicletaId, long origenId, long destinoId, String usuario);
}
