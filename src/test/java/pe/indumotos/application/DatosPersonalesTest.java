package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DatosPersonalesTest {
    @Test void conservaAcentosYCerosIniciales() {
        var datos = new DatosPersonales("  María   José ", "Muñoz Pérez", " MARIA@EXAMPLE.COM ", "987654321", "01234567").normalizados();
        assertEquals("María José Muñoz Pérez", datos.nombreCompleto());
        assertEquals("maria@example.com", datos.correo());
        assertEquals("01234567", datos.dni());
    }
    @Test void rechazaDatosIncompletosOConFormatosInvalidos() {
        assertThrows(ResponseStatusException.class, () -> new DatosPersonales("Ana", " ", "ana@example.com", "987654321", "01234567").normalizados());
        assertThrows(ResponseStatusException.class, () -> new DatosPersonales("Ana", "Lima", "sin-correo", "987654321", "01234567").normalizados());
        assertThrows(ResponseStatusException.class, () -> new DatosPersonales("Ana", "Lima", "ana@example.com", "123", "01234567").normalizados());
        assertThrows(ResponseStatusException.class, () -> new DatosPersonales("Ana", "Lima", "ana@example.com", "987654321", "1234567x").normalizados());
    }
}
