package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface UbicacionRepository {
    record Sede(long id, String codigo, String nombre, String tipo) {}
    record Movimiento(long id, long motocicletaId, Long origenId, long destinoId,
        String destinoNombre, String tipo, String registrador, OffsetDateTime fecha) {}

    List<Sede> sedes();
    Sede crearSede(String codigo, String nombre, String tipo);
    boolean existeMotocicleta(long id);
    Sede sede(long id);
    void bloquearMotocicleta(long id);
    boolean enTransito(long id);
    boolean reservadaSinEntrega(long id);
    Movimiento actual(long motocicletaId);
    List<Movimiento> historial(long motocicletaId);
    Movimiento registrar(long motocicletaId, long destinoId, String usuario);
    Movimiento trasladar(long motocicletaId, long origenId, long destinoId, String usuario);
}
