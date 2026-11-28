package pe.indumotos.presentation.llegadas;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.llegadas.CorregirLlegada;
import pe.indumotos.application.llegadas.RegistrarGuiaLlegada;

@RestController
@RequestMapping("/api/v1/llegadas")
public class CorreccionLlegadaController {
    private final CorregirLlegada correcciones;

    public CorreccionLlegadaController(CorregirLlegada correcciones) {
        this.correcciones = correcciones;
    }

    public record Motivo(String motivo) {}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(@PathVariable long id, @RequestBody Motivo datos,
        Authentication usuario) {
        correcciones.retirar(id, datos == null ? null : datos.motivo(), usuario.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/papelera")
    public List<CorregirLlegada.EnPapelera> papelera() { return correcciones.papelera(); }

    @GetMapping("/retiros")
    public List<CorregirLlegada.RetiroHistorico> retiros(@RequestParam(defaultValue="0") int pagina) {
        return correcciones.retiros(pagina);
    }

    @PostMapping("/{id}/restaurar")
    public ResponseEntity<Void> restaurar(@PathVariable long id, Authentication usuario) {
        correcciones.restaurar(id, usuario.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historial")
    public List<CorregirLlegada.Evento> historial(@PathVariable long id) {
        return correcciones.historial(id);
    }

    @PutMapping(path="/{id}/guias", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> reemplazar(@PathVariable long id,
        @RequestParam("imagenes") List<MultipartFile> imagenes, Authentication usuario) throws IOException {
        if (imagenes.size() < 1 || imagenes.size() > RegistrarGuiaLlegada.MAX_IMAGENES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta entre una y tres fotos.");
        var fotos = new java.util.ArrayList<RegistrarGuiaLlegada.ImagenNueva>();
        for (var imagen : imagenes) {
            if (imagen.isEmpty() || imagen.getSize() > RegistrarGuiaLlegada.MAX_BYTES)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada foto debe pesar hasta 5 MB.");
            fotos.add(new RegistrarGuiaLlegada.ImagenNueva(imagen.getOriginalFilename(),
                imagen.getContentType(), imagen.getBytes()));
        }
        correcciones.reemplazarFotos(id, fotos, usuario.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> duplicada() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "Ya existe una guía activa con ese número."));
    }
}
