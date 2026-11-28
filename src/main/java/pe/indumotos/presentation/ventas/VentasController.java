package pe.indumotos.presentation.ventas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.ventas.VentasService;

@RestController
@RequestMapping("/api/v1")
public class VentasController {
    private final VentasService servicio;
    public VentasController(VentasService servicio) { this.servicio=servicio; }
    public record TiendaAsignada(@NotNull @Positive Long tiendaId) {}
    public record NuevaVenta(@NotNull @Positive Long motocicletaId,
        @NotBlank @Size(max=100) String referenciaPago,
        @NotBlank @Size(max=100) String referenciaComprobante,
        @NotNull @Positive BigDecimal importe) {}

    @GetMapping("/modos/compania")
    public List<VentasService.Tienda> compania() { return servicio.compania(); }
    @PutMapping("/usuarios/{id}/tienda")
    public ResponseEntity<Void> asignar(@PathVariable long id,@Valid @RequestBody TiendaAsignada datos,
        Authentication actor) {
        servicio.asignar(id,datos.tiendaId(),actor.getName()); return ResponseEntity.noContent().build();
    }
    @GetMapping("/modos/tienda")
    public VentasService.Tienda tienda(@RequestParam(required=false) Long tiendaId,Authentication actor) {
        return servicio.detalle(actor.getName(),tiendaId);
    }
    @GetMapping("/modos/tienda/stock")
    public VentasService.Pagina<VentasService.Moto> stock(@RequestParam(required=false) Long tiendaId,
        @RequestParam(defaultValue="0") int pagina,Authentication actor) {
        return servicio.stock(actor.getName(),tiendaId,pagina);
    }
    @GetMapping("/modos/tienda/ventas")
    public VentasService.Pagina<VentasService.Venta> ventas(@RequestParam(required=false) Long tiendaId,
        @RequestParam(defaultValue="0") int pagina,Authentication actor) {
        return servicio.ventas(actor.getName(),tiendaId,pagina);
    }
    @PostMapping("/modos/tienda/ventas")
    public ResponseEntity<Map<String,String>> registrar(@RequestParam(required=false) Long tiendaId,
        @Valid @RequestBody NuevaVenta venta,Authentication actor) {
        servicio.registrar(actor.getName(),tiendaId,venta.motocicletaId(),venta.referenciaPago(),
            venta.referenciaComprobante(),venta.importe());
        return ResponseEntity.status(201).body(Map.of("mensaje","Venta registrada"));
    }
}
