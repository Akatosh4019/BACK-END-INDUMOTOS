package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

class DisponibilidadServiceTest {
    private static final OffsetDateTime T1 = OffsetDateTime.parse("2026-11-20T09:00:00-05:00");
    private static final OffsetDateTime T2 = OffsetDateTime.parse("2026-11-21T10:00:00-05:00");
    private static final OffsetDateTime T3 = OffsetDateTime.parse("2026-11-22T11:00:00-05:00");

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
            .thenReturn(List.of(hechos().venta(T3,"facturador").build()));
        var error=assertThrows(ResponseStatusException.class,
            ()->servicio.cambiar(8,"HABILITADA","Revisión","facturador"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void noSeHabilitaUnaMotoConDiferenciaPendiente(){
        when(jdbc.query(anyString(),(RowMapper)org.mockito.ArgumentMatchers.any(RowMapper.class),eq(8L)))
            .thenReturn(List.of(8L))
            .thenReturn(List.of(hechos().diferencia("color",T2,"almacen").build()));
        var error=assertThrows(ResponseStatusException.class,
            ()->servicio.cambiar(8,"HABILITADA","Revisada","admin"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
    }

    @Test
    void disponibleIndicaFechaQuienRegistroYQueSePuedeOfrecer(){
        var unidad=DisponibilidadService.derivar(hechos().build());
        assertEquals("DISPONIBLE",unidad.estado());
        assertEquals("Sin bloqueo registrado",unidad.motivo());
        assertEquals(T1,unidad.actualizadoEn());
        assertEquals("almacen",unidad.registradoPor());
        assertTrue(unidad.puedeOfrecerse());
    }

    @Test
    void habilitacionManualMuestraSuMotivoYResponsable(){
        var unidad=DisponibilidadService.derivar(hechos().bloqueo("HABILITADA","Revisión conforme",T2,"comercial").build());
        assertEquals("DISPONIBLE",unidad.estado());
        assertEquals("Revisión conforme",unidad.motivo());
        assertEquals("comercial",unidad.registradoPor());
        assertTrue(unidad.puedeOfrecerse());
    }

    @Test
    void diferenciaPendienteDejaLaMotoEnRevisionYNoSeOfrece(){
        var unidad=DisponibilidadService.derivar(hechos().diferencia("numeroMotor",T2,"inventarios")
            .asignada(T3,"logistica").build());
        assertEquals("EN_REVISION",unidad.estado());
        assertEquals("Diferencia de datos en revisión: número de motor",unidad.motivo());
        assertEquals(T2,unidad.actualizadoEn());
        assertEquals("inventarios",unidad.registradoPor());
        assertFalse(unidad.puedeOfrecerse());
    }

    @Test
    void prioridadVentaTransitoBloqueoRevisionAsignacionSinUbicacion(){
        var todo=hechos().venta(T3,"facturador").envio(T2,"almacen").bloqueo("BLOQUEADA","Golpe",T2,"admin")
            .diferencia("color",T2,"inventarios").asignada(T1,"logistica");
        assertEquals("VENDIDA",DisponibilidadService.derivar(todo.build()).estado());
        assertEquals("facturador",DisponibilidadService.derivar(todo.build()).registradoPor());
        assertEquals("EN_TRANSITO",DisponibilidadService.derivar(todo.venta(null,null).build()).estado());
        var bloqueada=DisponibilidadService.derivar(todo.envio(null,null).build());
        assertEquals("BLOQUEADA",bloqueada.estado());
        assertEquals("Golpe",bloqueada.motivo());
        assertEquals("admin",bloqueada.registradoPor());
        assertEquals("EN_REVISION",DisponibilidadService.derivar(todo.bloqueo(null,null,null,null).build()).estado());
        assertEquals("ASIGNADA",DisponibilidadService.derivar(todo.diferencia(null,null,null).build()).estado());
        var sinUbicacion=DisponibilidadService.derivar(hechos().sinUbicacion().build());
        assertEquals("SIN_UBICACION",sinUbicacion.estado());
        for(var u:List.of(DisponibilidadService.derivar(todo.build()),bloqueada,sinUbicacion))
            assertFalse(u.puedeOfrecerse());
    }

    private static Constructor hechos(){ return new Constructor(); }

    /** Arma los hechos de una moto ubicada en tienda y permite añadir o quitar cada hecho. */
    private static final class Constructor {
        String ubicacion="Tienda Puruchuco"; OffsetDateTime ubicacionEn=T1; String ubicacionPor="almacen";
        OffsetDateTime ventaEn; String ventaPor; OffsetDateTime envioEn; String envioPor;
        String bloqueoEstado,bloqueoMotivo; OffsetDateTime bloqueoEn; String bloqueoPor;
        String difCampo; OffsetDateTime difEn; String difPor; OffsetDateTime asignadaEn; String asignadaPor;

        Constructor sinUbicacion(){ ubicacion=null; ubicacionEn=null; ubicacionPor=null; return this; }
        Constructor venta(OffsetDateTime en,String por){ ventaEn=en; ventaPor=por; return this; }
        Constructor envio(OffsetDateTime en,String por){ envioEn=en; envioPor=por; return this; }
        Constructor bloqueo(String estado,String motivo,OffsetDateTime en,String por){
            bloqueoEstado=estado; bloqueoMotivo=motivo; bloqueoEn=en; bloqueoPor=por; return this; }
        Constructor diferencia(String campo,OffsetDateTime en,String por){ difCampo=campo; difEn=en; difPor=por; return this; }
        Constructor asignada(OffsetDateTime en,String por){ asignadaEn=en; asignadaPor=por; return this; }

        DisponibilidadService.Hechos build(){
            return new DisponibilidadService.Hechos(8,"IND-0008","Ronco","RZ","Negro",ubicacion,ubicacionEn,
                ubicacionPor,ventaEn,ventaPor,envioEn,envioPor,bloqueoEstado,bloqueoMotivo,bloqueoEn,bloqueoPor,
                difCampo,difEn,difPor,asignadaEn,asignadaPor);
        }
    }
}
