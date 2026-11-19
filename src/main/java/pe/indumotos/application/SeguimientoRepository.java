package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface SeguimientoRepository {
    record Evento(long id, String estado, String registrador, OffsetDateTime fecha, String motivo) {}
    boolean bloquearSolicitud(long id);
    boolean existeSolicitud(long id);
    Evento estado(long id);
    Evento agregar(long id, String nuevoEstado, String motivo, String usuario);
    void liberarUnidades(long id, String motivo, String usuario);
    List<Evento> historial(long id);
}
