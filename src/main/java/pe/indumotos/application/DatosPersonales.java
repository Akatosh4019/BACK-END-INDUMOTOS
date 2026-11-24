package pe.indumotos.application;

import java.util.Locale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record DatosPersonales(
    @NotBlank @Size(max=80) String nombres,
    @NotBlank @Size(max=80) String apellidos,
    @NotBlank @Email @Size(max=254) String correo,
    @NotBlank @Pattern(regexp="[0-9]{9}") String telefono,
    @NotBlank @Pattern(regexp="[0-9]{8}") String dni) {

    public DatosPersonales normalizados() {
        var datos = new DatosPersonales(limpiar(nombres), limpiar(apellidos),
            limpiar(correo).toLowerCase(Locale.ROOT), limpiar(telefono), limpiar(dni));
        if (datos.nombres.isEmpty() || datos.nombres.length() > 80 || datos.apellidos.isEmpty()
            || datos.apellidos.length() > 80 || datos.correo.length() > 254
            || !datos.correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
            || !datos.telefono.matches("[0-9]{9}") || !datos.dni.matches("[0-9]{8}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Completa nombres, apellidos, un correo válido, teléfono de 9 dígitos y DNI de 8 dígitos");
        return datos;
    }

    public String nombreCompleto() { return nombres + " " + apellidos; }
    private static String limpiar(String valor) { return valor == null ? "" : valor.strip().replaceAll("\\s+", " "); }
}
