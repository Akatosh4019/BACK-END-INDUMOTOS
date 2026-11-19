package pe.indumotos.presentation;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public record NuevaSede(@NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank String tipo) {}

    @PostMapping("/ubicaciones")
    public ResponseEntity<UbicacionRepository.Sede> crearSede(@Valid @RequestBody NuevaSede datos) {
        var creada = ubicaciones.crearSede(datos.codigo(), datos.nombre(), datos.tipo());
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<java.util.Map<String, String>> codigoDuplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(java.util.Map.of("error", "El código de ubicación ya existe"));
    }

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
