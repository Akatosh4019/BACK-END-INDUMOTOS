package pe.indumotos.application.llegadas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class CorregirLlegadaTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RegistrarGuiaLlegada imagenes = mock(RegistrarGuiaLlegada.class);
    private final CorregirLlegada servicio = new CorregirLlegada(jdbc, imagenes);

    @Test
    void exigeMotivoAntesDeRetirar() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.retirar(8, "duplicado", "almacen"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test
    void noBorraFotosCuandoElReemplazoNoTieneImagenes() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.reemplazarFotos(8, List.of(), "almacen"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(jdbc, imagenes);
    }
}
