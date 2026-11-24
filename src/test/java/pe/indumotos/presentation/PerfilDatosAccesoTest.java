package pe.indumotos.presentation;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import pe.indumotos.application.*;
import pe.indumotos.security.SecurityConfig;

class PerfilDatosAccesoTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private PerfilService perfiles;
    private static final String DATOS = """
        {"nombres":"Ana","apellidos":"Lima","correo":"ana@example.com","telefono":"987654321","dni":"01234567","usuarioId":999,"rolCodigo":"ADMIN"}
        """;
    @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
    static class Config {
        @Bean JdbcTemplate jdbc() { return mock(JdbcTemplate.class); }
        @Bean PermisoService permisos() { return mock(PermisoService.class); }
        @Bean JwtDecoder decoder() { return mock(JwtDecoder.class); }
        @Bean PerfilService perfiles() { return mock(PerfilService.class); }
        @Bean PerfilController controller(PerfilService perfiles) { return new PerfilController(perfiles); }
    }
    @BeforeEach void abrir() {
        context=new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        perfiles=context.getBean(PerfilService.class);
        when(context.getBean(JdbcTemplate.class).query(anyString(),org.mockito.ArgumentMatchers.<ResultSetExtractor<UserDetails>>any(),eq("ana")))
            .thenReturn(User.withUsername("ana").password("no-usada").authorities("ROLE_ALMACEN").build());
        when(context.getBean(JdbcTemplate.class).queryForObject("SELECT sesion_version FROM usuario WHERE nombre_usuario=?",Long.class,"ana")).thenReturn(0L);
        when(context.getBean(JwtDecoder.class).decode("demo")).thenReturn(Jwt.withTokenValue("demo").header("alg","HS256").subject("ana").claim("cv",0L).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build());
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(FilterChainProxy.class)).build();
    }
    @AfterEach void cerrar() { context.close(); }
    @Test void noPermiteEditarSinSesion() throws Exception {
        mvc.perform(put("/api/v1/auth/perfil/datos").contentType("application/json").content(DATOS)).andExpect(status().isUnauthorized());
        verifyNoInteractions(perfiles);
    }
    @Test void usaLaIdentidadDeLaSesionYNoElUsuarioORolDelCuerpo() throws Exception {
        mvc.perform(put("/api/v1/auth/perfil/datos").header("Authorization","Bearer demo").contentType("application/json").content(DATOS)).andExpect(status().isOk());
        verify(perfiles).actualizarDatos("ana",new DatosPersonales("Ana","Lima","ana@example.com","987654321","01234567"));
        verifyNoInteractions(context.getBean(PermisoService.class));
    }
    @Test void rechazaDatosInvalidosAntesDeGuardar() throws Exception {
        mvc.perform(put("/api/v1/auth/perfil/datos").header("Authorization","Bearer demo").contentType("application/json").content(DATOS.replace("987654321","123"))).andExpect(status().isBadRequest());
        verifyNoInteractions(perfiles);
    }
}
