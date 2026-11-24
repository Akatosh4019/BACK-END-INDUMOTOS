package pe.indumotos.presentation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.GestionarEnvios;

@RestController
@RequestMapping("/api/v1/motocicletas/{id}/envio-activo")
public class EnvioActivoController {
    private final GestionarEnvios envios;
    public EnvioActivoController(GestionarEnvios envios) { this.envios = envios; }
    @GetMapping
    public GestionarEnvios.Envio consultar(@PathVariable long id) { return envios.activoDeMotocicleta(id); }
}
