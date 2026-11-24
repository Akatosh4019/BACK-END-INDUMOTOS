package pe.indumotos.presentation;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.indumotos.application.DisponibilidadService;
import pe.indumotos.application.RecorridoMotocicleta;
import pe.indumotos.application.ReportesService;

/** Consultas de solo lectura: recorrido de una moto, estado de varias motos y reportes. */
@RestController
@RequestMapping("/api/v1")
public class SeguimientoMotoController {
    private final RecorridoMotocicleta recorrido;
    private final DisponibilidadService disponibilidad;
    private final ReportesService reportes;

    public SeguimientoMotoController(RecorridoMotocicleta recorrido, DisponibilidadService disponibilidad,
        ReportesService reportes) {
        this.recorrido = recorrido; this.disponibilidad = disponibilidad; this.reportes = reportes;
    }

    public record EstadoResumen(long id, String estado, boolean puedeOfrecerse) {}

    @GetMapping("/motocicletas/{id}/recorrido")
    public RecorridoMotocicleta.Recorrido recorrido(@PathVariable long id) { return recorrido.consultar(id); }

    /** Estado de disponibilidad de las motos que se ven en una página del inventario (máximo 50). */
    @GetMapping("/disponibilidad/estados")
    public List<EstadoResumen> estados(@RequestParam List<Long> ids) {
        if (ids.isEmpty() || ids.size() > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía entre 1 y 50 motocicletas");
        return ids.stream().distinct().map(id -> {
            var unidad = disponibilidad.consultar(id);
            return new EstadoResumen(unidad.id(), unidad.estado(), unidad.puedeOfrecerse());
        }).toList();
    }

    @GetMapping("/reportes/ventas")
    public ReportesService.ReporteVentas ventas(@RequestParam(defaultValue = "MES") String periodo) {
        return reportes.ventas(ReportesService.periodo(periodo));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",
            ex.getReason() == null ? "No se pudo completar la operación" : ex.getReason()));
    }
}
