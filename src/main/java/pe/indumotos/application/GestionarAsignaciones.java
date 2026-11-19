package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarAsignaciones {
    private final AsignacionRepository repo;
    public GestionarAsignaciones(AsignacionRepository repo) { this.repo=repo; }

    public List<AsignacionRepository.Candidata> elegibles(String chasis) {
        if (chasis == null || chasis.isBlank() || chasis.length() > 120)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica un chasis para buscar");
        return repo.buscarElegibles(chasis.trim(),20);
    }

    @Transactional
    public AsignacionRepository.Unidad asociar(long solicitudId,long detalleId,long motocicletaId,String usuario) {
        var linea=repo.bloquearLinea(solicitudId,detalleId);
        if (linea == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Línea de solicitud no encontrada");
        if (!"EN_PREPARACION".equals(linea.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud no está en preparación");
        if (linea.asignadas() >= linea.cantidad())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cupo de la línea completo");
        if (!repo.bloquearMotocicleta(motocicletaId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Motocicleta no encontrada");
        var moto=repo.candidata(motocicletaId);
        if (moto == null || !moto.marca().equalsIgnoreCase(linea.marca()) ||
            !moto.modelo().equalsIgnoreCase(linea.modelo()) || !moto.color().equalsIgnoreCase(linea.color()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La unidad no está disponible o no coincide con la línea");
        return repo.asociar(detalleId,motocicletaId,usuario);
    }

    public List<AsignacionRepository.Unidad> listar(long solicitudId,long detalleId) {
        if (repo.bloquearLinea(solicitudId,detalleId)==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Línea de solicitud no encontrada");
        return repo.asignadas(detalleId);
    }

    @Transactional
    public void retirar(long solicitudId,long detalleId,long asociacionId,String motivo,String usuario) {
        var linea=repo.bloquearLinea(solicitudId,detalleId);
        if (linea == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Línea de solicitud no encontrada");
        if (!"EN_PREPARACION".equals(linea.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud no admite cambios de asignación");
        if (motivo == null || motivo.isBlank() || motivo.length()>1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requiere un motivo de retiro");
        if (!repo.retirar(asociacionId,detalleId,usuario,motivo.trim()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Asignación activa no encontrada");
    }
}
