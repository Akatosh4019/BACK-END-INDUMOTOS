package pe.indumotos.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.PermisoService;

@RestController
@RequestMapping("/api/v1/usuarios/{usuarioId}")
public class PermisoController {
    private final PermisoService permisos;

    public PermisoController(PermisoService permisos) { this.permisos = permisos; }

    public record CambioRol(@NotBlank String rolCodigo) {}
    public record CambioExcepcion(@NotBlank String decision) {}

    @GetMapping("/permisos")
    public PermisoService.Accesos consultar(@PathVariable long usuarioId) {
        return permisos.consultar(usuarioId);
    }

    @PutMapping("/rol")
    public PermisoService.Accesos cambiarRol(@PathVariable long usuarioId,
        @Valid @RequestBody CambioRol request) {
        return permisos.cambiarRol(usuarioId, request.rolCodigo());
    }

    @PutMapping("/permisos/{codigo}")
    public PermisoService.Accesos guardarExcepcion(@PathVariable long usuarioId,
        @PathVariable String codigo, @Valid @RequestBody CambioExcepcion request) {
        return permisos.guardarExcepcion(usuarioId, codigo, request.decision());
    }

    @DeleteMapping("/permisos/{codigo}")
    public PermisoService.Accesos retirarExcepcion(@PathVariable long usuarioId,
        @PathVariable String codigo) {
        return permisos.retirarExcepcion(usuarioId, codigo);
    }
}
