package pe.indumotos.application;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegistroGuiaVersionTest {
    private final RegistroGuia.Borrador draft=new RegistroGuia.Borrador(1,"Honda","Wave","Rojo","QA1234567890","MOTORQA1234","DUA","1");
    @Test void sameContentKeepsVersion() {
        assertEquals(new RegistroGuia.Estado(false,2,0,List.of(draft)).getVersion(),
            new RegistroGuia.Estado(false,2,0,List.of(draft)).getVersion());
    }
    @Test void concurrentDraftOrFinalizationChangesVersion() {
        var initial=new RegistroGuia.Estado(false,2,0,List.of()).getVersion();
        assertNotEquals(initial,new RegistroGuia.Estado(false,2,0,List.of(draft)).getVersion());
        assertNotEquals(initial,new RegistroGuia.Estado(true,2,2,List.of()).getVersion());
    }
}
