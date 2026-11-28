package pe.indumotos.presentation.seguridad;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.ResponseEntity;
import pe.indumotos.infrastructure.seguridad.JdbcTokenRevocationRepository;
import pe.indumotos.infrastructure.seguridad.SesionRenovableRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import pe.indumotos.application.seguridad.PermisoService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authentication;
    private final JwtEncoder encoder;
    private final pe.indumotos.application.seguridad.PermisoService permisos;
    private final JdbcTokenRevocationRepository revocados;
    private final JdbcTemplate jdbc;
    private final SesionRenovableRepository sesiones;
    private final boolean cookieSegura;

    public AuthController(AuthenticationManager authentication, JwtEncoder encoder,
        pe.indumotos.application.seguridad.PermisoService permisos,
        JdbcTokenRevocationRepository revocados, JdbcTemplate jdbc,
        SesionRenovableRepository sesiones, @Value("${AUTH_COOKIE_SECURE:true}") boolean cookieSegura) {
        this.authentication = authentication;
        this.encoder = encoder;
        this.permisos = permisos;
        this.revocados = revocados;
        this.jdbc = jdbc;
        this.sesiones = sesiones;
        this.cookieSegura = cookieSegura;
    }

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String clave) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}

    @GetMapping("/me")
    public pe.indumotos.application.seguridad.PermisoService.Accesos me(
        org.springframework.security.core.Authentication authentication) {
        return permisos.consultarPropios(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication, HttpServletResponse response) {
        revocados.revocar(authentication.getToken().getTokenValue(),
            authentication.getToken().getExpiresAt());
        var sid = authentication.getToken().getClaimAsString("sid");
        if (sid != null) sesiones.revocar(UUID.fromString(sid), Instant.now());
        cookie(response, "", 0);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
            request.nombreUsuario().trim().toLowerCase(java.util.Locale.ROOT), request.clave()));
        Instant now = Instant.now();
        long seconds = 900;
        Long version = jdbc.queryForObject("SELECT sesion_version FROM usuario WHERE nombre_usuario=?",
            Long.class, result.getName());
        if (version == null) throw new org.springframework.security.authentication.BadCredentialsException("Cuenta no disponible");
        var refresh = sesiones.crear(result.getName(), version, now);
        cookie(response, refresh, 8 * 60 * 60);
        return emitir(result.getName(), version, now, refresh, 900);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@CookieValue(name = "indumotos_refresh", required = false) String refresh,
        @RequestHeader(name = "X-Indumotos-Refresh", required = false) String comprobacion,
        HttpServletResponse response) {
        if (!"1".equals(comprobacion)) throw new org.springframework.security.authentication.BadCredentialsException("Renovación inválida");
        var sesion = sesiones.renovar(refresh, Instant.now());
        if (sesion == null) {
            cookie(response, "", 0);
            throw new org.springframework.security.authentication.BadCredentialsException("Sesión vencida");
        }
        cookie(response, sesion.token(), Math.max(0, sesion.limite().getEpochSecond() - Instant.now().getEpochSecond()));
        var ahora = Instant.now();
        long segundos = Math.min(900, Math.max(1, sesion.limite().getEpochSecond() - ahora.getEpochSecond()));
        return emitir(sesion.usuario(), sesion.version(), ahora, sesion.token(), segundos);
    }

    private LoginResponse emitir(String usuario, long version, Instant now, String refresh, long seconds) {
        // El identificador de familia permite cerrar todas las renovaciones de esta sesión.
        UUID familia = sesiones.familia(refresh);
        var claims = JwtClaimsSet.builder().issuer("https://indumotos.local").subject(usuario)
            .issuedAt(now).expiresAt(now.plus(seconds, ChronoUnit.SECONDS))
            .id(UUID.randomUUID().toString()).claim("cv", version).claim("sid", familia.toString()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", seconds);
    }

    private void cookie(HttpServletResponse response, String valor, long segundos) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("indumotos_refresh", valor)
            .httpOnly(true).secure(cookieSegura).sameSite("Strict").path("/api/v1/auth")
            .maxAge(segundos).build().toString());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> credencialesInvalidas() {
        return org.springframework.http.ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(java.util.Map.of("error", "Credenciales inválidas o cuenta inactiva"));
    }
}
