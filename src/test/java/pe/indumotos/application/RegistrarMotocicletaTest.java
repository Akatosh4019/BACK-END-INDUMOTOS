package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RegistrarMotocicletaTest {
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
