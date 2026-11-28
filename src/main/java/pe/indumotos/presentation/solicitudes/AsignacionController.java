package pe.indumotos.presentation.solicitudes;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.solicitudes.AsignacionRepository;
import pe.indumotos.application.solicitudes.GestionarAsignaciones;

@RestController
@RequestMapping("/api/v1")
public class AsignacionController {
    private final GestionarAsignaciones asignaciones;
    public AsignacionController(GestionarAsignaciones asignaciones) { this.asignaciones=asignaciones; }
    public record NuevaUnidad(long motocicletaId) {}
    public record Retiro(String motivo) {}

    @GetMapping("/motocicletas/elegibles")
    public List<AsignacionRepository.Candidata> buscar(@RequestParam String chasis) {
        return asignaciones.elegibles(chasis);
    }

    @GetMapping("/solicitudes/{id}/lineas/{detalle}/unidades")
    public List<AsignacionRepository.Unidad> listar(@PathVariable long id,@PathVariable long detalle) {
        return asignaciones.listar(id,detalle);
    }

    @PostMapping("/solicitudes/{id}/lineas/{detalle}/unidades")
    public ResponseEntity<AsignacionRepository.Unidad> asociar(@PathVariable long id,@PathVariable long detalle,
        @RequestBody NuevaUnidad datos,Authentication usuario) {
        var guardada=asignaciones.asociar(id,detalle,datos.motocicletaId(),usuario.getName());
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/"+id+"/lineas/"+detalle+"/unidades"))
            .body(guardada);
    }

    @DeleteMapping("/solicitudes/{id}/lineas/{detalle}/unidades/{asociacion}")
    public ResponseEntity<Void> retirar(@PathVariable long id,@PathVariable long detalle,
        @PathVariable long asociacion,@RequestBody Retiro datos,Authentication usuario) {
        asignaciones.retirar(id,detalle,asociacion,datos.motivo(),usuario.getName());
        return ResponseEntity.noContent().build();
    }
}
