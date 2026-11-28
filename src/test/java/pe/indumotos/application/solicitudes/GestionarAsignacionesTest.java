package pe.indumotos.application.solicitudes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GestionarAsignacionesTest {
    private final AsignacionRepository repo=mock(AsignacionRepository.class);
    private final GestionarAsignaciones servicio=new GestionarAsignaciones(repo);
    private AsignacionRepository.Linea linea(String estado,int asignadas) {
        return new AsignacionRepository.Linea(1,2,"M","X","Rojo",2,estado,asignadas);
    }

    @Test
    void noPermiteAsignarMasQueLaCantidadSolicitada() {
        when(repo.bloquearLinea(1,2)).thenReturn(linea("EN_PREPARACION",2));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
            () -> servicio.asociar(1,2,3,"ensamblaje")).getStatusCode());
        verify(repo,never()).asociar(anyLong(),anyLong(),anyString());
    }

    @Test
    void noPermiteAsignarEnEstadoTerminal() {
        when(repo.bloquearLinea(1,2)).thenReturn(linea("ATENDIDA",0));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
            () -> servicio.asociar(1,2,3,"ensamblaje")).getStatusCode());
    }

    @Test
    void compruebaDisponibilidadYModelo() {
        when(repo.bloquearLinea(1,2)).thenReturn(linea("EN_PREPARACION",0));
        when(repo.bloquearMotocicleta(3)).thenReturn(true);
        when(repo.candidata(3)).thenReturn(new AsignacionRepository.Candidata(3,"CHASIS","M","OTRO","Rojo"));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
            () -> servicio.asociar(1,2,3,"ensamblaje")).getStatusCode());
        verify(repo,never()).asociar(anyLong(),anyLong(),anyString());
    }

    @Test
    void permiteAsociarUnidadCompatible() {
        when(repo.bloquearLinea(1,2)).thenReturn(linea("EN_PREPARACION",1));
        when(repo.bloquearMotocicleta(3)).thenReturn(true);
        when(repo.candidata(3)).thenReturn(new AsignacionRepository.Candidata(3,"CHASIS","M","X","Rojo"));
        servicio.asociar(1,2,3,"ensamblaje");
        verify(repo).asociar(2,3,"ensamblaje");
    }

    @Test
    void retirarExigeMotivo() {
        when(repo.bloquearLinea(1,2)).thenReturn(linea("EN_PREPARACION",1));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
            () -> servicio.retirar(1,2,3,"  ","ensamblaje")).getStatusCode());
    }
}
