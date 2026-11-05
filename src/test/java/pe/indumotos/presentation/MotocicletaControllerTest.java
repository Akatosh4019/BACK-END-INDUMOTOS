package pe.indumotos.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.indumotos.application.RegistrarMotocicleta;
import pe.indumotos.domain.Motocicleta;

class MotocicletaControllerTest {
    @Test
    void guardaFichaConLlegadaYConservaIdentificadoresComoTexto() {
        var registrar = mock(RegistrarMotocicleta.class);
        var moto = new Motocicleta(8L, 2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "admin", OffsetDateTime.now());
        when(registrar.ejecutar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "admin")).thenReturn(moto);
        var controller = new MotocicletaController(registrar);
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
}
