package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DuplicateKeyException;

class RegistrarMotocicletaTest {
    @Test
    void impideRegistrarOtraMotoCuandoLaGuiaYaTermino() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        when(repository.registroTerminado(2L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
            () -> new RegistrarMotocicleta(repository).ejecutar(2L,"RONCO","RZ 250","ROJO",
                "DEMO-A-0042","MOTOR-0001","000123","004","inventarios"));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(repository,never()).guardar(anyLong(),any(),any(),any(),any(),any(),any(),any(),any());
    }
    @Test
    void rechazaChasisExistenteAntesDeGuardar() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        when(repository.buscarIdPorChasis("DEMO-A-0042")).thenReturn(java.util.Optional.of(8L));
        var registrar = new RegistrarMotocicleta(repository);
        var error = assertThrows(RegistrarMotocicleta.ChasisDuplicado.class,
            () -> registrar.ejecutar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
                "MOTOR-0001", "000123", "004", "inventarios"));
        assertEquals(8L, error.fichaExistenteId());
        verify(repository, never()).guardar(anyLong(), any(), any(), any(), any(),
            any(), any(), any(), any());
    }

    @Test
    void traduceConflictoDeSolicitudesSimultaneas() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        when(repository.combinacionRegistrada("RONCO", "RZ 250", "ROJO")).thenReturn(true);
        when(repository.buscarIdPorChasis("DEMO-A-0042"))
            .thenReturn(java.util.Optional.empty(), java.util.Optional.of(8L));
        when(repository.guardar(anyLong(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new DuplicateKeyException("chasis duplicado"));
        var registrar = new RegistrarMotocicleta(repository);
        var error = assertThrows(RegistrarMotocicleta.ChasisDuplicado.class,
            () -> registrar.ejecutar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
                "MOTOR-0001", "000123", "004", "inventarios"));
        assertEquals(8L, error.fichaExistenteId());
    }
    @Test
    void rechazaMotorRepetidoConChasisDistinto() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        when(repository.combinacionRegistrada("RONCO", "RZ 250", "ROJO")).thenReturn(true);
        when(repository.buscarIdPorChasis("DEMO-A-0043")).thenReturn(java.util.Optional.empty());
        when(repository.guardar(anyLong(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new DuplicateKeyException("ux_motocicleta_numero_motor"));
        var registrar = new RegistrarMotocicleta(repository);
        var error = assertThrows(ResponseStatusException.class,
            () -> registrar.ejecutar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0043",
                "MOTOR-0001", "000123", "004", "inventarios"));
        assertEquals(409, error.getStatusCode().value());
        assertTrue(error.getReason().contains("número de motor"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"marca", "modelo", "color", "chasisCompleto",
        "numeroMotor", "dua", "itemDua"})
    void rechazaCadaCampoObligatorioSinConsultarNiGuardar(String campo) {
        var repository = mock(MotocicletaRepository.class);
        var registrar = new RegistrarMotocicleta(repository);
        java.util.Map<String, String> datos = new java.util.HashMap<>(java.util.Map.of(
            "marca", "RONCO", "modelo", "RZ 250", "color", "ROJO",
            "chasisCompleto", "DEMO-A-0042", "numeroMotor", "MOTOR-0001",
            "dua", "000123", "itemDua", "004"));
        datos.put(campo, "   ");

        var error = assertThrows(RegistrarMotocicleta.DatosInvalidos.class,
            () -> registrar.ejecutar(2L, datos.get("marca"), datos.get("modelo"),
                datos.get("color"), datos.get("chasisCompleto"), datos.get("numeroMotor"),
                datos.get("dua"), datos.get("itemDua"), "inventarios"));

        assertEquals(java.util.Set.of(campo), error.errores().keySet());
        verifyNoInteractions(repository);
    }

    @Test
    void aceptaFichaCompletaYConservaIdentificadoresComoTexto() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        when(repository.combinacionRegistrada("RONCO", "RZ 250", "ROJO")).thenReturn(true);
        var registrar = new RegistrarMotocicleta(repository);

        registrar.ejecutar(2L, " RONCO ", " RZ 250 ", " ROJO ", " DEMO-A-0042 ",
            " MOTOR-0001 ", " 000123 ", " 004 ", "inventarios");

        verify(repository).guardar(2L, "RONCO", "RZ 250", "ROJO", "DEMO-A-0042",
            "MOTOR-0001", "000123", "004", "inventarios");
    }
    @Test
    void rechazaModeloNoRegistradoEnCatalogo() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(2L)).thenReturn(true);
        var error = assertThrows(RegistrarMotocicleta.DatosInvalidos.class,
            () -> new RegistrarMotocicleta(repository).ejecutar(2L, "RONCO", "NUEVO", "ROJO",
                "DEMO-A-0042", "MOTOR-0001", "000123", "004", "inventarios"));
        assertTrue(error.errores().containsKey("modelo"));
        verify(repository, never()).guardar(anyLong(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    @Test
    void noGuardaFichaSiLlegadaNoExiste() {
        var repository = mock(MotocicletaRepository.class);
        when(repository.existeLlegada(999L)).thenReturn(false);
        var registrar = new RegistrarMotocicleta(repository);

        var ex = assertThrows(ResponseStatusException.class,
            () -> registrar.ejecutar(999L, "RONCO", "RZ 250", "ROJO",
                "DEMO-A-0042", "MOTOR-0001", "000123", "004", "admin"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(repository, never()).guardar(anyLong(), any(), any(), any(), any(),
            any(), any(), any(), any());
    }
}
