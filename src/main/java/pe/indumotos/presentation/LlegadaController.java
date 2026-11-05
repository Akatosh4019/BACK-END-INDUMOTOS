package pe.indumotos.presentation;

import java.net.URI;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import pe.indumotos.application.RegistrarLlegada;
import pe.indumotos.domain.Llegada;

@RestController
@RequestMapping("/api/v1/llegadas")
public class LlegadaController {
    private final RegistrarLlegada registrar;

    public LlegadaController(RegistrarLlegada registrar) {
        this.registrar = registrar;
    }

    public record NuevaLlegada(@NotBlank @Size(max = 120) String numeroGuia,
        @NotNull @Positive Integer cantidadDeclarada,
        @NotNull @Positive Integer cantidadRecibida,
        @Size(max = 1000) String observacion) {}

    @PostMapping
    public ResponseEntity<Llegada> crear(@Valid @RequestBody NuevaLlegada request,
        Authentication authentication) {
        Llegada llegada = registrar.ejecutar(request.numeroGuia(), authentication.getName(),
            request.cantidadDeclarada(), request.cantidadRecibida(), request.observacion());
        return ResponseEntity.created(URI.create("/api/v1/llegadas/" + llegada.id())).body(llegada);
    }

    @GetMapping("/{id}")
    public Llegada consultar(@PathVariable long id) {
        return registrar.consultar(id);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidos(MethodArgumentNotValidException ex) {
        var campos = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField()).distinct().sorted().toList();
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Datos inválidos para registrar la llegada", "campos", campos));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Llegada no encontrada"));
    }
}
