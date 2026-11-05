package pe.indumotos.application;

import java.util.List;

public record PaginaBusquedaChasis(List<ResultadoBusquedaChasis> items, int pagina,
    int tamano, long totalElementos, int totalPaginas) {}
