package pe.indumotos.presentation;

import java.net.URI;
import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;
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
        @NotNull @Positive @Digits(integer = 10, fraction = 0) @DecimalMax("2147483647") BigDecimal cantidadDeclarada,
        @NotNull @Positive @Digits(integer = 10, fraction = 0) @DecimalMax("2147483647") BigDecimal cantidadRecibida,
        @Size(max = 1000) String observacion) {}

    @PostMapping
    public ResponseEntity<Llegada> crear(@Valid @RequestBody NuevaLlegada request,
        Authentication authentication) {
        Llegada llegada = registrar.ejecutar(request.numeroGuia(), authentication.getName(),
            request.cantidadDeclarada().intValueExact(), request.cantidadRecibida().intValueExact(),
            request.observacion());
        return ResponseEntity.created(URI.create("/api/v1/llegadas/" + llegada.id())).body(llegada);
    }

    @GetMapping
    public java.util.List<Llegada> listar(
        @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String guia,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue="20") int limite) {
        return registrar.buscar(guia, limite);
    }

    @GetMapping("/{id}")
    public Llegada consultar(@PathVariable long id) {
        return registrar.consultar(id);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidos(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field ->
            errores.putIfAbsent(field.getField(), mensaje(field.getField())));
        return respuestaInvalida(errores);
    }

    @ExceptionHandler(RegistrarLlegada.DatosInvalidos.class)
    public ResponseEntity<Map<String, Object>> invalidos(RegistrarLlegada.DatosInvalidos ex) {
        return respuestaInvalida(ex.errores());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> formatoInvalido(HttpMessageNotReadableException ex) {
        Throwable causa = ex;
        while (causa != null) {
            if (causa instanceof JacksonException jackson) {
                var campo = jackson.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(nombre -> nombre != null &&
                        (nombre.equals("numeroGuia") || nombre.equals("cantidadDeclarada") ||
                         nombre.equals("cantidadRecibida")))
                    .findFirst();
                if (campo.isPresent()) return respuestaInvalida(Map.of(campo.get(), mensaje(campo.get())));
            }
            causa = causa.getCause();
        }
        return ResponseEntity.badRequest().body(Map.of(
            "error", "El contenido de la llegada no es JSON válido.",
            "campos", java.util.List.of(), "errores", Map.of()));
    }

    private ResponseEntity<Map<String, Object>> respuestaInvalida(Map<String, String> errores) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Revisa los campos indicados antes de registrar la llegada.",
            "campos", errores.keySet().stream().sorted().toList(), "errores", errores));
    }

    private String mensaje(String campo) {
        return switch (campo) {
            case "numeroGuia" -> "Ingresa el número de guía.";
            case "cantidadDeclarada" -> "Ingresa una cantidad declarada mayor que cero.";
            case "cantidadRecibida" -> "Ingresa una cantidad recibida mayor que cero.";
            default -> "Revisa este campo.";
        };
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Llegada no encontrada"));
    }
}
