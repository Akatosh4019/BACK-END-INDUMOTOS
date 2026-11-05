package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Motocicleta;

class BuscarMotocicletasPorChasisTest {
    private final MotocicletaRepository repository = mock(MotocicletaRepository.class);
    private final BuscarMotocicletasPorChasis service = new BuscarMotocicletasPorChasis(repository);

    @Test void completoIdentificaLaUnidadExactaSinConfundirIdConChasis() {
        when(repository.buscarIdPorChasis("DEMO-A-0042")).thenReturn(Optional.of(8L));
        when(repository.buscarPorId(8)).thenReturn(new Motocicleta(8L, 1L, "Marca", "Modelo A", "Azul",
            "DEMO-A-0042", "Motor", "DUA", "01", "ficticio", null));
        var page=service.buscar(" DEMO-A-0042 ",null,0,20);
        assertEquals(1,page.totalElementos()); assertEquals(8,page.items().getFirst().id());
        assertEquals("DEMO-A-0042",page.items().getFirst().chasisCompleto());
        verify(repository,never()).buscarPorTerminacion(anyString(),anyInt(),anyInt());
    }
    @Test void completoAusenteDevuelveListaVacia() {
        when(repository.buscarIdPorChasis("NO-EXISTE")).thenReturn(Optional.empty());
        var page=service.buscar("NO-EXISTE",null,0,20);
        assertTrue(page.items().isEmpty());assertEquals(0,page.totalElementos());
        verify(repository,never()).buscarPorId(anyLong());
    }
    @Test void sufijoConservaCerosYNoEligeLaPrimeraCoincidencia() {
        var expected=new PaginaBusquedaChasis(List.of(
            new ResultadoBusquedaChasis(8,"Marca","Modelo A","Azul","DEMO-A-0042"),
            new ResultadoBusquedaChasis(9,"Marca","Modelo B","Rojo","DEMO-B-0042")),0,20,2,1);
        when(repository.buscarPorTerminacion("0042",0,20)).thenReturn(expected);
        assertEquals(expected,service.buscar(null,"0042",0,20));
        assertEquals(2,expected.items().size());
        verify(repository,never()).buscarIdPorChasis(anyString());
    }
    @Test void sufijoPermiteCuatroCaracteresSegunGuiaSinConvertirlosANumero() {
        when(repository.buscarPorTerminacion("0A42",0,20)).thenReturn(new PaginaBusquedaChasis(List.of(),0,20,0,0));
        service.buscar(null,"0A42",0,20);
        verify(repository).buscarPorTerminacion("0A42",0,20);
    }
    @Test void paginacionNoOcultaElTotalYNoRepiteElResultadoExacto() {
        when(repository.buscarIdPorChasis("DEMO-A-0042")).thenReturn(Optional.of(8L));
        var page=service.buscar("DEMO-A-0042",null,1,1);
        assertEquals(1,page.totalElementos());assertEquals(1,page.totalPaginas());assertTrue(page.items().isEmpty());
        verify(repository,never()).buscarPorId(anyLong());
    }
    @Test void noAceptaConsultaVaciaDosModosNiTerminacionesIncompletas() {
        bad(null,null,0,20);bad("ABC","0042",0,20);bad(" ",null,0,20);
        bad(null,"042",0,20);bad(null,"00042",0,20);bad("A".repeat(121),null,0,20);
        verifyNoInteractions(repository);
    }
    @Test void paginaYTamanosInvalidosSeRechazanAntesDeConsultar() {
        bad(null,"0042",-1,20);bad(null,"0042",0,0);bad(null,"0042",0,101);
        verifyNoInteractions(repository);
    }
    private void bad(String completo,String sufijo,int page,int size) {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.buscar(completo,sufijo,page,size)).getStatusCode().value());
    }
}
