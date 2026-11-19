package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.indumotos.domain.Motocicleta;

class EditarMotocicletaTest {
    @Test
    void actualizaLaFichaExistenteSinCambiarSuLlegada() {
        var repo = mock(MotocicletaRepository.class);
        var ficha = new Motocicleta(5L, 2L, "Honda", "XR", "Rojo", "CHASIS-001",
            "MOTOR-01", "DUA-01", "01", "admin", OffsetDateTime.now());
        when(repo.buscarPorId(5L)).thenReturn(ficha);
        when(repo.buscarIdPorChasis("CHASIS-001")).thenReturn(Optional.of(5L));
        when(repo.combinacionRegistrada("Honda", "XR", "Azul")).thenReturn(true);
        when(repo.actualizar(5L, "Honda", "XR", "Azul", "CHASIS-001", "MOTOR-01", "DUA-01", "01"))
            .thenReturn(new Motocicleta(5L, 2L, "Honda", "XR", "Azul", "CHASIS-001",
                "MOTOR-01", "DUA-01", "01", "admin", ficha.creadoEn()));
        var resultado = new EditarMotocicleta(repo).ejecutar(5L, "Honda", "XR", "Azul",
            "CHASIS-001", "MOTOR-01", "DUA-01", "01");
        assertEquals("Azul", resultado.color());
        assertEquals(2L, resultado.llegadaId());
    }

    @Test
    void rechazaChasisAsignadoAOtraFicha() {
        var repo = mock(MotocicletaRepository.class);
        when(repo.buscarPorId(5L)).thenReturn(mock(Motocicleta.class));
        when(repo.buscarIdPorChasis("CHASIS-002")).thenReturn(Optional.of(7L));
        assertThrows(RegistrarMotocicleta.ChasisDuplicado.class,
            () -> new EditarMotocicleta(repo).ejecutar(5L, "Honda", "XR", "Azul",
                "CHASIS-002", "MOTOR-01", "DUA-01", "01"));
        verify(repo, never()).actualizar(anyLong(), any(), any(), any(), any(), any(), any(), any());
    }
}
