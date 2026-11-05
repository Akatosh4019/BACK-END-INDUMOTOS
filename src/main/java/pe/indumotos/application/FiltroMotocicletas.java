package pe.indumotos.application;

/**
 * Filtros públicos y paginación para consultar fichas de inventario.
 * Los tres filtros son opcionales; la consulta sin filtros lista las fichas paginadas.
 */
public record FiltroMotocicletas(String marca, String modelo, String color, int pagina, int tamano) {}
