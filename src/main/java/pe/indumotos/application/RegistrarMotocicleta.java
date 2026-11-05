package pe.indumotos.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Motocicleta;

@Service
public class RegistrarMotocicleta {
    private final MotocicletaRepository repository;

    public RegistrarMotocicleta(MotocicletaRepository repository) {
        this.repository = repository;
    }

    public Motocicleta ejecutar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario) {
        if (!repository.existeLlegada(llegadaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Llegada no encontrada");
        }
        return repository.guardar(llegadaId, marca.trim(), modelo.trim(), color.trim(),
            chasisCompleto.trim(), numeroMotor.trim(), dua.trim(), itemDua.trim(), creadorUsuario);
    }

    public Motocicleta consultar(long id) {
        return repository.buscarPorId(id);
    }
}
