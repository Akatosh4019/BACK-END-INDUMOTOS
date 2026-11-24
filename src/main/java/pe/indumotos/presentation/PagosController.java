package pe.indumotos.presentation;
import java.math.BigDecimal;import java.util.UUID;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.web.multipart.MultipartFile;
import pe.indumotos.application.PagosManuales;
@RestController @RequestMapping("/api/v1/pagos")
public class PagosController {
 private final PagosManuales servicio;public PagosController(PagosManuales servicio){this.servicio=servicio;}
 public record Confirmacion(String comprobanteVenta,boolean abonoVerificado){} public record Rechazo(String motivo){}
 @GetMapping public PagosManuales.Pagina lista(@RequestParam(required=false)Long tiendaId,@RequestParam(defaultValue="0")int pagina,Authentication a){return servicio.listar(a.getName(),tiendaId,pagina);}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<PagosManuales.Pago> crear(@RequestParam(required=false)Long tiendaId,@RequestParam UUID solicitud,@RequestParam long motocicletaId,@RequestParam String metodo,@RequestParam String entidad,@RequestParam String referencia,@RequestParam BigDecimal importe,@RequestPart("imagen")MultipartFile imagen,Authentication a)throws java.io.IOException{return ResponseEntity.status(201).body(servicio.registrar(a.getName(),tiendaId,solicitud,motocicletaId,metodo,entidad,referencia,importe,imagen.getBytes()));}
 @PostMapping("/{id}/confirmacion") public PagosManuales.Pago confirmar(@PathVariable long id,@RequestBody Confirmacion c,Authentication a){return servicio.confirmar(a.getName(),id,c.comprobanteVenta(),c.abonoVerificado());}
 @PostMapping("/{id}/rechazo") public PagosManuales.Pago rechazar(@PathVariable long id,@RequestBody Rechazo c,Authentication a){return servicio.rechazar(a.getName(),id,c.motivo());}
 @GetMapping("/{id}/comprobante")public ResponseEntity<byte[]> imagen(@PathVariable long id,Authentication a){var b=servicio.comprobante(a.getName(),id);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(b.mime())).header("X-Content-Type-Options","nosniff").body(b.contenido());}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<java.util.Map<String,String>> error(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("error",e.getReason()));}
}
