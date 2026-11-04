package pe.indumotos.presentation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authentication;
    private final JwtEncoder encoder;

    public AuthController(AuthenticationManager authentication, JwtEncoder encoder) {
        this.authentication = authentication;
        this.encoder = encoder;
    }

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String clave) {}
    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
            request.nombreUsuario().trim().toLowerCase(java.util.Locale.ROOT), request.clave()));
        Instant now = Instant.now();
        long seconds = 900;
        var claims = JwtClaimsSet.builder().issuer("https://indumotos.local").subject(result.getName())
            .issuedAt(now).expiresAt(now.plus(seconds, ChronoUnit.SECONDS)).build();
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
