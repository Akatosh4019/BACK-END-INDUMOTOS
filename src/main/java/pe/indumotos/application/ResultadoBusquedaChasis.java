package pe.indumotos.application;

/** Identificación logística bajo INVENTARIO_CONSULTAR; la ficha se abre por su ID. */
public record ResultadoBusquedaChasis(long id, String marca, String modelo, String color,
    String chasisCompleto) {}
