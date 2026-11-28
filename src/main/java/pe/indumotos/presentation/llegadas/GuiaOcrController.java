package pe.indumotos.presentation.llegadas;

import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.llegadas.ExtraerBorradoresGuia;

@RestController
@RequestMapping("/api/v1/llegadas")
public class GuiaOcrController {
    private final ExtraerBorradoresGuia extractor;

    public GuiaOcrController(ExtraerBorradoresGuia extractor) { this.extractor = extractor; }

    @GetMapping("/{id}/sugerencias-ocr")
    public ResponseEntity<ExtraerBorradoresGuia.Resultado> sugerencias(@PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(extractor.extraer(id));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Guía no encontrada"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> errorOcr(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",
            ex.getReason() == null ? "No se pudo leer la guía" : ex.getReason()));
    }
}
