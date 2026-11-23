package pe.indumotos.presentation;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.GestionarEnvios;

@RestController
@RequestMapping("/api/v1/solicitudes/{id}/envios")
public class EnvioController {
    private final GestionarEnvios envios;
    public EnvioController(GestionarEnvios envios) { this.envios = envios; }

    @GetMapping
    public List<GestionarEnvios.Envio> listar(@PathVariable long id) { return envios.listar(id); }

    @PostMapping("/despacho")
    public List<GestionarEnvios.Envio> despachar(@PathVariable long id, Authentication usuario) {
        return envios.despachar(id,usuario.getName());
    }

    @PostMapping("/recepcion")
    public List<GestionarEnvios.Envio> recibir(@PathVariable long id, @Valid @RequestBody Recepcion datos,
        Authentication usuario) {
        return envios.recibir(id,datos.motocicletaId(),usuario.getName());
    }
    public record Recepcion(@NotNull @Positive Long motocicletaId) {}
}
