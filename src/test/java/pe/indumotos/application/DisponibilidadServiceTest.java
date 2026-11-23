package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class DisponibilidadServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final DisponibilidadService servicio=new DisponibilidadService(jdbc);

    @Test
    void rechazaBusquedaSinChasisSuficiente(){
        var error=assertThrows(ResponseStatusException.class,()->servicio.buscar("123",0));
        assertEquals(HttpStatus.BAD_REQUEST,error.getStatusCode());
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void ventaConfirmadaNoAdmiteCambioManual(){
        when(jdbc.query(anyString(),(RowMapper)org.mockito.ArgumentMatchers.any(RowMapper.class),eq(8L)))
            .thenReturn(List.of(8L))
            .thenReturn(List.of(new DisponibilidadService.Unidad(8,"IND-0008","Ronco","RZ","Negro",
                "Tienda Puruchuco","VENDIDA","Pago y comprobante registrados")));
        var error=assertThrows(ResponseStatusException.class,
            ()->servicio.cambiar(8,"HABILITADA","Revisión","facturador"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }
}
