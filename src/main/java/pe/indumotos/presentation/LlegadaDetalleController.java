package pe.indumotos.presentation;

import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.ConsultarDetalleLlegada;

@RestController
@RequestMapping("/api/v1/llegadas")
public class LlegadaDetalleController {
    private final ConsultarDetalleLlegada detalle;

    public LlegadaDetalleController(ConsultarDetalleLlegada detalle) {
        this.detalle = detalle;
    }

    @GetMapping("/{id}/detalle")
    public ConsultarDetalleLlegada.Detalle consultar(@PathVariable long id) {
        return detalle.consultar(id);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Llegada no encontrada"));
    }
}
