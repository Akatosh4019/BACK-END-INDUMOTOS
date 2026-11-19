package pe.indumotos.application;

import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarSeguimiento {
    private final SeguimientoRepository repository;
    public GestionarSeguimiento(SeguimientoRepository repository) { this.repository = repository; }

    public List<SeguimientoRepository.Evento> historial(long id) {
        if (id < 1 || !repository.existeSolicitud(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        return repository.historial(id);
    }

    @Transactional
    public SeguimientoRepository.Evento avanzar(long id, String estado, String motivo, String usuario) {
        if (id < 1 || !repository.bloquearSolicitud(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        var anterior = repository.estado(id);
        if (anterior == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no tiene estado inicial");
        var permitidos = switch (anterior.estado()) {
            case "PENDIENTE" -> Set.of("EN_PREPARACION", "RECHAZADA");
            case "EN_PREPARACION" -> Set.of("ATENDIDA", "RECHAZADA");
            default -> Set.<String>of();
        };
        if (estado == null || !permitidos.contains(estado))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transición de solicitud no permitida");
        if ("RECHAZADA".equals(estado) && (motivo == null || motivo.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de rechazo es obligatorio");
        if (motivo != null && motivo.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo demasiado largo");
        return repository.agregar(id, estado, motivo == null || motivo.isBlank() ? null : motivo.trim(), usuario);
    }
}
