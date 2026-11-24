package pe.indumotos.presentation;
import pe.indumotos.application.FichaAmpliada;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/v1/motocicletas/{id}")
public class FichaAmpliadaController {
 private final FichaAmpliada servicio;
 public FichaAmpliadaController(FichaAmpliada servicio){this.servicio=servicio;}
 @GetMapping("/ficha-ampliada") public FichaAmpliada.Ficha leer(@PathVariable long id){return servicio.consultar(id);}
 @PutMapping("/ficha-ampliada") public FichaAmpliada.Ficha guardar(@PathVariable long id,@RequestBody FichaAmpliada.Datos datos,Authentication a){return servicio.guardar(id,datos,a.getName());}
 @PostMapping("/fotos") public ResponseEntity<FichaAmpliada.Ficha> agregar(@PathVariable long id,@RequestPart("imagen") MultipartFile imagen,Authentication a) throws java.io.IOException{return ResponseEntity.status(201).body(servicio.foto(id,imagen.getBytes(),a.getName()));}
 @GetMapping("/fotos/{foto}") public ResponseEntity<byte[]> imagen(@PathVariable long id,@PathVariable long foto){var b=servicio.foto(id,foto);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(b.mime())).body(b.contenido());}
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<java.util.Map<String,String>> error(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("error",e.getReason()));}
}
