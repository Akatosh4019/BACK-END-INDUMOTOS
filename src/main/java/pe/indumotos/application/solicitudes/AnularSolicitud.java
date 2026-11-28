package pe.indumotos.application.solicitudes;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnularSolicitud {
    private final SeguimientoRepository repository;
    public AnularSolicitud(SeguimientoRepository repository) { this.repository=repository; }

    @Transactional
    public SeguimientoRepository.Evento anular(long id,String motivo,String usuario) {
        if (id < 1 || !repository.bloquearSolicitud(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada");
        var estado=repository.estado(id);
        // Criterio conservador hasta que Product Owner confirme el punto de corte.
        if (estado == null || !"PENDIENTE".equals(estado.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se pueden anular solicitudes pendientes");
        if (motivo == null || motivo.isBlank() || motivo.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indique un motivo de hasta 1000 caracteres");
        return repository.agregar(id,"ANULADA",motivo.trim(),usuario);
    }
}
