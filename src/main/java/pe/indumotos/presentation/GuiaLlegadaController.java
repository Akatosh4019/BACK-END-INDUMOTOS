package pe.indumotos.presentation;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.GuiaLlegadaRepository;
import pe.indumotos.application.RegistrarGuiaLlegada;
import pe.indumotos.application.RegistrarLlegada;
import pe.indumotos.domain.Llegada;

@RestController
@RequestMapping("/api/v1/llegadas")
public class GuiaLlegadaController {
    private final RegistrarGuiaLlegada guias;

    public GuiaLlegadaController(RegistrarGuiaLlegada guias) { this.guias = guias; }

    @PostMapping(path = "/con-guia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Llegada> registrar(@RequestParam String numeroGuia,
        @RequestParam int cantidadDeclarada, @RequestParam int cantidadRecibida,
        @RequestParam(required = false) String observacion,
        @RequestParam("imagenes") List<MultipartFile> imagenes, Authentication authentication)
        throws IOException {
        if (imagenes.size() < 1 || imagenes.size() > RegistrarGuiaLlegada.MAX_IMAGENES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta entre una y tres fotos de la guía.");
        for (MultipartFile imagen : imagenes) {
            if (imagen.isEmpty() || imagen.getSize() > RegistrarGuiaLlegada.MAX_BYTES)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada foto debe pesar hasta 5 MB.");
        }
        List<RegistrarGuiaLlegada.ImagenNueva> archivos = imagenes.stream().map(imagen -> {
            try {
                return new RegistrarGuiaLlegada.ImagenNueva(imagen.getOriginalFilename(),
                    imagen.getContentType(), imagen.getBytes());
            } catch (IOException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer una foto de la guía.", ex);
            }
        }).toList();
        Llegada llegada = guias.registrar(numeroGuia, authentication.getName(),
            cantidadDeclarada, cantidadRecibida, observacion, archivos);
        return ResponseEntity.created(URI.create("/api/v1/llegadas/" + llegada.id())).body(llegada);
    }

    @GetMapping("/{llegadaId}/guias")
    public List<GuiaLlegadaRepository.Resumen> listar(@PathVariable long llegadaId) {
        return guias.listar(llegadaId);
    }

    @GetMapping("/{llegadaId}/guias/{imagenId}")
    public ResponseEntity<byte[]> imagen(@PathVariable long llegadaId, @PathVariable long imagenId) {
        GuiaLlegadaRepository.Imagen imagen = guias.consultar(llegadaId, imagenId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagen.tipoMime()))
            .cacheControl(CacheControl.noStore()).body(imagen.contenido());
    }

    @ExceptionHandler({EmptyResultDataAccessException.class})
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Guía o llegada no encontrada"));
    }

    @ExceptionHandler(RegistrarLlegada.DatosInvalidos.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(RegistrarLlegada.DatosInvalidos ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Revisa los datos de la llegada.",
            "errores", ex.errores()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> archivoInvalido(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error",
            ex.getReason() == null ? "Archivo inválido" : ex.getReason()));
    }
}
