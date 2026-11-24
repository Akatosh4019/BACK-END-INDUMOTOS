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
import pe.indumotos.infrastructure.JdbcTokenRevocationRepository;

class AuthControllerTest {
    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "HmacSHA256");

    private org.springframework.jdbc.core.JdbcTemplate jdbc(){
        var db=mock(org.springframework.jdbc.core.JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(String.class),anyString())).thenReturn("hash-de-prueba");return db;
    }

    @Test
    void loginEmiteTokenFirmadoYExpiraEnQuinceMinutos() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of()));
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key),
            mock(pe.indumotos.application.PermisoService.class),
            mock(JdbcTokenRevocationRepository.class), jdbc());
        var response = controller.login(new AuthController.LoginRequest("ADMIN", "clave_de_prueba"));
        var jwt = new JwtConfig().jwtDecoder(key,
            mock(JdbcTokenRevocationRepository.class)).decode(response.accessToken());
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
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key),
            mock(pe.indumotos.application.PermisoService.class),
            mock(JdbcTokenRevocationRepository.class), jdbc());
        assertThrows(BadCredentialsException.class,
            () -> controller.login(new AuthController.LoginRequest("admin", "incorrecta")));
    }

    @Test
    void logoutRevocaElMismoTokenYElDecoderLoRechaza() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of()));
        var revocados = mock(JdbcTokenRevocationRepository.class);
        var config = new JwtConfig();
        var controller = new AuthController(manager, config.jwtEncoder(key),
            mock(pe.indumotos.application.PermisoService.class), revocados, jdbc());
        String token = controller.login(new AuthController.LoginRequest("admin", "clave")).accessToken();
        var jwt = config.jwtDecoder(key, revocados).decode(token);
        assertEquals(204, controller.logout(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt))
            .getStatusCode().value());
        verify(revocados).revocar(eq(token), eq(jwt.getExpiresAt()));
        when(revocados.revocado(token)).thenReturn(true);
        assertThrows(org.springframework.security.oauth2.jwt.BadJwtException.class,
            () -> config.jwtDecoder(key, revocados).decode(token));
    }
}
