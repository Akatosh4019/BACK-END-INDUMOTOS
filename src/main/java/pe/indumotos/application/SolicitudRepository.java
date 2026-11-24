package pe.indumotos.application;

import java.time.OffsetDateTime;
import java.util.List;

public interface SolicitudRepository {
    record Linea(long id, String marca, String modelo, String color, int cantidad) {}
    record Solicitud(long id, String solicitante, long destinoId, String destinoNombre, OffsetDateTime creadaEn,
        String observacion, String estado, List<Linea> lineas) {}
    record NuevaLinea(String marca, String modelo, String color, int cantidad) {}
    record OpcionMotocicleta(String marca, String modelo, String color) {}
    record Bloqueada(String estado, String solicitante, long destinoId) {}
    record Edicion(long id, String editor, OffsetDateTime fecha, String destinoAnterior,
        String observacionAnterior, String lineasAnteriores) {}

    Solicitud guardar(long destinoId, String observacion, String usuario, List<NuevaLinea> lineas);
    Solicitud buscar(long id);
    List<Solicitud> listar(int limite, int desplazamiento);
    boolean destinoActivo(long id);
    List<OpcionMotocicleta> opcionesMotocicleta();
    boolean combinacionRegistrada(String marca, String modelo, String color);
    List<Solicitud> filtrar(String texto, String estado, int limite, int desplazamiento);
    long contar(String texto, String estado);
    Bloqueada bloquear(long id);
    boolean esAdministrador(String usuario);
    boolean tieneUnidadesAsignadas(long id);
    void registrarEdicion(long id, String usuario);
    Solicitud reemplazar(long id, long destinoId, String observacion, List<NuevaLinea> lineas);
    List<Edicion> ediciones(long id);
}
