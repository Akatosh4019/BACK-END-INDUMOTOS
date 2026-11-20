package pe.indumotos.presentation;

import java.util.Map;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping("/{id}")
    public Llegada consultar(@PathVariable long id) {
        return registrar.consultar(id);
    }

    @GetMapping
    public List<Llegada> buscarPorGuia(@RequestParam String numeroGuia) {
        return registrar.buscarPorGuia(numeroGuia);
    }

    @GetMapping("/pagina")
    public RegistrarLlegada.PaginaLlegadas listar(@RequestParam(defaultValue="") String numeroGuia,
        @RequestParam(defaultValue="0") int pagina) {
        return registrar.listar(numeroGuia, pagina);
    }

    @ExceptionHandler(RegistrarLlegada.DatosInvalidos.class)
    public ResponseEntity<Map<String, Object>> invalidos(RegistrarLlegada.DatosInvalidos ex) {
        return respuestaInvalida(ex.errores());
    }

    private ResponseEntity<Map<String, Object>> respuestaInvalida(Map<String, String> errores) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Revisa los campos indicados antes de registrar la llegada.",
            "campos", errores.keySet().stream().sorted().toList(), "errores", errores));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Llegada no encontrada"));
    }
}
