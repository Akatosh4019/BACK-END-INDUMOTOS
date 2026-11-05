package pe.indumotos.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.indumotos.application.RegistrarLlegada;
import pe.indumotos.domain.Llegada;

class LlegadaControllerTest {
    @Test
    void creaLlegadaConUsuarioAutenticadoYDosCantidades() {
        var registrar = mock(RegistrarLlegada.class);
        var llegada = new Llegada(7L, "GR-PRUEBA-001", OffsetDateTime.now(), 3L,
            "almacen_prueba", 2, 1, null);
        when(registrar.ejecutar("GR-PRUEBA-001", "almacen_prueba", 2, 1, null))
            .thenReturn(llegada);
        var controller = new LlegadaController(registrar);
        var auth = UsernamePasswordAuthenticationToken.authenticated(
            "almacen_prueba", null, java.util.List.of());

        var response = controller.crear(
            new LlegadaController.NuevaLlegada("GR-PRUEBA-001", BigDecimal.valueOf(2),
                BigDecimal.ONE, null), auth);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/v1/llegadas/7", response.getHeaders().getLocation().toString());
        assertEquals(2, response.getBody().cantidadDeclarada());
        assertEquals(1, response.getBody().cantidadRecibida());
        verify(registrar).ejecutar("GR-PRUEBA-001", "almacen_prueba", 2, 1, null);
    }

    @Test
    void rechazaCamposBasicosInvalidos() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var errores = validator.validate(new LlegadaController.NuevaLlegada(" ", BigDecimal.ZERO, null, null));
        assertEquals(3, errores.size());
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("numeroGuia")));
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("cantidadDeclarada")));
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("cantidadRecibida")));
    }

    @Test
    void rechazaCantidadesFraccionarias() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var errores = validator.validate(new LlegadaController.NuevaLlegada("GR-PRUEBA-001",
            new BigDecimal("1.5"), BigDecimal.ONE, null));
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("cantidadDeclarada")));
    }
}
