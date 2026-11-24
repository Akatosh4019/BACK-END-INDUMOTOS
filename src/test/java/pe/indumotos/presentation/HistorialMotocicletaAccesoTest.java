package pe.indumotos.presentation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
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

class HistorialMotocicletaAccesoTest {
 private AnnotationConfigWebApplicationContext context;private MockMvc mvc;private PermisoService permisos;private ConsultarHistorialMotocicleta consulta;
 @Configuration @EnableWebMvc @EnableWebSecurity @Import(SecurityConfig.class)
 static class Config {
  @Bean JdbcTemplate jdbc(){return mock(JdbcTemplate.class);}
  @Bean PermisoService permisos(){return mock(PermisoService.class);}
  @Bean JwtDecoder decoder(){return mock(JwtDecoder.class);}
  @Bean ConsultarHistorialMotocicleta consulta(){return mock(ConsultarHistorialMotocicleta.class);}
  @Bean HistorialMotocicletaController controller(ConsultarHistorialMotocicleta consulta){return new HistorialMotocicletaController(consulta);}
 }
 @BeforeEach void abrir(){
  context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
  permisos=context.getBean(PermisoService.class);consulta=context.getBean(ConsultarHistorialMotocicleta.class);
  when(context.getBean(JdbcTemplate.class).query(anyString(),org.mockito.ArgumentMatchers.<ResultSetExtractor<UserDetails>>any(),eq("inventarios-demo"))).thenReturn(User.withUsername("inventarios-demo").password("no-usada").authorities("ROLE_INVENTARIOS").build());
  // Las ramas oficiales más recientes comprueban también la versión de sesión.
  when(context.getBean(JdbcTemplate.class).queryForObject("SELECT sesion_version FROM usuario WHERE nombre_usuario=?",Long.class,"inventarios-demo")).thenReturn(0L);
  when(context.getBean(JwtDecoder.class).decode("token-demo")).thenReturn(Jwt.withTokenValue("token-demo").header("alg","HS256").subject("inventarios-demo").claim("cv",0L).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build());
  mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(FilterChainProxy.class)).build();
 }
 @AfterEach void cerrar(){if(context!=null)context.close();}
 @Test void exigeSesionYPermisoDeInventario()throws Exception{
  mvc.perform(get("/api/v1/motocicletas/7/historial-integral")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/motocicletas/7/historial-integral").header("Authorization","Bearer token-demo")).andExpect(status().isForbidden());
  verifyNoInteractions(consulta);
 }
 @Test void entregaHistorialYParametrosSinExigirPermisoDeEdicion()throws Exception{
  when(permisos.permitido("inventarios-demo","INVENTARIO_CONSULTAR")).thenReturn(true);
  var unidad=new HistorialMotocicletaRepository.Unidad(7,"DEMO0042","Marca","Modelo","Azul");
  when(consulta.consultar(7,null,null,0,20)).thenReturn(new HistorialMotocicletaRepository.Pagina(unidad,List.of(),0,20,0,0,List.of("fichas")));
  mvc.perform(get("/api/v1/motocicletas/7/historial-integral").header("Authorization","Bearer token-demo")).andExpect(status().isOk()).andExpect(jsonPath("$.motocicleta.chasisCompleto").value("DEMO0042")).andExpect(jsonPath("$.fuentesDisponibles[0]").value("fichas"));
  verify(consulta).consultar(7,null,null,0,20);
 }
 @Test void rechazaFormatoDeFechaYNoAceptaEscrituras()throws Exception{
  when(permisos.permitido("inventarios-demo","INVENTARIO_CONSULTAR")).thenReturn(true);
  mvc.perform(get("/api/v1/motocicletas/7/historial-integral?desde=ayer").header("Authorization","Bearer token-demo")).andExpect(status().isBadRequest());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/motocicletas/7/historial-integral").header("Authorization","Bearer token-demo")).andExpect(status().isForbidden());
  verifyNoInteractions(consulta);
 }
}
