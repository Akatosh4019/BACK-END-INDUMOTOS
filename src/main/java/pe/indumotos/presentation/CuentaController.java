package pe.indumotos.presentation;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.CuentaRepository;
import pe.indumotos.application.GestionarCuentas;

/** HU-16 (desactivar/reactivar) y HU-17 (consultar cuenta, estado y permisos). */
@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}")
public class CuentaController {
    private final GestionarCuentas cuentas;

    public CuentaController(GestionarCuentas cuentas) { this.cuentas = cuentas; }

    public record CambioEstado(@NotBlank String estado, @NotBlank @Size(max = 500) String motivo) {}

    @GetMapping
    public GestionarCuentas.Detalle consultar(@PathVariable long usuarioId) {
        return cuentas.consultar(usuarioId);
    }

    @GetMapping("/estado/historial")
    public List<CuentaRepository.EventoEstado> historial(@PathVariable long usuarioId) {
        return cuentas.historial(usuarioId);
    }

    @PutMapping("/estado")
    public GestionarCuentas.Detalle cambiarEstado(@PathVariable long usuarioId,
        @Valid @RequestBody CambioEstado cambio, Authentication actor) {
        return cuentas.cambiarEstado(usuarioId, cambio.estado(), cambio.motivo(), actor.getName());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(Map.of("error", ex.getReason() == null ? "Solicitud inválida" : ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException ex) {
        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField()).distinct().sorted().toList();
        return ResponseEntity.badRequest().body(Map.of("error", "Escribe el estado y el motivo", "campos", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido() {
        return ResponseEntity.badRequest().body(Map.of("error", "Cuerpo de la solicitud no válido"));
    }
}
