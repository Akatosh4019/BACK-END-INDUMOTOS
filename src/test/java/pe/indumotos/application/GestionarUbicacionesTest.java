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
        verify(repository, never()).crearSede(anyString(), anyString(), anyString());
    }

    @Test
    void normalizaDatosDeSedeAntesDePersistir() {
        servicio.crearSede(" A1 ", " Almacén ", "ALMACEN");
        verify(repository).crearSede("A1", "Almacén", "ALMACEN");
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
}
