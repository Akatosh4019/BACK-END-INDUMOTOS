package pe.indumotos.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
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
    private final JdbcTemplate jdbc;

    public PermisoController(PermisoService permisos, JdbcTemplate jdbc) {
        this.permisos = permisos;
        this.jdbc = jdbc;
    }

    private void registrarActor(Authentication actor) {
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
    }

    public record CambioRol(@NotBlank String rolCodigo) {}
    public record CambioExcepcion(@NotBlank String decision) {}

    @GetMapping("/permisos")
    public PermisoService.Accesos consultar(@PathVariable long usuarioId) {
        return permisos.consultar(usuarioId);
    }

    @PutMapping("/rol")
    @Transactional
    public PermisoService.Accesos cambiarRol(@PathVariable long usuarioId,
        @Valid @RequestBody CambioRol request, Authentication actor) {
        registrarActor(actor);
        return permisos.cambiarRol(usuarioId, request.rolCodigo());
    }

    @PutMapping("/permisos/{codigo}")
    @Transactional
    public PermisoService.Accesos guardarExcepcion(@PathVariable long usuarioId,
        @PathVariable String codigo, @Valid @RequestBody CambioExcepcion request, Authentication actor) {
        registrarActor(actor);
        return permisos.guardarExcepcion(usuarioId, codigo, request.decision());
    }

    @DeleteMapping("/permisos/{codigo}")
    @Transactional
    public PermisoService.Accesos retirarExcepcion(@PathVariable long usuarioId,
        @PathVariable String codigo, Authentication actor) {
        registrarActor(actor);
        return permisos.retirarExcepcion(usuarioId, codigo);
    }
}
