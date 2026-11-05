package pe.indumotos.presentation;

import java.net.URI;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.RegistrarMotocicleta;
import pe.indumotos.domain.Motocicleta;

@RestController
@RequestMapping("/api/v1/motocicletas")
public class MotocicletaController {
    private final RegistrarMotocicleta registrar;

    public MotocicletaController(RegistrarMotocicleta registrar) {
        this.registrar = registrar;
    }

    public record NuevaFicha(@NotNull @Positive Long llegadaId,
        @NotBlank @Size(max = 120) String marca,
        @NotBlank @Size(max = 120) String modelo,
        @NotBlank @Size(max = 120) String color,
        @NotBlank @Size(max = 120) String chasisCompleto,
        @NotBlank @Size(max = 120) String numeroMotor,
        @NotBlank @Size(max = 120) String dua,
        @NotBlank @Size(max = 120) String itemDua) {}

    @PostMapping
    public ResponseEntity<Motocicleta> crear(@Valid @RequestBody NuevaFicha ficha,
        Authentication authentication) {
        Motocicleta guardada = registrar.ejecutar(ficha.llegadaId(), ficha.marca(), ficha.modelo(),
            ficha.color(), ficha.chasisCompleto(), ficha.numeroMotor(), ficha.dua(),
            ficha.itemDua(), authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/motocicletas/" + guardada.id()))
            .body(guardada);
    }

    @GetMapping("/{id}")
    public Motocicleta consultar(@PathVariable long id) {
        return registrar.consultar(id);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidos(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField()).distinct().sorted().toList();
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Datos inválidos para registrar la ficha", "campos", campos));
    }

    @ExceptionHandler({EmptyResultDataAccessException.class, ResponseStatusException.class})
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Registro no encontrado"));
    }
}
