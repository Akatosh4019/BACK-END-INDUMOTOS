package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RegistrarMotocicletaTest {
    @ParameterizedTest
    @ValueSource(strings = {"marca", "modelo", "color", "chasisCompleto",
        "numeroMotor", "dua", "itemDua"})
    void rechazaCadaCampoObligatorioSinConsultarNiGuardar(String campo) {
        var repository = mock(MotocicletaRepository.class);
        var registrar = new RegistrarMotocicleta(repository);
        java.util.Map<String, String> datos = new java.util.HashMap<>(java.util.Map.of(
            "marca", "RONCO", "modelo", "RZ 250", "color", "ROJO",
            "chasisCompleto", "DEMO-A-0042", "numeroMotor", "MOTOR-0001",
            "dua", "000123", "itemDua", "004"));
        datos.put(campo, "   ");

        var error = assertThrows(RegistrarMotocicleta.DatosInvalidos.class,
            () -> registrar.ejecutar(2L, datos.get("marca"), datos.get("modelo"),
                datos.get("color"), datos.get("chasisCompleto"), datos.get("numeroMotor"),
                datos.get("dua"), datos.get("itemDua"), "inventarios"));

        assertEquals(java.util.Set.of(campo), error.errores().keySet());
        verifyNoInteractions(repository);
    }

    @Test
    void aceptaFichaCompletaYConservaIdentificadoresComoTexto() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        var registrar = new RegistrarMotocicleta(repository);

        registrar.ejecutar(2L, " RONCO ", " RZ 250 ", " ROJO ", " DEMO-A-0042 ",
            " MOTOR-0001 ", " 000123 ", " 004 ", "inventarios");

        verify(repository).guardar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "inventarios");
    }
    @Test
    void noGuardaFichaSiLlegadaNoExiste() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(999L)).thenReturn(false);
        var registrar = new RegistrarMotocicleta(repository);

        var ex = assertThrows(ResponseStatusException.class,
            () -> registrar.ejecutar(999L, "RONCO", "RZ 250", "ROJO",
                "DEMO-A-0042", "MOTOR-0001", "000123", "004", "admin"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(repository, never()).guardar(anyLong(), any(), any(), any(), any(),
            any(), any(), any(), any());
    }
}
