package pe.indumotos.presentation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.OffsetDateTime;
import pe.indumotos.application.RegistrarGuiaLlegada;
import pe.indumotos.application.RegistrarLlegada;
import pe.indumotos.domain.Llegada;

class LlegadaControllerTest {
    @Test
    void rechazaRegistroJsonSinImagen() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(
            new LlegadaController(mock(RegistrarLlegada.class)),
            new GuiaLlegadaController(mock(RegistrarGuiaLlegada.class))).build();
        mvc.perform(post("/api/v1/llegadas")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"numeroGuia\":\"G-1\",\"cantidadDeclarada\":1,\"cantidadRecibida\":1}"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void aceptaFormularioConFotoEnLaRutaPrincipal() throws Exception {
        var guias = mock(RegistrarGuiaLlegada.class);
        var llegada = new Llegada(7L, "G-1", OffsetDateTime.now(), 2L, "almacen", 1, 1, null, "PENDIENTE");
        when(guias.registrar(org.mockito.ArgumentMatchers.eq("G-1"),
            org.mockito.ArgumentMatchers.eq("almacen"), org.mockito.ArgumentMatchers.eq(1),
            org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.anyList())).thenReturn(llegada);
        var mvc = MockMvcBuilders.standaloneSetup(
            new LlegadaController(mock(RegistrarLlegada.class)), new GuiaLlegadaController(guias)).build();
        var foto = new MockMultipartFile("imagenes", "guia.jpg", "image/jpeg", new byte[]{1, 2, 3});
        var usuario = UsernamePasswordAuthenticationToken.authenticated("almacen", null, java.util.List.of());
        mvc.perform(multipart("/api/v1/llegadas").file(foto).principal(usuario)
            .param("numeroGuia", "G-1").param("cantidadDeclarada", "1").param("cantidadRecibida", "1"))
            .andExpect(status().isCreated());
    }
}
