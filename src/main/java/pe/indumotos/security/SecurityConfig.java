package pe.indumotos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SecurityConfig {
    private final JdbcTemplate jdbc;

    public SecurityConfig(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return username -> jdbc.query(
            "SELECT u.nombre_usuario, u.clave_hash, u.estado, r.codigo, r.estado " +
            "FROM usuario u JOIN rol r ON r.id_rol = u.id_rol WHERE u.nombre_usuario = ?",
            rs -> {
                if (!rs.next()) throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Cuenta no encontrada");
                return User.withUsername(rs.getString(1)).password(rs.getString(2))
                    .disabled(!"ACTIVO".equals(rs.getString(3)) || !"ACTIVO".equals(rs.getString(5)))
                    .authorities("ROLE_" + rs.getString(4)).build();
            }, username);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(this::jwtAuthentication)))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
    }

    private JwtAuthenticationToken jwtAuthentication(Jwt jwt) {
        // Releer el estado y rol actuales evita confiar en un rol antiguo incluido en el token.
        org.springframework.security.core.userdetails.UserDetails user;
        try { user = userDetailsService(jdbc).loadUserByUsername(jwt.getSubject()); }
        catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new org.springframework.security.oauth2.jwt.BadJwtException("Cuenta no disponible");
        }
        if (!user.isEnabled()) throw new org.springframework.security.oauth2.jwt.BadJwtException("Cuenta inactiva");
        return new JwtAuthenticationToken(jwt, user.getAuthorities(), user.getUsername());
    }
}
