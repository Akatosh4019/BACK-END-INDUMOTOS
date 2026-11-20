package pe.indumotos.application;

import java.util.List;

public interface GuiaLlegadaRepository {
    record Imagen(long id, long llegadaId, int orden, String nombreArchivo,
        String tipoMime, byte[] contenido) {}
    record Resumen(long id, int orden, String nombreArchivo, String tipoMime, int tamano) {}

    void guardar(long llegadaId, int orden, String nombreArchivo, String tipoMime, byte[] contenido);
    List<Resumen> listar(long llegadaId);
    Imagen buscar(long llegadaId, long imagenId);
}
