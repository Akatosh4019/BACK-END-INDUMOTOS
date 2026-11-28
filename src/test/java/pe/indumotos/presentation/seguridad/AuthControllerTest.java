package pe.indumotos.presentation.seguridad;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.indumotos.security.JwtConfig;
import pe.indumotos.infrastructure.seguridad.JdbcTokenRevocationRepository;
import pe.indumotos.infrastructure.seguridad.SesionRenovableRepository;
import pe.indumotos.application.seguridad.PermisoService;

class AuthControllerTest {
    private final SecretKeySpec key = new SecretKeySpec(new byte[32], "HmacSHA256");
    private JdbcTemplate jdbc() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any())).thenReturn(0L);
        return jdbc;
    }
    private SesionRenovableRepository sesiones() {
        var sesiones = mock(SesionRenovableRepository.class);
        when(sesiones.crear(anyString(), anyLong(), any())).thenReturn("refresh-test");
        when(sesiones.familia(any())).thenReturn(UUID.randomUUID());
        return sesiones;
    }

    @Test
    void loginEmiteTokenFirmadoYExpiraEnQuinceMinutos() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of()));
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key),
            mock(pe.indumotos.application.seguridad.PermisoService.class),
            mock(JdbcTokenRevocationRepository.class), jdbc(), sesiones(), false);
        var response = controller.login(new AuthController.LoginRequest("ADMIN", "clave_de_prueba"), new MockHttpServletResponse());
        var jwt = new JwtConfig().jwtDecoder(key,
            mock(JdbcTokenRevocationRepository.class)).decode(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("admin", jwt.getSubject());
        assertEquals(0L, ((Number) jwt.getClaim("cv")).longValue());
        assertEquals("https://indumotos.local", jwt.getIssuer().toString());
        assertEquals(900, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        verify(manager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void credencialesIncorrectasNoEmitenToken() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Incorrectas"));
        var controller = new AuthController(manager, new JwtConfig().jwtEncoder(key),
            mock(pe.indumotos.application.seguridad.PermisoService.class),
            mock(JdbcTokenRevocationRepository.class), jdbc(), sesiones(), false);
        assertThrows(BadCredentialsException.class,
            () -> controller.login(new AuthController.LoginRequest("admin", "incorrecta"), new MockHttpServletResponse()));
    }

    @Test
    void logoutRevocaElMismoTokenYElDecoderLoRechaza() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenReturn(
            UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of()));
        var revocados = mock(JdbcTokenRevocationRepository.class);
        var sesiones = sesiones();
        var config = new JwtConfig();
        var controller = new AuthController(manager, config.jwtEncoder(key),
            mock(pe.indumotos.application.seguridad.PermisoService.class), revocados, jdbc(), sesiones, false);
        String token = controller.login(new AuthController.LoginRequest("admin", "clave"), new MockHttpServletResponse()).accessToken();
        var jwt = config.jwtDecoder(key, revocados).decode(token);
        assertEquals(204, controller.logout(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt), new MockHttpServletResponse())
            .getStatusCode().value());
        verify(revocados).revocar(eq(token), eq(jwt.getExpiresAt()));
        verify(sesiones).revocar(any(UUID.class), any(Instant.class));
        when(revocados.revocado(token)).thenReturn(true);
        assertThrows(org.springframework.security.oauth2.jwt.BadJwtException.class,
            () -> config.jwtDecoder(key, revocados).decode(token));
    }

    @Test
    void renovacionRotaCookieYConservaLaSesion() {
        var sesiones = sesiones();
        when(sesiones.renovar(eq("anterior"), any())).thenReturn(
            new SesionRenovableRepository.Renovacion("nuevo", "admin", 0L, Instant.now().plusSeconds(3600)));
        var controller = new AuthController(mock(AuthenticationManager.class), new JwtConfig().jwtEncoder(key),
            mock(PermisoService.class), mock(JdbcTokenRevocationRepository.class), jdbc(), sesiones, false);
        var response = new MockHttpServletResponse();
        var login = controller.refresh("anterior", "1", response);
        assertEquals(900, login.expiresIn());
        assertTrue(response.getHeader("Set-Cookie").contains("indumotos_refresh=nuevo"));
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
        assertEquals("admin", new JwtConfig().jwtDecoder(key,
            mock(JdbcTokenRevocationRepository.class)).decode(login.accessToken()).getSubject());
        verify(sesiones).renovar(eq("anterior"), any());
    }
}
