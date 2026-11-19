package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface AsignacionRepository {
    record Linea(long solicitudId, long detalleId, String marca, String modelo, String color, int cantidad,
        String estado, int asignadas) {}
    record Unidad(long asociacionId, long motocicletaId, String chasis, String asignador,
        OffsetDateTime fecha) {}
    record Candidata(long motocicletaId, String chasis, String marca, String modelo, String color) {}

    Linea bloquearLinea(long solicitudId, long detalleId);
    boolean bloquearMotocicleta(long motocicletaId);
    Candidata candidata(long motocicletaId);
    Unidad asociar(long detalleId,long motocicletaId,String usuario);
    List<Unidad> asignadas(long detalleId);
    boolean retirar(long asociacionId,long detalleId,String usuario,String motivo);
    List<Candidata> buscarElegibles(String chasis,int limite);
}
