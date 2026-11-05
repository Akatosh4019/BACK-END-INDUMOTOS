package pe.indumotos.application;

import org.springframework.stereotype.Service;
import pe.indumotos.domain.Llegada;

@Service
public class RegistrarLlegada {
    private final LlegadaRepository repository;

    public RegistrarLlegada(LlegadaRepository repository) {
        this.repository = repository;
    }

    public Llegada ejecutar(String numeroGuia, String receptorUsuario, int cantidadDeclarada,
        int cantidadRecibida, String observacion) {
        return repository.guardar(numeroGuia.trim(), receptorUsuario, cantidadDeclarada,
            cantidadRecibida, observacion == null || observacion.isBlank() ? null : observacion.trim());
    }

    public Llegada consultar(long id) {
        return repository.buscarPorId(id);
    }
}
