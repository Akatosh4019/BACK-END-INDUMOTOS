package pe.indumotos.presentation.inventario;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.application.inventario.RegistrarMotocicleta;
import pe.indumotos.application.inventario.EditarMotocicleta;
import pe.indumotos.application.inventario.ConsultarMotocicletasFiltradas;
import pe.indumotos.application.inventario.PaginaMotocicletas;
import pe.indumotos.domain.Motocicleta;
import pe.indumotos.application.seguridad.PermisoService;

@RestController
@RequestMapping("/api/v1/motocicletas")
public class MotocicletaController {
    private final RegistrarMotocicleta registrar;
    private final PermisoService permisos;
    private final ConsultarMotocicletasFiltradas consultarFiltradas;
    private final EditarMotocicleta editar;
    private final JdbcTemplate jdbc;

    public MotocicletaController(RegistrarMotocicleta registrar, PermisoService permisos,
        ConsultarMotocicletasFiltradas consultarFiltradas, EditarMotocicleta editar, JdbcTemplate jdbc) {
        this.registrar = registrar;
        this.permisos = permisos;
        this.consultarFiltradas = consultarFiltradas;
        this.editar = editar;
        this.jdbc = jdbc;
    }

    public record NuevaFicha(@NotNull @Positive Long llegadaId,
        @NotBlank @Size(max = 120) String marca,
        @NotBlank @Size(max = 120) String modelo,
        @NotBlank @Size(max = 120) String color,
        @NotBlank @Size(max = 120) String chasisCompleto,
        @NotBlank @Size(max = 120) String numeroMotor,
        @NotBlank @Size(max = 120) String dua,
        @NotBlank @Size(max = 120) String itemDua) {}

    public record CambiosFicha(@NotBlank @Size(max = 120) String marca,
        @NotBlank @Size(max = 120) String modelo,
        @NotBlank @Size(max = 120) String color,
        @NotBlank @Size(max = 120) String chasisCompleto,
        @NotBlank @Size(max = 120) String numeroMotor,
        @NotBlank @Size(max = 120) String dua,
        @NotBlank @Size(max = 120) String itemDua) {}

    @PutMapping("/{id}")
    @Transactional
    public Motocicleta actualizar(@PathVariable long id, @Valid @RequestBody CambiosFicha cambios,
        Authentication actor) {
        registrarActor(actor.getName());
        return editar.ejecutar(id, cambios.marca(), cambios.modelo(), cambios.color(),
            cambios.chasisCompleto(), cambios.numeroMotor(), cambios.dua(), cambios.itemDua());
    }

    private void registrarActor(String nombre) {
        // El contexto dura solo esta transacción; el disparador lo añade a la bitácora.
        jdbc.queryForObject("SELECT set_config('indumotos.actor', ?, true)", String.class, nombre);
    }

    @PostMapping
    public ResponseEntity<Motocicleta> crear(@Valid @RequestBody NuevaFicha ficha,
        Authentication authentication) {
        Motocicleta guardada = registrar.ejecutar(ficha.llegadaId(), ficha.marca(), ficha.modelo(),
            ficha.color(), ficha.chasisCompleto(), ficha.numeroMotor(), ficha.dua(),
            ficha.itemDua(), authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/motocicletas/" + guardada.id()))
            .body(guardada);
    }

    @GetMapping("/{id}")
    public Motocicleta consultar(@PathVariable long id) {
        return registrar.consultar(id);
    }

    @GetMapping
    public PaginaMotocicletas listar(@RequestParam(required = false) String marca,
        @RequestParam(required = false) String modelo, @RequestParam(required = false) String color,
        @RequestParam(required = false) String chasis,
        @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return consultarFiltradas.consultar(marca, modelo, color, chasis, pagina, tamano);
    }

    @ExceptionHandler(RegistrarMotocicleta.ChasisDuplicado.class)
    public ResponseEntity<Map<String, Object>> chasisDuplicado(
        RegistrarMotocicleta.ChasisDuplicado ex, Authentication authentication) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", "El chasis completo ya está registrado.");
        respuesta.put("campo", "chasisCompleto");
        if (permisos.permitido(authentication.getName(), "INVENTARIO_CONSULTAR"))
            respuesta.put("fichaExistenteId", ex.fichaExistenteId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidos(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field ->
            errores.putIfAbsent(field.getField(), mensaje(field.getField())));
        return respuestaInvalida(errores);
    }

    @ExceptionHandler(RegistrarMotocicleta.DatosInvalidos.class)
    public ResponseEntity<Map<String, Object>> invalidos(RegistrarMotocicleta.DatosInvalidos ex) {
        return respuestaInvalida(ex.errores());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> formatoInvalido(HttpMessageNotReadableException ex) {
        Throwable causa = ex;
        while (causa != null) {
            if (causa instanceof JacksonException jackson) {
                var campo = jackson.getPath().stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .filter(nombre -> nombre != null && mensaje(nombre) != null)
                    .findFirst();
                if (campo.isPresent()) return respuestaInvalida(Map.of(campo.get(), mensaje(campo.get())));
            }
            causa = causa.getCause();
        }
        return ResponseEntity.badRequest().body(Map.of(
            "error", "El contenido de la ficha no es JSON válido.",
            "campos", java.util.List.of(), "errores", Map.of()));
    }

    private ResponseEntity<Map<String, Object>> respuestaInvalida(Map<String, String> errores) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Revisa los campos indicados antes de registrar la ficha.",
            "campos", errores.keySet().stream().sorted().toList(), "errores", errores));
    }

    private String mensaje(String campo) {
        return switch (campo) {
            case "llegadaId" -> "Selecciona una llegada válida.";
            case "marca" -> "Ingresa la marca.";
            case "modelo" -> "Ingresa el modelo.";
            case "color" -> "Ingresa el color.";
            case "chasisCompleto" -> "Ingresa el chasis completo.";
            case "numeroMotor" -> "Ingresa el número de motor.";
            case "dua" -> "Ingresa la DUA.";
            case "itemDua" -> "Ingresa el ítem de DUA.";
            default -> null;
        };
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> noEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Registro no encontrado"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> respuestaEstado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(Map.of("error", ex.getReason() == null ? "Solicitud inválida" : ex.getReason()));
    }
}
