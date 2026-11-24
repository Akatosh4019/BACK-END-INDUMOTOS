package pe.indumotos.presentation;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.PerfilService;

@RestController
@RequestMapping("/api/v1/auth/perfil")
public class PerfilController {
    private final PerfilService perfiles;

    public PerfilController(PerfilService perfiles) { this.perfiles = perfiles; }

    public record CambiarUsuario(@NotBlank @Size(min = 3, max = 80)
        @Pattern(regexp = "[A-Za-z0-9._-]+") String nombreUsuario,
        @NotBlank String claveActual) {}
    public record CambiarClave(@NotBlank String claveActual,
        @NotBlank @Size(min = 12, max = 100) String claveNueva) {}

    @GetMapping
    public PerfilService.Perfil consultar(Authentication actor) {
        return perfiles.consultar(actor.getName());
    }

    @PutMapping("/usuario")
    public PerfilService.Perfil cambiarUsuario(@Valid @RequestBody CambiarUsuario cambio,
        Authentication actor) {
        return perfiles.cambiarUsuario(actor.getName(), cambio.nombreUsuario(), cambio.claveActual());
    }

    @PutMapping("/clave")
    public ResponseEntity<Void> cambiarClave(@Valid @RequestBody CambiarClave cambio,
        Authentication actor) {
        perfiles.cambiarClave(actor.getName(), cambio.claveActual(), cambio.claveNueva());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(Map.of("error", ex.getReason() == null ? "Solicitud inválida" : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos() {
        return ResponseEntity.badRequest().body(Map.of("error", "Revisa los datos ingresados"));
    }
}
