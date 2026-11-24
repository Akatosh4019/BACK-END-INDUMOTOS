package pe.indumotos.presentation;

import java.net.URI;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.GestionarSolicitudes;
import pe.indumotos.application.SolicitudRepository;

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

    @PutMapping("/{id}")
    public SolicitudRepository.Solicitud editar(@PathVariable long id, @Valid @RequestBody NuevaSolicitud datos,
        Authentication usuario) {
        return solicitudes.editar(id, datos.destinoId(), datos.observacion(), datos.lineas(), usuario.getName());
    }

    @GetMapping("/{id}")
    public SolicitudRepository.Solicitud consultar(@PathVariable long id) { return solicitudes.consultar(id); }

    @GetMapping("/{id}/ediciones")
    public List<SolicitudRepository.Edicion> ediciones(@PathVariable long id) { return solicitudes.ediciones(id); }

    /** La respuesta sigue siendo una lista; el total para la paginación viaja en X-Total-Count. */
    @GetMapping
    public ResponseEntity<List<SolicitudRepository.Solicitud>> listar(@RequestParam(defaultValue="0") int pagina,
        @RequestParam(defaultValue="20") int tamano, @RequestParam(required=false) String buscar,
        @RequestParam(required=false) String estado) {
        var resultado = solicitudes.buscar(buscar, estado, pagina, tamano);
        return ResponseEntity.ok().header("X-Total-Count", String.valueOf(resultado.total()))
            .header("Access-Control-Expose-Headers", "X-Total-Count").body(resultado.items());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",
            ex.getReason() == null ? "No se pudo completar la operación" : ex.getReason()));
    }
}
