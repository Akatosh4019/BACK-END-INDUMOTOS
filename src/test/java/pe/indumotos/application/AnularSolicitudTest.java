package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AnularSolicitudTest {
    private final SeguimientoRepository repo=mock(SeguimientoRepository.class);
    private final AnularSolicitud servicio=new AnularSolicitud(repo);

    @Test void inexistente() {
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
            () -> servicio.anular(8,"error","comercial")).getStatusCode());
    }

    @Test void noAnulaLuegoDeEmpezarPreparacion() {
        when(repo.bloquearSolicitud(8)).thenReturn(true);
        when(repo.estado(8)).thenReturn(new SeguimientoRepository.Evento(1,"EN_PREPARACION","comercial",null,null));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
            () -> servicio.anular(8,"error","comercial")).getStatusCode());
        verify(repo,never()).agregar(anyLong(),any(),any(),any());
    }

    @Test void exigeMotivo() {
        when(repo.bloquearSolicitud(8)).thenReturn(true);
        when(repo.estado(8)).thenReturn(new SeguimientoRepository.Evento(1,"PENDIENTE","comercial",null,null));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> servicio.anular(8," ","comercial")).getStatusCode());
        verify(repo,never()).agregar(anyLong(),any(),any(),any());
    }

    @Test void registraEventoSinBorrarHistorial() {
        when(repo.bloquearSolicitud(8)).thenReturn(true);
        when(repo.estado(8)).thenReturn(new SeguimientoRepository.Evento(1,"PENDIENTE","comercial",null,null));
        servicio.anular(8,"  Pedido duplicado  ","roberto");
        verify(repo).agregar(8,"ANULADA","Pedido duplicado","roberto");
    }
}
