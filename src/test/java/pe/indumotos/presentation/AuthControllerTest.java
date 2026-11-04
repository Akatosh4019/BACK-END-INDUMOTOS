package pe.indumotos.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pe.indumotos.security.JwtConfig;

class AuthControllerTest {
    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "HmacSHA256");

    @Test
    void loginEmiteTokenFirmadoYExpiraEnQuinceMinutos() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of()));
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key));
        var response = controller.login(new AuthController.LoginRequest("ADMIN", "clave_de_prueba"));
        var jwt = new JwtConfig().jwtDecoder(key).decode(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("admin", jwt.getSubject());
        assertEquals("https://indumotos.local", jwt.getIssuer().toString());
        assertEquals(900, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        verify(manager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void credencialesIncorrectasNoEmitenToken() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Incorrectas"));
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key));
        assertThrows(BadCredentialsException.class,
            () -> controller.login(new AuthController.LoginRequest("admin", "incorrecta")));
    }
}
