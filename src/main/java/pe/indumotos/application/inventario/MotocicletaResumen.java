package pe.indumotos.application.inventario;

/** Vista de inventario: evita exponer datos sensibles de la ficha completa. */
public record MotocicletaResumen(long id, String marca, String modelo, String color,
    String chasisVista) {}
