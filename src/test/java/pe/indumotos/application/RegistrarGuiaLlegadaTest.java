package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Llegada;

class RegistrarGuiaLlegadaTest {
    private final RegistrarLlegada llegadas = mock(RegistrarLlegada.class);
    private final GuiaLlegadaRepository imagenes = mock(GuiaLlegadaRepository.class);
    private final RegistrarGuiaLlegada servicio = new RegistrarGuiaLlegada(llegadas, imagenes);

    @Test
    void guardaDosPaginasEnLaMismaLlegada() throws IOException {
        byte[] paginaJpg = imagen("jpg");
        byte[] paginaPng = imagen("png");
        Llegada llegada = new Llegada(8L, "TG01-38337", OffsetDateTime.now(), 2L,
            "almacen", 19, 19, null, "PENDIENTE");
        when(llegadas.ejecutar("TG01-38337", "almacen", 19, 19, null)).thenReturn(llegada);

        assertEquals(llegada, servicio.registrar("TG01-38337", "almacen", 19, 19, null,
            List.of(new RegistrarGuiaLlegada.ImagenNueva("pagina-1.jpg", "image/jpeg", paginaJpg),
                new RegistrarGuiaLlegada.ImagenNueva("pagina-2.png", "image/png", paginaPng))));
        verify(imagenes).guardar(8L, 1, "pagina-1.jpg", "image/jpeg", paginaJpg);
        verify(imagenes).guardar(8L, 2, "pagina-2.png", "image/png", paginaPng);
    }

    @Test
    void rechazaArchivosAusentesOFalsificadosAntesDeCrearLaLlegada() throws IOException {
        byte[] truncada = imagen("jpg");
        truncada[truncada.length - 1] = 0;
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null, List.of()));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.jpg", "image/jpeg", new byte[]{1, 2, 3}))));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.jpg", "image/jpeg", truncada))));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.png", "image/png", imagen("jpg")))));
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.jpg", "image/jpeg",
                    new byte[RegistrarGuiaLlegada.MAX_BYTES + 1]))));
        verifyNoInteractions(llegadas, imagenes);
    }

    @Test
    void rechazaImagenDemasiadoPequena() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB), "png", salida);
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.png", "image/png", salida.toByteArray()))));
        verifyNoInteractions(llegadas, imagenes);
    }

    @Test
    void rechazaImagenVaciaAunqueSePuedeDecodificar() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(800, 1000, BufferedImage.TYPE_INT_RGB), "png", salida);
        assertThrows(ResponseStatusException.class,
            () -> servicio.registrar("G", "almacen", 1, 1, null,
                List.of(new RegistrarGuiaLlegada.ImagenNueva("guia.png", "image/png", salida.toByteArray()))));
        verifyNoInteractions(llegadas, imagenes);
    }

    private byte[] imagen(String formato) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        BufferedImage pagina = new BufferedImage(800, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D pincel = pagina.createGraphics();
        try {
            pincel.setColor(Color.WHITE);
            pincel.fillRect(0, 0, 800, 1000);
            pincel.setColor(Color.BLACK);
            for (int y = 80; y < 920; y += 24) pincel.fillRect(40, y, 550, 6);
        } finally {
            pincel.dispose();
        }
        ImageIO.write(pagina, formato, salida);
        return salida.toByteArray();
    }
}
