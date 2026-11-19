package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionarUbicaciones {
    private final UbicacionRepository repository;

    public GestionarUbicaciones(UbicacionRepository repository) {
        this.repository = repository;
    }

    public List<UbicacionRepository.Sede> sedes() { return repository.sedes(); }

    public UbicacionRepository.Sede crearSede(String codigo, String nombre, String tipo) {
        if (codigo == null || codigo.isBlank() || nombre == null || nombre.isBlank() ||
            tipo == null || !java.util.Set.of("ALMACEN", "TIENDA", "TALLER").contains(tipo))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de ubicación no válidos");
        return repository.crearSede(codigo.trim(), nombre.trim(), tipo);
    }

    public UbicacionRepository.Movimiento actual(long motocicletaId) {
        verificarMotocicleta(motocicletaId);
        return repository.actual(motocicletaId);
    }

    public List<UbicacionRepository.Movimiento> historial(long motocicletaId) {
        verificarMotocicleta(motocicletaId);
        return repository.historial(motocicletaId);
    }

    @Transactional
    public UbicacionRepository.Movimiento registrar(long motocicletaId, long destinoId, String usuario) {
        verificarMotocicleta(motocicletaId);
        repository.bloquearMotocicleta(motocicletaId);
        var sede = repository.sede(destinoId);
        if (sede == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ubicación inexistente o inactiva");
        var actual = repository.actual(motocicletaId);
        if (actual != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "La motocicleta ya tiene ubicación; registre un traslado para conservar su recorrido");
        return repository.registrar(motocicletaId, destinoId, usuario);
    }

    private void verificarMotocicleta(long id) {
        if (id <= 0 || !repository.existeMotocicleta(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Motocicleta no encontrada");
    }
}
