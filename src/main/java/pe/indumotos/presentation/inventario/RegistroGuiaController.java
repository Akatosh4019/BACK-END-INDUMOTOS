package pe.indumotos.presentation.inventario;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.inventario.RegistroGuia;
import pe.indumotos.application.inventario.RegistrarMotocicleta;

@RestController
@RequestMapping("/api/v1/llegadas/{id}/registro")
public class RegistroGuiaController {
    private final RegistroGuia registro;
    public RegistroGuiaController(RegistroGuia registro) { this.registro = registro; }

    @GetMapping
    public RegistroGuia.Estado consultar(@PathVariable long id) { return registro.consultar(id); }

    @PutMapping("/borradores/{posicion}")
    public RegistroGuia.Estado guardar(@PathVariable long id, @PathVariable int posicion,
        @RequestBody RegistroGuia.Borrador borrador, Authentication usuario) {
        return registro.guardar(id,posicion,borrador,usuario.getName());
    }

    @PostMapping("/finalizar")
    public RegistroGuia.Estado finalizar(@PathVariable long id, @Valid @RequestBody AlmacenRecepcion datos,
        Authentication usuario) {
        return registro.finalizar(id,datos.almacenId(),usuario.getName());
    }

    public record AlmacenRecepcion(@NotNull @Positive Long almacenId) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",ex.getReason()));
    }

    @ExceptionHandler(RegistrarMotocicleta.DatosInvalidos.class)
    public ResponseEntity<Map<String,Object>> fichaInvalida(RegistrarMotocicleta.DatosInvalidos ex) {
        return ResponseEntity.badRequest().body(Map.of("error","Corrige un borrador antes de finalizar.",
            "errores",ex.errores()));
    }

    @ExceptionHandler(RegistrarMotocicleta.ChasisDuplicado.class)
    public ResponseEntity<Map<String,String>> chasisDuplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
            "Un chasis ya está registrado. Revisa los borradores de esta guía."));
    }
}
