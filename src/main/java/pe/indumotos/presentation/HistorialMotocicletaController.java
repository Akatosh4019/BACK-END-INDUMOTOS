package pe.indumotos.presentation;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.ConsultarHistorialMotocicleta;
import pe.indumotos.application.HistorialMotocicletaRepository.Pagina;

@RestController
@RequestMapping("/api/v1/motocicletas/{id}/historial-integral")
public class HistorialMotocicletaController {
    private final ConsultarHistorialMotocicleta consulta;
    public HistorialMotocicletaController(ConsultarHistorialMotocicleta consulta) { this.consulta = consulta; }

    @GetMapping
    public Pagina consultar(@PathVariable long id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return consulta.consultar(id, desde, hasta, pagina, tamano);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason()));
    }
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String,String>> formato() {
        return ResponseEntity.badRequest().body(Map.of("error", "Usa fechas AAAA-MM-DD y números válidos."));
    }
}
