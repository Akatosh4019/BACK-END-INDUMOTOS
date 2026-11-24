package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GestionarUbicacionesTest {
    private final UbicacionRepository repository = mock(UbicacionRepository.class);
    private final GestionarUbicaciones servicio = new GestionarUbicaciones(repository);

    @Test
    void rechazaSedeInvalidaSinGuardar() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.crearSede("A1", "Almacén", "DESCONOCIDO"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository, never()).crearSede(anyString(), anyString(), anyString(), any());
    }

    @Test
    void normalizaDatosDeSedeAntesDePersistir() {
        servicio.crearSede(" A1 ", " Almacén ", "ALMACEN");
        verify(repository).crearSede("A1", "Almacén", "ALMACEN", null);
    }

    @Test
    void rechazaMotocicletaInexistenteSinGuardar() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.registrar(9, 2, "almacen"));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).registrar(anyLong(), anyLong(), anyString());
    }

    @Test
    void rechazaDestinoInactivoSinGuardar() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.registrar(9, 2, "almacen"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repository).bloquearMotocicleta(9);
        verify(repository, never()).registrar(anyLong(), anyLong(), anyString());
    }

    @Test
    void impideRepetirUbicacionInicial() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.sede(2)).thenReturn(new UbicacionRepository.Sede(2, "ALM", "Almacén", "ALMACEN"));
        when(repository.actual(9)).thenReturn(new UbicacionRepository.Movimiento(
            1, 9, null, 2, "Almacén", "UBICACION", "almacen", null));
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.registrar(9, 2, "almacen"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).registrar(anyLong(), anyLong(), anyString());
    }

    @Test
    void guardaPrimeraUbicacionConElUsuarioAutenticado() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.sede(2)).thenReturn(new UbicacionRepository.Sede(2, "ALM", "Almacén", "ALMACEN"));
        servicio.registrar(9, 2, "almacen");
        var orden = inOrder(repository);
        orden.verify(repository).bloquearMotocicleta(9);
        orden.verify(repository).actual(9);
        orden.verify(repository).registrar(9, 2, "almacen");
    }

    @Test
    void trasladoExigeUbicacionPrevia() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> servicio.trasladar(9, 3, "almacen")).getStatusCode());
        verify(repository, never()).trasladar(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    void trasladoExigeDestinoValidoDistintoDelActual() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.actual(9)).thenReturn(new UbicacionRepository.Movimiento(
            1, 9, null, 2, "Almacen", "UBICACION", "almacen", null));
        when(repository.sede(3)).thenReturn(null);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.trasladar(9, 3, "almacen")).getStatusCode());
        when(repository.sede(2)).thenReturn(new UbicacionRepository.Sede(2, "ALM", "Almacen", "TIENDA"));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> servicio.trasladar(9, 2, "almacen")).getStatusCode());
        verify(repository, never()).trasladar(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    void trasladoConservaOrigenDestinoYActor() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.actual(9)).thenReturn(new UbicacionRepository.Movimiento(
            1, 9, null, 2, "Almacen", "UBICACION", "almacen", null));
        when(repository.sede(3)).thenReturn(new UbicacionRepository.Sede(3, "TIE", "Tienda", "TIENDA"));
        servicio.trasladar(9, 3, "roberto");
        verify(repository).bloquearMotocicleta(9);
        verify(repository).trasladar(9, 2, 3, "roberto");
    }

    @Test
    void noPermiteTrasladoManualMientrasLaMotoEstaEnTransito() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.enTransito(9)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.trasladar(9,3,"almacen"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(repository,never()).trasladar(anyLong(),anyLong(),anyLong(),anyString());
    }

    @Test
    void noPermiteSaltarseElDespachoDeUnaMotoAsignada() {
        when(repository.existeMotocicleta(9)).thenReturn(true);
        when(repository.reservadaSinEntrega(9)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.trasladar(9,3,"almacen"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(repository,never()).trasladar(anyLong(),anyLong(),anyLong(),anyString());
    }

    private final UbicacionRepository.SedeDetalle tienda =
        new UbicacionRepository.SedeDetalle(4, "T1", "Tienda", "TIENDA", null, true);

    @Test
    void noDesactivaSedeConMotosOSolicitudesAbiertas() {
        when(repository.bloquearSede(4)).thenReturn(tienda);
        when(repository.usoVigente(4)).thenReturn("Hay 2 moto(s) ubicadas en esta sede");
        var error = assertThrows(ResponseStatusException.class, () -> servicio.cambiarEstadoSede(4, false));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).cambiarEstadoSede(anyLong(), anyBoolean());
    }

    @Test
    void desactivaSedeLibreYLaPuedeReactivar() {
        when(repository.bloquearSede(4)).thenReturn(tienda);
        servicio.cambiarEstadoSede(4, false);
        verify(repository).cambiarEstadoSede(4, false);
        when(repository.bloquearSede(4)).thenReturn(new UbicacionRepository.SedeDetalle(4, "T1", "Tienda", "TIENDA", null, false));
        servicio.cambiarEstadoSede(4, true);
        verify(repository).cambiarEstadoSede(4, true);
        verify(repository, times(1)).usoVigente(4);
    }

    @Test
    void noCambiaTipoDeSedeConHistorial() {
        when(repository.bloquearSede(4)).thenReturn(tienda);
        when(repository.tieneHistorial(4)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.editarSede(4, "Tienda", "ALMACEN", null));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).actualizarSede(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void editaNombreYReferenciaSinCambiarTipo() {
        when(repository.bloquearSede(4)).thenReturn(tienda);
        servicio.editarSede(4, " Tienda Puruchuco ", "TIENDA", " Av. Javier Prado ");
        verify(repository).actualizarSede(4, "Tienda Puruchuco", "TIENDA", "Av. Javier Prado");
        verify(repository, never()).tieneHistorial(anyLong());
    }
}
