package pe.indumotos.presentation.sistema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HealthControllerTest {
    @Test
    void informaQueLaApiResponde() {
        assertEquals("ok", new HealthController().health().get("status"));
    }
}
