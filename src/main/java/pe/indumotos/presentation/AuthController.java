package pe.indumotos.presentation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import pe.indumotos.infrastructure.JdbcTokenRevocationRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authentication;
    private final JwtEncoder encoder;
    private final pe.indumotos.application.PermisoService permisos;
    private final JdbcTokenRevocationRepository revocados;
    private final JdbcTemplate jdbc;
    // Duración de la sesión: una hora por defecto, configurable con INDUMOTOS_SESION_SEGUNDOS.
    private long sesionSegundos = 3600;

    public AuthController(AuthenticationManager authentication, JwtEncoder encoder,
        pe.indumotos.application.PermisoService permisos,
        JdbcTokenRevocationRepository revocados, JdbcTemplate jdbc) {
        this.authentication = authentication;
        this.encoder = encoder;
        this.permisos = permisos;
        this.revocados = revocados;
        this.jdbc = jdbc;
    }

    @org.springframework.beans.factory.annotation.Value("${indumotos.sesion.segundos:3600}")
    void setSesionSegundos(long segundos) {
        if (segundos < 300 || segundos > 43200)
            throw new IllegalStateException("La sesión debe durar entre 5 minutos y 12 horas");
        this.sesionSegundos = segundos;
    }

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String clave) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}

    @GetMapping("/me")
    public pe.indumotos.application.PermisoService.Accesos me(
        org.springframework.security.core.Authentication authentication) {
        return permisos.consultarPropios(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(JwtAuthenticationToken authentication) {
        revocados.revocar(authentication.getToken().getTokenValue(),
            authentication.getToken().getExpiresAt());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
            request.nombreUsuario().trim().toLowerCase(java.util.Locale.ROOT), request.clave()));
        Instant now = Instant.now();
        long seconds = sesionSegundos;
        Long version = jdbc.queryForObject("SELECT sesion_version FROM usuario WHERE nombre_usuario=?",
            Long.class, result.getName());
        if (version == null) throw new org.springframework.security.authentication.BadCredentialsException("Cuenta no disponible");
        var claims = JwtClaimsSet.builder().issuer("https://indumotos.local").subject(result.getName())
            .claim("credential",pe.indumotos.security.CredentialStamp.of(jdbc.queryForObject("SELECT clave_hash FROM usuario WHERE nombre_usuario=?",String.class,result.getName())))
            .issuedAt(now).expiresAt(now.plus(seconds, ChronoUnit.SECONDS)).claim("cv", version).build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", seconds);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> credencialesInvalidas() {
        return org.springframework.http.ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(java.util.Map.of("error", "Credenciales inválidas o cuenta inactiva"));
    }
}
