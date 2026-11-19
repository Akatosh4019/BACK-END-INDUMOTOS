package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GestionarSolicitudesTest {
    private final SolicitudRepository repository = mock(SolicitudRepository.class);
    private final GestionarSolicitudes servicio = new GestionarSolicitudes(repository);

    @Test
    void noPermiteSolicitudSinRenglones() {
        when(repository.destinoActivo(1)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crear(1, null, List.of(), "comercial"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).guardar(anyLong(), any(), anyString(), anyList());
    }

    @Test
    void rechazaCantidadNoPositiva() {
        when(repository.destinoActivo(1)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crear(1, null, List.of(new SolicitudRepository.NuevaLinea("Marca","Modelo","Azul",0)),
                "comercial"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaDestinoInactivo() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crear(1, null, List.of(new SolicitudRepository.NuevaLinea("Marca","Modelo","Azul",1)),
                "comercial"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).guardar(anyLong(), any(), anyString(), anyList());
    }

    @Test
    void aceptaVariosRenglonesYNormalizaDatos() {
        when(repository.destinoActivo(1)).thenReturn(true);
        when(repository.combinacionRegistrada(anyString(),anyString(),anyString())).thenReturn(true);
        servicio.crear(1, " urgente ", List.of(
            new SolicitudRepository.NuevaLinea(" M "," X "," Rojo ",2),
            new SolicitudRepository.NuevaLinea("N","Y","Azul",1)), "comercial");
        verify(repository).guardar(eq(1L), eq("urgente"), eq("comercial"),
            eq(List.of(new SolicitudRepository.NuevaLinea("M","X","Rojo",2),
                new SolicitudRepository.NuevaLinea("N","Y","Azul",1))));
    }

    @Test
    void rechazaCombinacionQueNoExisteEnFichas() {
        when(repository.destinoActivo(1)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crear(1, null, List.of(new SolicitudRepository.NuevaLinea("Marca","Modelo","Azul",1)), "comercial"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).guardar(anyLong(), any(), anyString(), anyList());
    }
}
