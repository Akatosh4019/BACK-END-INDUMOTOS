package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ConsultarMotocicletasFiltradasTest {
    @Test
    void normalizaLosFiltrosAntesDeConsultar() {
        var repository = mock(MotocicletaRepository.class);
        var service = new ConsultarMotocicletasFiltradas(repository);
        var filtro = new FiltroMotocicletas("RONCO", null, "ROJO", 0, 20);
        when(repository.buscarPorFiltros(filtro)).thenReturn(new PaginaMotocicletas(
            List.of(), 0, 20, 0, 0));

        service.consultar(" RONCO ", "  ", " ROJO ", 0, 20);

        verify(repository).buscarPorFiltros(filtro);
    }

    @Test
    void rechazaParametrosDePaginacionFueraDeRango() {
        var service = new ConsultarMotocicletasFiltradas(mock(MotocicletaRepository.class));

        var paginaInvalida = assertThrows(ResponseStatusException.class,
            () -> service.consultar(null, null, null, -1, 20));
        var tamanoInvalido = assertThrows(ResponseStatusException.class,
            () -> service.consultar(null, null, null, 0, 101));

        assertEquals(400, paginaInvalida.getStatusCode().value());
        assertEquals(400, tamanoInvalido.getStatusCode().value());
    }
}
