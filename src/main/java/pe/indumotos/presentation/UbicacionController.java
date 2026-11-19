package pe.indumotos.presentation;

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
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.GestionarUbicaciones;
import pe.indumotos.application.UbicacionRepository;

@RestController
@RequestMapping("/api/v1")
public class UbicacionController {
    private final GestionarUbicaciones ubicaciones;

    public UbicacionController(GestionarUbicaciones ubicaciones) { this.ubicaciones = ubicaciones; }

    public record NuevaUbicacion(@NotNull @Positive Long destinoId) {}

    @GetMapping("/ubicaciones")
    public List<UbicacionRepository.Sede> sedes() { return ubicaciones.sedes(); }

    @GetMapping("/motocicletas/{id}/ubicacion")
    public UbicacionRepository.Movimiento actual(@PathVariable long id) { return ubicaciones.actual(id); }

    @GetMapping("/motocicletas/{id}/ubicacion/historial")
    public List<UbicacionRepository.Movimiento> historial(@PathVariable long id) {
        return ubicaciones.historial(id);
    }

    @PostMapping("/motocicletas/{id}/ubicacion")
    public ResponseEntity<UbicacionRepository.Movimiento> registrar(@PathVariable long id,
        @Valid @RequestBody NuevaUbicacion datos, Authentication usuario) {
        var guardado = ubicaciones.registrar(id, datos.destinoId(), usuario.getName());
        return ResponseEntity.created(URI.create("/api/v1/motocicletas/" + id + "/ubicacion"))
            .body(guardado);
    }
}
