package pe.indumotos.presentation.catalogo;

import java.io.ByteArrayInputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/catalogo-modelos")
public class CatalogoModeloController {
    public record Modelo(long id, String marca, String modelo, String descripcion,
        boolean activo, boolean tieneImagen, String[] colores) {}
    private final JdbcTemplate jdbc;

    public CatalogoModeloController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Modelo> listar() {
        return jdbc.query("SELECT m.id_modelo,m.marca,m.modelo,m.descripcion,m.activo," +
            "m.imagen_contenido IS NOT NULL AS tiene_imagen," +
            "array_agg(DISTINCT c.color ORDER BY c.color) AS colores " +
            "FROM catalogo_modelo m JOIN catalogo_motocicleta c " +
            "ON lower(btrim(c.marca))=lower(btrim(m.marca)) " +
            "AND lower(btrim(c.modelo))=lower(btrim(m.modelo)) " +
            "GROUP BY m.id_modelo ORDER BY m.marca,m.modelo",
            (rs,n) -> new Modelo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getBoolean(5),rs.getBoolean(6),(String[]) rs.getArray(7).getArray()));
    }

    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable long id) {
        var fotos = jdbc.query("SELECT imagen_mime,imagen_contenido FROM catalogo_modelo WHERE id_modelo=?",
            (rs,n) -> new Object[]{rs.getString(1),rs.getBytes(2)},id);
        if (fotos.isEmpty() || fotos.getFirst()[1] == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagen no disponible");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType((String) fotos.getFirst()[0]))
            .body((byte[]) fotos.getFirst()[1]);
    }

    @PutMapping(value="/{id}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Modelo actualizar(@PathVariable long id,
        @RequestParam String descripcion, @RequestParam boolean activo,
        @RequestParam(defaultValue="false") boolean quitarImagen,
        @RequestParam(required=false) MultipartFile imagen, Authentication actor) throws Exception {
        if (descripcion.length() > 1200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción supera 1200 caracteres");
        if (imagen != null && !imagen.isEmpty() && quitarImagen)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una imagen nueva o elimina la anterior");
        byte[] contenido = null;
        String mime = null;
        if (imagen != null && !imagen.isEmpty()) {
            mime = imagen.getContentType();
            if (!"image/jpeg".equals(mime) && !"image/png".equals(mime))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa una imagen JPG o PNG");
            if (imagen.getSize() > 3 * 1024 * 1024)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe pesar como máximo 3 MB");
            contenido = imagen.getBytes();
            var lectura = ImageIO.read(new ByteArrayInputStream(contenido));
            if (lectura == null || lectura.getWidth() < 200 || lectura.getHeight() < 200
                || lectura.getWidth() > 5000 || lectura.getHeight() > 5000)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es legible o tiene dimensiones inválidas");
        }
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, actor.getName());
        int cambios = jdbc.update("UPDATE catalogo_modelo SET descripcion=?,activo=?," +
            "imagen_mime=CASE WHEN ? THEN NULL WHEN ? IS NOT NULL THEN ? ELSE imagen_mime END," +
            "imagen_contenido=CASE WHEN ? THEN NULL WHEN ? IS NOT NULL THEN ? ELSE imagen_contenido END," +
            "actualizado_en=CURRENT_TIMESTAMP WHERE id_modelo=?", descripcion.trim(), activo,
            quitarImagen, contenido, mime, quitarImagen, contenido, contenido, id);
        if (cambios == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado");
        return listar().stream().filter(m -> m.id() == id).findFirst().orElseThrow();
    }
}
