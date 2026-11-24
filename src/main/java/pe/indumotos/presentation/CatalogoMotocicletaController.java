package pe.indumotos.presentation;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.CatalogoMotocicletas;

@RestController
@RequestMapping("/api/v1/catalogo-motocicletas")
public class CatalogoMotocicletaController {
    private final CatalogoMotocicletas catalogo;
    public CatalogoMotocicletaController(CatalogoMotocicletas catalogo) { this.catalogo = catalogo; }

    public record NuevaOpcion(@NotBlank @Size(max=120) String marca,
        @NotBlank @Size(max=120) String modelo, @NotBlank @Size(max=120) String color) {}
    public record CambioEstado(@NotNull Boolean activo) {}

    @GetMapping
    public List<CatalogoMotocicletas.Opcion> listar() { return catalogo.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoMotocicletas.Opcion agregar(@Valid @RequestBody NuevaOpcion datos,
        Authentication usuario) {
        return catalogo.agregar(datos.marca(), datos.modelo(), datos.color(), usuario.getName());
    }

    @PutMapping("/{id}/estado")
    public CatalogoMotocicletas.Opcion cambiarEstado(@PathVariable long id,
        @Valid @RequestBody CambioEstado datos, Authentication usuario) {
        return catalogo.cambiarEstado(id, datos.activo(), usuario.getName());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",
            ex.getReason() == null ? "No se pudo completar la operación" : ex.getReason()));
    }
}
