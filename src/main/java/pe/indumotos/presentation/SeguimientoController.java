package pe.indumotos.presentation;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.GestionarSeguimiento;
import pe.indumotos.application.SeguimientoRepository;

@RestController
@RequestMapping("/api/v1/solicitudes/{id}/seguimiento")
public class SeguimientoController {
    private final GestionarSeguimiento seguimiento;
    public SeguimientoController(GestionarSeguimiento seguimiento) { this.seguimiento = seguimiento; }

    public record Cambio(String estado,String motivo) {}

    @GetMapping
    public List<SeguimientoRepository.Evento> historial(@PathVariable long id) {
        return seguimiento.historial(id);
    }

    @PostMapping
    public SeguimientoRepository.Evento avanzar(@PathVariable long id,
        @RequestBody Cambio cambio, Authentication usuario) {
        return seguimiento.avanzar(id,cambio.estado(),cambio.motivo(),usuario.getName());
    }
}
