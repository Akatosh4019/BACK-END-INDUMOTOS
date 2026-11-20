package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import pe.indumotos.domain.Llegada;
import pe.indumotos.domain.Motocicleta;

class ConsultarDetalleLlegadaTest {
    private final LlegadaRepository llegadas = mock(LlegadaRepository.class);
    private final MotocicletaRepository motocicletas = mock(MotocicletaRepository.class);
    private final ConsultarDetalleLlegada servicio = new ConsultarDetalleLlegada(llegadas, motocicletas);

    @Test
    void llegadaSinFichasDevuelveListaVaciaYDiferenciaInformativa() {
        when(llegadas.buscarPorId(10L)).thenReturn(llegada(10L, 2));
        when(motocicletas.listarPorLlegada(10L)).thenReturn(List.of());

        var detalle = servicio.consultar(10L);

        assertEquals("GUIA-10", detalle.llegada().numeroGuia());
        assertTrue(detalle.fichas().isEmpty());
        assertEquals(0, detalle.cantidadFichas());
        assertEquals(2, detalle.diferenciaInformativa());
    }

    @Test
    void variasFichasPertenecenALaMismaLlegadaYConservanSusIds() {
        when(llegadas.buscarPorId(11L)).thenReturn(llegada(11L, 3));
        when(motocicletas.listarPorLlegada(11L)).thenReturn(List.of(ficha(5L, 11L), ficha(8L, 11L)));

        var detalle = servicio.consultar(11L);

        assertEquals(List.of(5L, 8L), detalle.fichas().stream().map(Motocicleta::id).toList());
        assertEquals(2, detalle.cantidadFichas());
        assertEquals(1, detalle.diferenciaInformativa());
        verify(motocicletas).listarPorLlegada(11L);
    }

    @Test
    void llegadaInexistenteNoConsultaFichas() {
        when(llegadas.buscarPorId(99L)).thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(EmptyResultDataAccessException.class, () -> servicio.consultar(99L));
        verifyNoInteractions(motocicletas);
    }

    private Llegada llegada(long id, int recibida) {
        return new Llegada(id, "GUIA-" + id, OffsetDateTime.now(), 2L, "almacen", recibida,
            recibida, null, "PENDIENTE");
    }

    private Motocicleta ficha(long id, long llegadaId) {
        return new Motocicleta(id, llegadaId, "RONCO", "250", "ROJO", "CHASIS-" + id,
            "MOTOR-" + id, "DUA", "1", "inventarios", OffsetDateTime.now());
    }
}
