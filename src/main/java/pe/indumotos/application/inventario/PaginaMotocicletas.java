package pe.indumotos.application.inventario;

import java.util.List;

public record PaginaMotocicletas(List<MotocicletaResumen> items, int pagina, int tamano,
    long totalElementos, int totalPaginas) {}
