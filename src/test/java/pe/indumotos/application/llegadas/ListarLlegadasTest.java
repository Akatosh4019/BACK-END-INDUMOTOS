package pe.indumotos.application.llegadas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ListarLlegadasTest {
    private final LlegadaRepository repository = mock(LlegadaRepository.class);
    private final RegistrarLlegada servicio = new RegistrarLlegada(repository);

    @Test
    void muestraTodasLasGuiasEnPaginasDeDiez() {
        when(repository.contar("")).thenReturn(23L);
        when(repository.listar("", 10, 10)).thenReturn(List.of());
        var pagina = servicio.listar("", 1);
        assertEquals(10, pagina.tamano());
        assertEquals(23, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
        verify(repository).listar("", 10, 10);
    }

    @Test
    void rechazaPaginaNegativa() {
        var error = assertThrows(ResponseStatusException.class, () -> servicio.listar("GR", -1));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void separaPendientesYConfirmadasAntesDePaginar() {
        when(repository.contarPorEstado("", "PENDIENTE")).thenReturn(12L);
        when(repository.listarPorEstado("", "PENDIENTE", 10, 10)).thenReturn(List.of());
        var pagina = servicio.listar("", 1, "PENDIENTE");
        assertEquals(12, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
        verify(repository).listarPorEstado("", "PENDIENTE", 10, 10);
    }

    @Test
    void rechazaEstadoDesconocido() {
        assertThrows(ResponseStatusException.class, () -> servicio.listar("", 0, "CANCELADA"));
        verifyNoInteractions(repository);
    }

    @Test
    void combinaFechaCantidadYResponsableAntesDePaginar() {
        LocalDate fecha = LocalDate.of(2026, 11, 27);
        when(repository.contarFiltradas("TG01", "CONFIRMADA", fecha, 2, "admin")).thenReturn(1L);
        when(repository.listarFiltradas("TG01", "CONFIRMADA", fecha, 2, "admin", 10, 0))
            .thenReturn(List.of());
        var pagina = servicio.listar(" TG01 ", 0, "confirmada", fecha, 2, " admin ");
        assertEquals(1, pagina.totalElementos());
        verify(repository).listarFiltradas("TG01", "CONFIRMADA", fecha, 2, "admin", 10, 0);
    }
}
