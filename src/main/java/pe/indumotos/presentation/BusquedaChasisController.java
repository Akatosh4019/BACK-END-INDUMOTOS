package pe.indumotos.presentation;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.BuscarMotocicletasPorChasis;
import pe.indumotos.application.PaginaBusquedaChasis;

@RestController
@RequestMapping("/api/v1/motocicletas/busqueda")
public class BusquedaChasisController {
    private final BuscarMotocicletasPorChasis buscar;
    public BusquedaChasisController(BuscarMotocicletasPorChasis buscar) { this.buscar = buscar; }

    @GetMapping
    public PaginaBusquedaChasis consultar(@RequestParam(required=false) String chasisCompleto,
        @RequestParam(required=false) String ultimosCuatro,
        @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamano) {
        return buscar.buscar(chasisCompleto, ultimosCuatro, pagina, tamano);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> invalido(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason()==null?"Consulta inválida":ex.getReason()));
    }
}
