package pe.indumotos.application;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
class ConsultarHistorialMotocicletaTest {
 @Test void validaAntesDeConsultarDatos(){
  var repo=mock(HistorialMotocicletaRepository.class);var servicio=new ConsultarHistorialMotocicleta(repo);
  assertEquals(400,assertThrows(ResponseStatusException.class,()->servicio.consultar(0,null,null,0,20)).getStatusCode().value());
  assertThrows(ResponseStatusException.class,()->servicio.consultar(1,null,null,-1,20));
  assertThrows(ResponseStatusException.class,()->servicio.consultar(1,null,null,0,101));
  assertThrows(ResponseStatusException.class,()->servicio.consultar(1,null,null,Integer.MAX_VALUE,100));
  assertThrows(ResponseStatusException.class,()->servicio.consultar(1,LocalDate.of(2026,11,24),LocalDate.of(2026,11,23),0,20));
  verifyNoInteractions(repo);
 }
 @Test void convierteFechasADiasCompletosEnPeru(){
  var repo=mock(HistorialMotocicletaRepository.class);var u=new HistorialMotocicletaRepository.Unidad(1,"DEMO0042","Marca","Modelo","Azul");when(repo.buscarUnidad(1)).thenReturn(u);
  new ConsultarHistorialMotocicleta(repo).consultar(1,LocalDate.of(2026,11,23),LocalDate.of(2026,11,23),0,20);
  verify(repo).consultar(u,OffsetDateTime.parse("2026-11-23T00:00:00-05:00"),OffsetDateTime.parse("2026-11-24T00:00:00-05:00"),0,20);
 }
 @Test void devuelve404SiNoExiste(){var repo=mock(HistorialMotocicletaRepository.class);assertEquals(404,assertThrows(ResponseStatusException.class,()->new ConsultarHistorialMotocicleta(repo).consultar(99,null,null,0,20)).getStatusCode().value());}
}
