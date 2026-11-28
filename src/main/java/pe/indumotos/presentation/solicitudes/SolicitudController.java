package pe.indumotos.presentation.solicitudes;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.solicitudes.GestionarSolicitudes;
import pe.indumotos.application.solicitudes.SolicitudRepository;

@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {
    private final GestionarSolicitudes solicitudes;
    public SolicitudController(GestionarSolicitudes solicitudes) { this.solicitudes = solicitudes; }

    public record NuevaSolicitud(@NotNull @Positive Long destinoId, String observacion,
        List<SolicitudRepository.NuevaLinea> lineas) {}

    @GetMapping("/opciones-motocicleta")
    public List<SolicitudRepository.OpcionMotocicleta> opcionesMotocicleta() {
        return solicitudes.opcionesMotocicleta();
    }

    @PostMapping
    public ResponseEntity<SolicitudRepository.Solicitud> crear(@Valid @RequestBody NuevaSolicitud datos,
        Authentication usuario) {
        var creada = solicitudes.crear(datos.destinoId(), datos.observacion(), datos.lineas(), usuario.getName());
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + creada.id())).body(creada);
    }

    @GetMapping("/{id}")
    public SolicitudRepository.Solicitud consultar(@PathVariable long id) { return solicitudes.consultar(id); }

    @GetMapping
    public List<SolicitudRepository.Solicitud> listar(@RequestParam(defaultValue="0") int pagina,
        @RequestParam(defaultValue="20") int tamano) { return solicitudes.listar(pagina,tamano); }
}
