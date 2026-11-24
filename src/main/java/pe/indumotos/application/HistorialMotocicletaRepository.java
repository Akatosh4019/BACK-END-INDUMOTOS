package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

/** Consulta de hechos registrados; no altera el inventario ni reconstruye hechos sin auditoría. */
public interface HistorialMotocicletaRepository {
    record Unidad(long id, String chasisCompleto, String marca, String modelo, String color) {}
    record Evento(String clave, OffsetDateTime fecha, String tipo, String descripcion,
        String registradoPor, String origen, String destino, Long solicitudId) {}
    record Pagina(Unidad motocicleta, List<Evento> items, int pagina, int tamano,
        long totalElementos, int totalPaginas, List<String> fuentesDisponibles) {}

    Unidad buscarUnidad(long id);
    Pagina consultar(Unidad unidad, OffsetDateTime desde, OffsetDateTime hastaExclusiva,
        int pagina, int tamano);
}
