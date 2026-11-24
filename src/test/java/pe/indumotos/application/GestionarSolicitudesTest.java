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

    private final List<SolicitudRepository.NuevaLinea> lineas =
        List.of(new SolicitudRepository.NuevaLinea(" Honda ", "CB 190R", "Rojo", 2));

    @Test
    void soloSeEditaUnaSolicitudPendiente() {
        when(repository.bloquear(5)).thenReturn(new SolicitudRepository.Bloqueada("EN_PREPARACION", "comercial", 1));
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.editar(5, 1, null, lineas, "comercial"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).reemplazar(anyLong(), anyLong(), any(), anyList());
    }

    @Test
    void otraCuentaNoAdministradoraNoPuedeEditar() {
        when(repository.bloquear(5)).thenReturn(new SolicitudRepository.Bloqueada("PENDIENTE", "comercial", 1));
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.editar(5, 1, null, lineas, "otra"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).registrarEdicion(anyLong(), anyString());
    }

    @Test
    void editarGuardaLoAnteriorAntesDeReemplazar() {
        when(repository.bloquear(5)).thenReturn(new SolicitudRepository.Bloqueada("PENDIENTE", "comercial", 1));
        when(repository.destinoActivo(2)).thenReturn(true);
        when(repository.combinacionRegistrada("Honda", "CB 190R", "Rojo")).thenReturn(true);
        servicio.editar(5, 2, "  ", lineas, "COMERCIAL");
        var orden = inOrder(repository);
        orden.verify(repository).registrarEdicion(5, "COMERCIAL");
        orden.verify(repository).reemplazar(5, 2, null,
            List.of(new SolicitudRepository.NuevaLinea("Honda", "CB 190R", "Rojo", 2)));
    }

    @Test
    void administracionPuedeEditarSolicitudAjena() {
        when(repository.bloquear(5)).thenReturn(new SolicitudRepository.Bloqueada("PENDIENTE", "comercial", 1));
        when(repository.esAdministrador("admin")).thenReturn(true);
        when(repository.destinoActivo(1)).thenReturn(true);
        when(repository.combinacionRegistrada(anyString(), anyString(), anyString())).thenReturn(true);
        servicio.editar(5, 1, "Cambio de color", lineas, "admin");
        verify(repository).registrarEdicion(5, "admin");
    }

    @Test
    void combinacionInactivaNoSePuedeSolicitar() {
        when(repository.destinoActivo(1)).thenReturn(true);
        when(repository.combinacionRegistrada(anyString(), anyString(), anyString())).thenReturn(false);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crear(1, null, lineas, "comercial"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("activa del catálogo"));
    }

    @Test
    void busquedaRechazaEstadoDesconocido() {
        var error = assertThrows(ResponseStatusException.class, () -> servicio.buscar("", "PERDIDA", 0, 10));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).filtrar(any(), any(), anyInt(), anyInt());
    }

    @Test
    void busquedaDevuelveElementosYTotal() {
        when(repository.contar("honda", "PENDIENTE")).thenReturn(12L);
        var pagina = servicio.buscar("honda", "pendiente", 1, 10);
        assertEquals(12, pagina.total());
        verify(repository).filtrar("honda", "PENDIENTE", 10, 10);
    }
}
