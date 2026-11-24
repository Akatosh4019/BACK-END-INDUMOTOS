package pe.indumotos.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
class RestablecerClaveTest {
    private final CuentaSeguridadRepository repo=mock(CuentaSeguridadRepository.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private RestablecerClave service(){when(repo.buscar("admin")).thenReturn(new CuentaSeguridadRepository.Cuenta(1,"admin","hash-admin","ADMIN","ACTIVO"));when(encoder.matches("actual","hash-admin")).thenReturn(true);return new RestablecerClave(repo,encoder);}
    @Test void requiereReautenticarAdministrador(){var s=service();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->s.ejecutar("admin",2,"incorrecta","Nueva-de-prueba-123",true));verify(repo,never()).cambiarClave(anyLong(),anyString());}
    @Test void noPermiteRolesOperativos(){when(repo.buscar("almacen")).thenReturn(new CuentaSeguridadRepository.Cuenta(2,"almacen","h","ALMACEN","ACTIVO"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new RestablecerClave(repo,encoder).ejecutar("almacen",3,"actual","Nueva-de-prueba-123",true));verify(repo,never()).cambiarClave(anyLong(),anyString());}
    @Test void validaIdentidadYActualizaSoloHash(){var s=service();when(repo.bloquear(2)).thenReturn(new CuentaSeguridadRepository.Cuenta(2,"usuario","vieja","ALMACEN","ACTIVO"));when(encoder.encode("Nueva-de-prueba-123")).thenReturn("hash-nuevo");s.ejecutar("admin",2,"actual","Nueva-de-prueba-123",true);verify(repo).cambiarClave(2,"hash-nuevo");}
    @Test void cambioDeHashInvalidaMarcaAnterior(){String stamp=pe.indumotos.security.CredentialStamp.of("hash-anterior");assertTrue(pe.indumotos.security.CredentialStamp.matches(stamp,"hash-anterior"));assertFalse(pe.indumotos.security.CredentialStamp.matches(stamp,"hash-nuevo"));assertFalse(pe.indumotos.security.CredentialStamp.matches(null,"hash-nuevo"));}
    @Test void limitaIntentosFallidos(){var s=service();for(int i=0;i<5;i++)assertThrows(org.springframework.web.server.ResponseStatusException.class,()->s.ejecutar("admin",2,"mala","Nueva-de-prueba-123",true));var e=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->s.ejecutar("admin",2,"actual","Nueva-de-prueba-123",true));assertEquals(429,e.getStatusCode().value());}
}
