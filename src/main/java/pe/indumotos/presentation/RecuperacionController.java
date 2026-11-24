package pe.indumotos.presentation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import pe.indumotos.application.RestablecerClave;
@RestController
@RequestMapping("/api/v1/usuarios")
public class RecuperacionController {
    private final RestablecerClave servicio;
    public RecuperacionController(RestablecerClave servicio){this.servicio=servicio;}
    public record Solicitud(@NotBlank @Size(max=100) String claveAdministrador,@NotBlank @Size(min=12,max=100) String nuevaClave,@AssertTrue boolean identidadVerificada) {}
    @PostMapping("/{id}/restablecer-clave")
    public ResponseEntity<Void> restablecer(@PathVariable long id,@Valid @RequestBody Solicitud request,Authentication actor){
        servicio.ejecutar(actor.getName(),id,request.claveAdministrador(),request.nuevaClave(),request.identidadVerificada());return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<java.util.Map<String,String>> invalido(){return ResponseEntity.badRequest().body(java.util.Map.of("error","Verifica identidad y longitud de contraseña"));}
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String,String>> error(org.springframework.web.server.ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of("error",ex.getReason()));}
}
