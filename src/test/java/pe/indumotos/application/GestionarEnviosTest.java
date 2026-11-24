package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class GestionarEnviosTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final UbicacionRepository ubicaciones = mock(UbicacionRepository.class);
    private final GestionarEnvios servicio = new GestionarEnvios(jdbc,ubicaciones);

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void soloElSolicitanteOAdminPuedeConfirmarRecepcion() {
        when(jdbc.query(anyString(), (RowMapper) org.mockito.ArgumentMatchers.any(RowMapper.class),
            eq(12L))).thenReturn(List.of(3L));
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),eq("otro"),eq(12L)))
            .thenReturn(false);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.recibir(12L,9L,"otro"));
        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
        verifyNoInteractions(ubicaciones);
    }
}
