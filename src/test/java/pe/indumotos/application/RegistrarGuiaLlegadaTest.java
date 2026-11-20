package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Llegada;

class RegistrarGuiaLlegadaTest {
    private final RegistrarLlegada llegadas = mock(RegistrarLlegada.class);
    private final GuiaLlegadaRepository imagenes = mock(GuiaLlegadaRepository.class);
    private final RegistrarGuiaLlegada servicio = new RegistrarGuiaLlegada(llegadas, imagenes);

    @Test
    void guardaDosPaginasEnLaMismaLlegada() {
        byte[] paginaJpg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
        byte[] paginaPng = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        Llegada llegada = new Llegada(8L, "TG01-38337", OffsetDateTime.now(), 2L,
            "almacen", 19, 19, null);
        when(llegadas.ejecutar("TG01-38337", "almacen", 19, 19, null)).thenReturn(llegada);

        assertEquals(llegada, servicio.registrar("TG01-38337", "almacen", 19, 19, null,
            List.of(new RegistrarGuiaLlegada.ImagenNueva("pagina-1.jpg", "image/jpeg", paginaJpg),
                new RegistrarGuiaLlegada.ImagenNueva("pagina-2.png", "image/png", paginaPng))));
        verify(imagenes).guardar(8L, 1, "pagina-1.jpg", "image/jpeg", paginaJpg);
        verify(imagenes).guardar(8L, 2, "pagina-2.png", "image/png", paginaPng);
    }

    @Test
    void rechazaArchivosAusentesOFalsificadosAntesDeCrearLaLlegada() {
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null, List.of()));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.jpg", "image/jpeg", new byte[]{1, 2, 3}))));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.jpg", "image/jpeg",
                    new byte[RegistrarGuiaLlegada.MAX_BYTES + 1]))));
        verifyNoInteractions(llegadas, imagenes);
    }
}
