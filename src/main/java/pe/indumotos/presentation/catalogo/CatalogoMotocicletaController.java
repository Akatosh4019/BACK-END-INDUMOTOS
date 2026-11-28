package pe.indumotos.presentation.catalogo;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.catalogo.CatalogoMotocicletas;

@RestController
@RequestMapping("/api/v1/catalogo-motocicletas")
public class CatalogoMotocicletaController {
    private final CatalogoMotocicletas catalogo;
    public CatalogoMotocicletaController(CatalogoMotocicletas catalogo) { this.catalogo = catalogo; }

    public record NuevaOpcion(@NotBlank @Size(max=120) String marca,
        @NotBlank @Size(max=120) String modelo, @NotBlank @Size(max=120) String color) {}

    @GetMapping
    public List<CatalogoMotocicletas.Opcion> listar() { return catalogo.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoMotocicletas.Opcion agregar(@Valid @RequestBody NuevaOpcion datos,
        Authentication usuario) {
        return catalogo.agregar(datos.marca(), datos.modelo(), datos.color(), usuario.getName());
    }
}
