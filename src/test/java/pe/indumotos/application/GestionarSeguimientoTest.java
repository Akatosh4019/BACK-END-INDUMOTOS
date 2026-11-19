package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GestionarSeguimientoTest {
    private final SeguimientoRepository repo=mock(SeguimientoRepository.class);
    private final GestionarSeguimiento servicio=new GestionarSeguimiento(repo);

    @Test
    void noActualizaSolicitudInexistente() {
        var error=assertThrows(ResponseStatusException.class,
            () -> servicio.avanzar(5,"EN_PREPARACION",null,"ensamblaje"));
        assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
        verify(repo,never()).agregar(anyLong(),any(),any(),any());
    }

    @Test
    void soloPermiteTransicionValida() {
        when(repo.bloquearSolicitud(5)).thenReturn(true);
        when(repo.estado(5)).thenReturn(new SeguimientoRepository.Evento(1,"PENDIENTE","comercial",null,null));
        var error=assertThrows(ResponseStatusException.class,
            () -> servicio.avanzar(5,"ATENDIDA",null,"ensamblaje"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        servicio.avanzar(5,"EN_PREPARACION",null,"ensamblaje");
        verify(repo).agregar(5,"EN_PREPARACION",null,"ensamblaje");
    }

    @Test
    void rechazoExigeMotivo() {
        when(repo.bloquearSolicitud(5)).thenReturn(true);
        when(repo.estado(5)).thenReturn(new SeguimientoRepository.Evento(1,"EN_PREPARACION","ensamblaje",null,null));
        var error=assertThrows(ResponseStatusException.class,
            () -> servicio.avanzar(5,"RECHAZADA"," ","ensamblaje"));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
        verify(repo,never()).agregar(anyLong(),any(),any(),any());
    }

    @Test
    void noPuedeAvanzarEstadoTerminal() {
        when(repo.bloquearSolicitud(5)).thenReturn(true);
        when(repo.estado(5)).thenReturn(new SeguimientoRepository.Evento(1,"ATENDIDA","ensamblaje",null,null));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
            () -> servicio.avanzar(5,"EN_PREPARACION",null,"ensamblaje")).getStatusCode());
    }
}
