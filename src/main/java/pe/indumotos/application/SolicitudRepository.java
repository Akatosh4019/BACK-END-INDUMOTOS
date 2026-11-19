package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface SolicitudRepository {
    record Linea(long id, String marca, String modelo, String color, int cantidad) {}
    record Solicitud(long id, String solicitante, long destinoId, String destinoNombre, OffsetDateTime creadaEn,
        String observacion, String estado, List<Linea> lineas) {}
    record NuevaLinea(String marca, String modelo, String color, int cantidad) {}
    record OpcionMotocicleta(String marca, String modelo, String color) {}

    Solicitud guardar(long destinoId, String observacion, String usuario, List<NuevaLinea> lineas);
    Solicitud buscar(long id);
    List<Solicitud> listar(int limite, int desplazamiento);
    boolean destinoActivo(long id);
    List<OpcionMotocicleta> opcionesMotocicleta();
    boolean combinacionRegistrada(String marca, String modelo, String color);
}
