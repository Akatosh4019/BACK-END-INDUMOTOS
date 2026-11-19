package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;

class RegistrarLlegadaTest {
    private final LlegadaRepository repository = mock(LlegadaRepository.class);
    private final RegistrarLlegada servicio = new RegistrarLlegada(repository);

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

    @Test
    void buscaLlegadasPorNumeroDeGuiaNormalizado() {
        servicio.buscarPorGuia(" GR-PRUEBA ");
        verify(repository).buscarPorGuia("GR-PRUEBA");
    }

    @Test
    void rechazaBusquedaSinGuia() {
        assertThrows(RegistrarLlegada.DatosInvalidos.class, () -> servicio.buscarPorGuia(" "));
        verifyNoInteractions(repository);
    }
}
