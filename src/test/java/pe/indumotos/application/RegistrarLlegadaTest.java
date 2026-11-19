package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class RegistrarLlegadaTest {
    private final LlegadaRepository repository = mock(LlegadaRepository.class);
    private final RegistrarLlegada servicio = new RegistrarLlegada(repository);

    @Test
    void listadoLimitaResultadosYNormalizaGuia() {
        servicio.buscar("  GUIA-01  ",20);
        verify(repository).buscar("GUIA-01",20);
    }
    @Test
    void listadoRechazaLimitesInvalidos() {
        assertThrows(RegistrarLlegada.DatosInvalidos.class,()->servicio.buscar("",0));
        assertThrows(RegistrarLlegada.DatosInvalidos.class,()->servicio.buscar("",51));
        assertThrows(RegistrarLlegada.DatosInvalidos.class,()->servicio.buscar("x".repeat(121),20));
        verifyNoInteractions(repository);
    }

    @Test
    void rechazaDatosIncompletosSinPersistir() {
        var error = assertThrows(RegistrarLlegada.DatosInvalidos.class,
            () -> servicio.ejecutar("  ", "almacen", 0, -1, null));
        assertEquals(3, error.errores().size());
        assertTrue(error.errores().containsKey("numeroGuia"));
        assertTrue(error.errores().containsKey("cantidadDeclarada"));
        assertTrue(error.errores().containsKey("cantidadRecibida"));
        verifyNoInteractions(repository);
    }

    @Test
    void rechazaGuiaNulaYReceptorAusenteSinPersistir() {
        var error = assertThrows(RegistrarLlegada.DatosInvalidos.class,
            () -> servicio.ejecutar(null, " ", 2, 2, null));
        assertTrue(error.errores().containsKey("numeroGuia"));
        assertTrue(error.errores().containsKey("usuarioReceptor"));
        verifyNoInteractions(repository);
    }

    @Test
    void conservaCasoValidoYNormalizaObservacionOpcional() {
        servicio.ejecutar(" GR-PRUEBA-001 ", "almacen", 2, 1, " ");
        verify(repository).guardar("GR-PRUEBA-001", "almacen", 2, 1, null);
    }
}
