package pe.indumotos.application.inventario;

/**
 * Filtros públicos y paginación para consultar fichas de inventario.
 * Los tres filtros son opcionales; la consulta sin filtros lista las fichas paginadas.
 */
public record FiltroMotocicletas(String marca, String modelo, String color, String chasis,
    int pagina, int tamano) {}
