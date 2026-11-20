package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;

class ExtraerBorradoresGuiaTest {
    @Test
    void extraeSoloBorradoresSinRegistrarMotocicletas() {
        var extractor = new ExtraerBorradoresGuia(mock(RegistrarLlegada.class),
            mock(GuiaLlegadaRepository.class));
        String texto = "Modelo RZ 250 Color VERDE Marca RONCO\n" +
            "Chasis L2BB3322XVMD12128 Motor 167YMMVZ212128 Año Modelo 2027\n" +
            "118-2026-10-269348-00_IT. 128\n" +
            "Modelo RZ 250 Color NEGRO Marca RONCO\n" +
            "Chasis L2BB33221VMD12132 Motor 167YMMVZ212132\n";
        var sugerencias = extractor.analizar(texto, 1);
        assertEquals(2, sugerencias.size());
        assertEquals("L2BB3322XVMD12128", sugerencias.get(0).chasisCompleto());
        assertEquals("167YMMVZ212128", sugerencias.get(0).numeroMotor());
        assertEquals(1, sugerencias.get(0).pagina());
    }
}
