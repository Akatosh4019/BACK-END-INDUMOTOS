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
        assertEquals("RZ 250", sugerencias.get(0).modelo());
        assertEquals("VERDE", sugerencias.get(0).color());
        assertEquals("RONCO", sugerencias.get(0).marca());
        assertEquals(1, sugerencias.get(0).pagina());
    }

    @Test
    void reconoceCamposJuntosYSeparadosComoEnLaFotoDeGuia() {
        var extractor = new ExtraerBorradoresGuia(mock(RegistrarLlegada.class),
            mock(GuiaLlegadaRepository.class));
        String texto = "Modelo¡CITY 5.0 Color:GRIS, Marca RONCO ELECTRY\n" +
            "Chasis HE1NWR10XSO000451 Motor WS05391SAGSO306106 Año Modelo; 2025\n" +
            "DUA: 118-2025-10-278083-00_1T. 71\n" +
            "Modelo.FLEX 10.0 Color VERDE, Marca/RONCO ELECTRY\n" +
            "Chasis L2BB56013VMD23042 Motor 60V1000WWVZ123042\n" +
            "118-2026-10-308730-00_1T. 42\n";
        var sugerencias = extractor.analizar(texto, 1);
        assertEquals(2, sugerencias.size());
        assertEquals("CITY 5.0", sugerencias.get(0).modelo());
        assertEquals("GRIS,", sugerencias.get(0).color());
        assertEquals("RONCO ELECTRY", sugerencias.get(0).marca());
        assertEquals("118-2025-10-278083-00", sugerencias.get(0).dua());
        assertEquals("71", sugerencias.get(0).itemDua());
        assertEquals("FLEX 10.0", sugerencias.get(1).modelo());
        assertEquals("VERDE,", sugerencias.get(1).color());
    }
}
