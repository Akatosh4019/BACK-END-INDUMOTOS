package pe.indumotos.application;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarSolicitudes {
    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "EN_PREPARACION", "ATENDIDA", "RECHAZADA", "ANULADA");
    private final SolicitudRepository repository;

    public GestionarSolicitudes(SolicitudRepository repository) { this.repository = repository; }

    public record Pagina(List<SolicitudRepository.Solicitud> items, long total) {}

    @Transactional
    public SolicitudRepository.Solicitud crear(long destinoId, String observacion,
        List<SolicitudRepository.NuevaLinea> lineas, String usuario) {
        var normalizadas = validar(destinoId, observacion, lineas);
        return repository.guardar(destinoId, limpiar(observacion), usuario, normalizadas);
    }

    /**
     * Solo una solicitud PENDIENTE puede corregirse: todavía nadie preparó unidades para ella.
     * Lo que se pidió antes queda guardado en el historial de ediciones.
     */
    @Transactional
    public SolicitudRepository.Solicitud editar(long id, long destinoId, String observacion,
        List<SolicitudRepository.NuevaLinea> lineas, String usuario) {
        var actual = id <= 0 ? null : repository.bloquear(id);
        if (actual == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        if (!"PENDIENTE".equals(actual.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Solo se puede editar una solicitud pendiente; Almacén ya empezó a atenderla");
        if (!actual.solicitante().equalsIgnoreCase(usuario) && !repository.esAdministrador(usuario))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo quien registró la solicitud o Administración puede editarla");
        if (repository.tieneUnidadesAsignadas(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya tiene motos asignadas");
        var normalizadas = validar(destinoId, observacion, lineas);
        repository.registrarEdicion(id, usuario);
        return repository.reemplazar(id, destinoId, limpiar(observacion), normalizadas);
    }

    public List<SolicitudRepository.Edicion> ediciones(long id) {
        consultar(id);
        return repository.ediciones(id);
    }

    public List<SolicitudRepository.OpcionMotocicleta> opcionesMotocicleta() {
        return repository.opcionesMotocicleta();
    }

    public SolicitudRepository.Solicitud consultar(long id) {
        var solicitud = id <= 0 ? null : repository.buscar(id);
        if (solicitud == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        return solicitud;
    }

    public List<SolicitudRepository.Solicitud> listar(int pagina, int tamano) {
        validarPagina(pagina, tamano);
        return repository.listar(tamano, pagina * tamano);
    }

    /** Búsqueda por número, sede, solicitante, marca, modelo o color, con filtro opcional de estado. */
    public Pagina buscar(String texto, String estado, int pagina, int tamano) {
        validarPagina(pagina, tamano);
        String estadoLimpio = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(java.util.Locale.ROOT);
        if (estadoLimpio != null && !ESTADOS.contains(estadoLimpio))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de solicitud no válido");
        if (texto != null && texto.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La búsqueda es demasiado larga");
        return new Pagina(repository.filtrar(texto, estadoLimpio, tamano, pagina * tamano),
            repository.contar(texto, estadoLimpio));
    }

    private List<SolicitudRepository.NuevaLinea> validar(long destinoId, String observacion,
        List<SolicitudRepository.NuevaLinea> lineas) {
        if (destinoId <= 0 || !repository.destinoActivo(destinoId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destino inexistente o inactivo");
        if (lineas == null || lineas.isEmpty() || lineas.stream().anyMatch(l -> l == null ||
            vacio(l.marca()) || vacio(l.modelo()) || vacio(l.color()) || l.cantidad() <= 0 ||
            l.marca().length() > 120 || l.modelo().length() > 120 || l.color().length() > 120))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere una o más líneas completas con cantidad positiva");
        if (observacion != null && observacion.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Observación demasiado larga");
        var normalizadas = lineas.stream().map(l -> new SolicitudRepository.NuevaLinea(
            l.marca().trim(), l.modelo().trim(), l.color().trim(), l.cantidad())).toList();
        if (normalizadas.stream().anyMatch(l -> !repository.combinacionRegistrada(l.marca(), l.modelo(), l.color())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La marca, modelo y color deben corresponder a una combinación activa del catálogo");
        return normalizadas;
    }

    private void validarPagina(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100 || (long) pagina * tamano > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación no válida");
    }

    private String limpiar(String observacion) {
        return observacion == null || observacion.isBlank() ? null : observacion.trim();
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
