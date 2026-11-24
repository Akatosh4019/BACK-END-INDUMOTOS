package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class VentasServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final VentasService ventas=new VentasService(jdbc);

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void trabajadorNoPuedeConsultarOtraTienda() {
        when(jdbc.query(anyString(),(RowMapper)org.mockito.ArgumentMatchers.any(RowMapper.class),
            eq("vendedor"))).thenReturn(java.util.Collections.singletonList(new Object[]{"COMERCIAL",4L}));
        var error=assertThrows(ResponseStatusException.class,()->ventas.tienda("vendedor",7L));
        assertEquals(HttpStatus.FORBIDDEN,error.getStatusCode());
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void cuentaSinTiendaNoPuedeRegistrarVenta() {
        when(jdbc.query(anyString(),(RowMapper)org.mockito.ArgumentMatchers.any(RowMapper.class),
            eq("vendedor"))).thenReturn(java.util.Collections.singletonList(new Object[]{"COMERCIAL",null}));
        var error=assertThrows(ResponseStatusException.class,
            ()->ventas.registrar("vendedor",null,1L,"PAGO-1","F001-1",java.math.BigDecimal.ONE));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void motoFueraDeLaTiendaNoSeVende() {
        when(jdbc.query(anyString(),(RowMapper)any(RowMapper.class),eq("facturador")))
            .thenReturn(java.util.Collections.singletonList(new Object[]{"FACTURACION",4L}));
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),eq(4L))).thenReturn(true);
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("FOR UPDATE"),(RowMapper)any(RowMapper.class),eq(8L)))
            .thenReturn(List.of(8L));
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),eq(8L),eq(4L)))
            .thenReturn(false);
        var error=assertThrows(ResponseStatusException.class,()->ventas.registrar(
            "facturador",null,8L,"PAGO-8","F001-8",java.math.BigDecimal.TEN));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void motoConDiferenciaPendienteNoSeVende() {
        when(jdbc.query(anyString(),(RowMapper)any(RowMapper.class),eq("facturador")))
            .thenReturn(java.util.Collections.singletonList(new Object[]{"FACTURACION",4L}));
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),eq(4L))).thenReturn(true);
        when(jdbc.query(org.mockito.ArgumentMatchers.contains("FOR UPDATE"),(RowMapper)any(RowMapper.class),eq(8L)))
            .thenReturn(List.of(8L));
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.contains("diferencia_datos"),eq(Boolean.class),eq(8L)))
            .thenReturn(true);
        var error=assertThrows(ResponseStatusException.class,()->ventas.registrar(
            "facturador",null,8L,"PAGO-8","F001-8",java.math.BigDecimal.TEN));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        assertEquals("La moto tiene una diferencia de datos en revisión",error.getReason());
    }
}
