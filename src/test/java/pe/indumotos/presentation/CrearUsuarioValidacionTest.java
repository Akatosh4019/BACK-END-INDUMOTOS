package pe.indumotos.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CrearUsuarioValidacionTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaNombreVacio() {
        assertTrue(validator.validate(new UsuarioController.CrearUsuario("", "operador", "ClaveDePrueba_2026!", "ALMACEN", null))
            .stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombreCompleto")));
    }

    @Test
    void rechazaClaveCorta() {
        assertTrue(validator.validate(new UsuarioController.CrearUsuario("Operador", "operador", "123", "ALMACEN", null))
            .stream().anyMatch(v -> v.getPropertyPath().toString().equals("clave")));
    }

    @Test
    void rechazaUsuarioConCaracteresNoPermitidos() {
        assertTrue(validator.validate(new UsuarioController.CrearUsuario("Operador", "operador con espacios", "ClaveDePrueba_2026!", "ALMACEN", null))
            .stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombreUsuario")));
    }

    @Test
    void aceptaDatosValidos() {
        assertTrue(validator.validate(new UsuarioController.CrearUsuario("Operador", "operador_1", "ClaveDePrueba_2026!", "ALMACEN", null)).isEmpty());
    }
}
