package pe.indumotos.application;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class RestablecerClave {
    private final CuentaSeguridadRepository repo; private final PasswordEncoder encoder;
    private record Intentos(long desde,int cantidad) {}
    private final ConcurrentHashMap<String,Intentos> intentos=new ConcurrentHashMap<>();
    public RestablecerClave(CuentaSeguridadRepository repo,PasswordEncoder encoder){this.repo=repo;this.encoder=encoder;}
    @Transactional
    public void ejecutar(String actor,long destino,String claveAdmin,String nueva,boolean verificada) {
        var admin=repo.buscar(actor);
        if(admin==null||!"ADMIN".equals(admin.rol())||!"ACTIVO".equals(admin.estado()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo Administración puede recuperar cuentas");
        long now=System.currentTimeMillis();
        var attempts=intentos.compute(actor,(k,v)->v==null||now-v.desde()>600_000?new Intentos(now,1):new Intentos(v.desde(),v.cantidad()+1));
        if(attempts.cantidad()>5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espera diez minutos antes de otro intento");
        if(!encoder.matches(claveAdmin,admin.hash()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No se confirmó la identidad del administrador");
        if(!verificada||nueva==null||nueva.length()<12||nueva.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Verifica la identidad y usa entre12 y100 caracteres");
        if(admin.id()==destino)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Este procedimiento es para recuperar otra cuenta");
        var cuenta=repo.bloquear(destino);
        if(cuenta==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no encontrada");
        if(!"ACTIVO".equals(cuenta.estado()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta está inactiva");
        repo.cambiarClave(destino,encoder.encode(nueva));
        org.slf4j.LoggerFactory.getLogger(RestablecerClave.class).info("Contraseña restablecida: administradorId={}, cuentaId={}",admin.id(),destino);
    }
}
