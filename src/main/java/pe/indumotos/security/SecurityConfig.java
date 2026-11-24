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
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import pe.indumotos.application.PermisoService;

@Configuration
public class SecurityConfig {
    private final JdbcTemplate jdbc;
    private final PermisoService permisos;

    public SecurityConfig(JdbcTemplate jdbc, PermisoService permisos) {
        this.jdbc = jdbc;
        this.permisos = permisos;
    }
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
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").access(exige("USUARIOS_CREAR"))
                .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/*/tienda")
                    .access(exige("COMPANIA_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/modos/compania")
                    .access(exige("COMPANIA_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/modos/tienda/ventas")
                    .access(exigeTodos("TIENDA_CONSULTAR","VENTAS_REGISTRAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidad", "/api/v1/motocicletas/*/disponibilidad",
                    "/api/v1/motocicletas/*/disponibilidad/historial")
                    .access(exige("DISPONIBILIDAD_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/motocicletas/*/disponibilidad")
                    .access(exige("DISPONIBILIDAD_CAMBIAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/diferencias")
                    .access(exige("DIFERENCIAS_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/diferencias")
                    .access(exige("DIFERENCIAS_REPORTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/diferencias/*/resolucion")
                    .access(exigeTodos("DIFERENCIAS_RESOLVER","FICHAS_EDITAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/modos/tienda", "/api/v1/modos/tienda/**")
                    .access(exige("TIENDA_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios")
                    .access(exigeAlguno("USUARIOS_CREAR", "PERMISOS_ASIGNAR", "USUARIOS_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/*", "/api/v1/usuarios/*/estado/historial")
                    .access(exige("USUARIOS_CONSULTAR"))
                .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/*/estado").access(exige("USUARIOS_DESACTIVAR"))
                .requestMatchers("/api/v1/usuarios/*/rol", "/api/v1/usuarios/*/permisos",
                    "/api/v1/usuarios/*/permisos/*")
                    .access(exige("PERMISOS_ASIGNAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/llegadas").access(exige("LLEGADAS_REGISTRAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/llegadas/con-guia").access(exige("LLEGADAS_REGISTRAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/llegadas").access(exige("INVENTARIO_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/llegadas/*/sugerencias-ocr")
                    .access(exigeTodos("FICHAS_REGISTRAR", "INVENTARIO_CONSULTAR"))
                .requestMatchers(HttpMethod.PUT, "/api/v1/llegadas/*/registro/borradores/*")
                    .access(exigeTodos("FICHAS_REGISTRAR", "INVENTARIO_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/llegadas/*/registro/finalizar")
                    .access(exigeTodos("FICHAS_REGISTRAR", "INVENTARIO_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/llegadas/**").access(exige("INVENTARIO_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/motocicletas").access(exige("FICHAS_REGISTRAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo-motocicletas")
                    .access(exigeAlguno("CATALOGO_GESTIONAR", "FICHAS_REGISTRAR", "FICHAS_EDITAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/catalogo-motocicletas")
                    .access(exige("CATALOGO_GESTIONAR"))
                .requestMatchers(HttpMethod.PUT, "/api/v1/motocicletas/*").access(exige("FICHAS_EDITAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/ubicaciones").access(exige("UBICACIONES_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/ubicaciones").access(exige("PERMISOS_ASIGNAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes").access(exige("SOLICITUDES_CREAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/solicitudes/opciones-motocicleta")
                    .access(exige("SOLICITUDES_CREAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/anulacion")
                    .access(exige("SOLICITUDES_ANULAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/seguimiento")
                    .access(exige("SOLICITUDES_AVANZAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/envios/despacho")
                    .access(exige("TRASLADOS_REGISTRAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/envios/recepcion")
                    .access(exige("RECEPCIONES_CONFIRMAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/solicitudes/*/envios")
                    .access(exige("SOLICITUDES_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/motocicletas/elegibles")
                    .access(exige("UNIDADES_ASIGNAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes/*/lineas/*/unidades")
                    .access(exige("UNIDADES_ASIGNAR"))
                .requestMatchers(HttpMethod.DELETE, "/api/v1/solicitudes/*/lineas/*/unidades/*")
                    .access(exige("UNIDADES_ASIGNAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/solicitudes/**")
                    .access(exige("SOLICITUDES_CONSULTAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/motocicletas/*/ubicacion")
                    .access(exige("UBICACIONES_REGISTRAR"))
                .requestMatchers(HttpMethod.POST, "/api/v1/motocicletas/*/traslados")
                    .access(exige("TRASLADOS_REGISTRAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/motocicletas/*/ubicacion",
                    "/api/v1/motocicletas/*/ubicacion/historial",
                    "/api/v1/motocicletas/*/envio-activo")
                    .access(exige("UBICACIONES_CONSULTAR"))
                .requestMatchers(HttpMethod.GET, "/api/v1/motocicletas/**").access(exige("INVENTARIO_CONSULTAR"))
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(this::jwtAuthentication)))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
    }

    private AuthorizationManager<RequestAuthorizationContext> exige(String codigo) {
        return (authentication, context) -> {
            var actual = authentication.get();
            return new AuthorizationDecision(actual != null && actual.isAuthenticated()
                && permisos.permitido(actual.getName(), codigo));
        };
    }

    private AuthorizationManager<RequestAuthorizationContext> exigeAlguno(String... codigos) {
        return (authentication, context) -> {
            var actual = authentication.get();
            return new AuthorizationDecision(actual != null && actual.isAuthenticated()
                && java.util.Arrays.stream(codigos).anyMatch(codigo -> permisos.permitido(actual.getName(), codigo)));
        };
    }

    private AuthorizationManager<RequestAuthorizationContext> exigeTodos(String... codigos) {
        return (authentication, context) -> {
            var actual = authentication.get();
            return new AuthorizationDecision(actual != null && actual.isAuthenticated()
                && java.util.Arrays.stream(codigos).allMatch(codigo -> permisos.permitido(actual.getName(), codigo)));
        };
    }

    private JwtAuthenticationToken jwtAuthentication(Jwt jwt) {
        // Releer el estado y rol actuales evita confiar en un rol antiguo incluido en el token.
        org.springframework.security.core.userdetails.UserDetails user;
        try { user = userDetailsService(jdbc).loadUserByUsername(jwt.getSubject()); }
        catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new org.springframework.security.oauth2.server.resource.InvalidBearerTokenException("Cuenta no disponible");
        }
        if (!user.isEnabled()) throw new org.springframework.security.oauth2.server.resource.InvalidBearerTokenException("Cuenta inactiva");
        // HU-16: un cambio de estado cierra las sesiones abiertas antes de ese momento.
        java.time.OffsetDateTime validasDesde = jdbc.query(
            "SELECT sesiones_validas_desde FROM usuario WHERE nombre_usuario = ?",
            rs -> rs.next() ? rs.getObject(1, java.time.OffsetDateTime.class) : null, jwt.getSubject());
        // JWT codifica iat en segundos: los tokens del mismo segundo del cambio también son anteriores.
        if (validasDesde != null && (jwt.getIssuedAt() == null || !jwt.getIssuedAt().isAfter(validasDesde.toInstant())))
            throw new org.springframework.security.oauth2.server.resource.InvalidBearerTokenException("Sesión cerrada por cambio de estado");
        return new JwtAuthenticationToken(jwt, user.getAuthorities(), user.getUsername());
    }
}
