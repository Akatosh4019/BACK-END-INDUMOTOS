package pe.indumotos.presentation.inventario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.indumotos.application.inventario.RegistrarMotocicleta;
import pe.indumotos.application.inventario.ConsultarMotocicletasFiltradas;
import pe.indumotos.application.inventario.PaginaMotocicletas;
import pe.indumotos.application.inventario.MotocicletaResumen;
import pe.indumotos.domain.Motocicleta;
import pe.indumotos.application.inventario.EditarMotocicleta;
import pe.indumotos.application.seguridad.PermisoService;

class MotocicletaControllerTest {
    @Test
    void guardaFichaConLlegadaYConservaIdentificadoresComoTexto() {
        var registrar = mock(RegistrarMotocicleta.class);
        var moto = new Motocicleta(8L, 2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "admin", OffsetDateTime.now());
        when(registrar.ejecutar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "admin")).thenReturn(moto);
        var controller = new MotocicletaController(registrar,
            mock(pe.indumotos.application.seguridad.PermisoService.class),
            mock(ConsultarMotocicletasFiltradas.class), mock(pe.indumotos.application.inventario.EditarMotocicleta.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class));
        var auth = UsernamePasswordAuthenticationToken.authenticated(
            "admin", null, java.util.List.of());

        var response = controller.crear(new MotocicletaController.NuevaFicha(
            2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042", "MOTOR-0001", "000123", "004"), auth);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/v1/motocicletas/8", response.getHeaders().getLocation().toString());
        assertEquals("000123", response.getBody().dua());
        assertEquals("004", response.getBody().itemDua());
        assertEquals(2L, response.getBody().llegadaId());
    }

    @Test
    void listaResumenesConFiltrosOpcionales() {
        var registrar = mock(RegistrarMotocicleta.class);
        var consulta = mock(ConsultarMotocicletasFiltradas.class);
        var esperado = new PaginaMotocicletas(java.util.List.of(
            new MotocicletaResumen(8L, "RONCO", "RZ 250", "ROJO", "**** 0042")),
            0, 20, 1, 1);
        when(consulta.consultar("RONCO", null, "ROJO", null, 0, 20)).thenReturn(esperado);
        var controller = new MotocicletaController(registrar,
            mock(pe.indumotos.application.seguridad.PermisoService.class), consulta,
            mock(pe.indumotos.application.inventario.EditarMotocicleta.class),
            mock(org.springframework.jdbc.core.JdbcTemplate.class));

        var respuesta = controller.listar("RONCO", null, "ROJO", null, 0, 20);

        assertEquals(1, respuesta.items().size());
        assertEquals("**** 0042", respuesta.items().getFirst().chasisVista());
        verify(consulta).consultar("RONCO", null, "ROJO", null, 0, 20);
    }
}
