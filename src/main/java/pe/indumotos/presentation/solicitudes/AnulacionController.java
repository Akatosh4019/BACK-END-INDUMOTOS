package pe.indumotos.presentation.solicitudes;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.solicitudes.AnularSolicitud;
import pe.indumotos.application.solicitudes.SeguimientoRepository;

@RestController
@RequestMapping("/api/v1/solicitudes/{id}/anulacion")
public class AnulacionController {
    private final AnularSolicitud servicio;
    public AnulacionController(AnularSolicitud servicio) { this.servicio=servicio; }
    public record Peticion(String motivo) {}

    @PostMapping
    public SeguimientoRepository.Evento anular(@PathVariable long id,@RequestBody Peticion datos,
        Authentication usuario) {
        return servicio.anular(id,datos.motivo(),usuario.getName());
    }
}
