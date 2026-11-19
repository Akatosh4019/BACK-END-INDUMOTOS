package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarSolicitudes {
    private final SolicitudRepository repository;

    public GestionarSolicitudes(SolicitudRepository repository) { this.repository = repository; }

    @Transactional
    public SolicitudRepository.Solicitud crear(long destinoId, String observacion,
        List<SolicitudRepository.NuevaLinea> lineas, String usuario) {
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
        return repository.guardar(destinoId, observacion == null || observacion.isBlank() ? null : observacion.trim(),
            usuario, normalizadas);
    }

    public SolicitudRepository.Solicitud consultar(long id) {
        var solicitud = id <= 0 ? null : repository.buscar(id);
        if (solicitud == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        return solicitud;
    }

    public List<SolicitudRepository.Solicitud> listar(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100 || (long) pagina * tamano > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación no válida");
        return repository.listar(tamano, pagina * tamano);
    }

    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }
}
