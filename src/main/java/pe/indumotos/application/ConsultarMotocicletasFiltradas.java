package pe.indumotos.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultarMotocicletasFiltradas {
    private static final int TAMANO_MAXIMO = 100;
    private final MotocicletaRepository repository;

    public ConsultarMotocicletasFiltradas(MotocicletaRepository repository) {
        this.repository = repository;
    }

    public PaginaMotocicletas consultar(String marca, String modelo, String color, String chasis,
        int pagina, int tamano) {
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "La página no puede ser negativa");
        if (tamano < 1 || tamano > TAMANO_MAXIMO) throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "El tamaño debe estar entre 1 y " + TAMANO_MAXIMO);
        String chasisNormalizado = normalizar(chasis);
        if (chasisNormalizado != null && chasisNormalizado.length() < 4)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ingresa el chasis completo o sus últimos cuatro dígitos");
        if (chasisNormalizado != null && chasisNormalizado.length() == 4
            && !chasisNormalizado.matches("\\d{4}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los últimos cuatro caracteres deben ser dígitos");
        return repository.buscarPorFiltros(new FiltroMotocicletas(
            normalizar(marca), normalizar(modelo), normalizar(color), chasisNormalizado,
            pagina, tamano));
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
