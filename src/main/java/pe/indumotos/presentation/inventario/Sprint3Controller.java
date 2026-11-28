package pe.indumotos.presentation.inventario;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.inventario.DiferenciasService;
import pe.indumotos.application.inventario.DisponibilidadService;

@RestController
@RequestMapping("/api/v1")
public class Sprint3Controller {
    private final DisponibilidadService disponibilidad;
    private final DiferenciasService diferencias;
    public Sprint3Controller(DisponibilidadService disponibilidad,DiferenciasService diferencias){
        this.disponibilidad=disponibilidad;this.diferencias=diferencias;
    }
    public record Cambio(@NotBlank String estado,@NotBlank @Size(max=500) String motivo) {}
    public record Reporte(@NotNull @Positive Long motocicletaId,@NotBlank String campo,
        @NotBlank @Size(max=300) String valorObservado,
        @NotBlank @Size(max=1000) String descripcion) {}
    public record Resolucion(@NotBlank @Size(max=300) String valorCorregido,
        @NotBlank @Size(max=500) String fuente,
        @NotBlank @Size(max=1000) String resolucion) {}

    @GetMapping("/disponibilidad")
    public DisponibilidadService.Pagina buscar(@RequestParam String chasis,
        @RequestParam(defaultValue="0") int pagina){return disponibilidad.buscar(chasis,pagina);}
    @GetMapping("/motocicletas/{id}/disponibilidad")
    public DisponibilidadService.Unidad consultar(@PathVariable long id){return disponibilidad.consultar(id);}
    @GetMapping("/motocicletas/{id}/disponibilidad/historial")
    public java.util.List<DisponibilidadService.Evento> historial(@PathVariable long id){
        return disponibilidad.historial(id);
    }
    @PostMapping("/motocicletas/{id}/disponibilidad")
    public DisponibilidadService.Unidad cambiar(@PathVariable long id,
        @Valid @RequestBody Cambio cambio,Authentication actor){
        return disponibilidad.cambiar(id,cambio.estado(),cambio.motivo(),actor.getName());
    }
    @GetMapping("/diferencias")
    public DiferenciasService.Pagina listar(@RequestParam(defaultValue="PENDIENTE") String estado,
        @RequestParam(defaultValue="0") int pagina,Authentication actor){
        return diferencias.listar(actor.getName(),estado,pagina);
    }
    @PostMapping("/diferencias")
    public ResponseEntity<Void> reportar(@Valid @RequestBody Reporte reporte,Authentication actor){
        diferencias.reportar(reporte.motocicletaId(),reporte.campo(),reporte.valorObservado(),
            reporte.descripcion(),actor.getName());
        return ResponseEntity.status(201).build();
    }
    @PostMapping("/diferencias/{id}/resolucion")
    public ResponseEntity<Void> resolver(@PathVariable long id,@Valid @RequestBody Resolucion datos,
        Authentication actor){
        diferencias.resolver(id,datos.valorCorregido(),datos.fuente(),datos.resolucion(),actor.getName());
        return ResponseEntity.noContent().build();
    }
}
