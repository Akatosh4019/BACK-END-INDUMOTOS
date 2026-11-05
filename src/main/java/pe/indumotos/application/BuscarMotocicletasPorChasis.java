package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuscarMotocicletasPorChasis {
    private final MotocicletaRepository repository;
    public BuscarMotocicletasPorChasis(MotocicletaRepository repository) { this.repository = repository; }

    public PaginaBusquedaChasis buscar(String chasisCompleto, String ultimosCuatro, int pagina, int tamano) {
        // Un parámetro enviado vacío es inválido: no se transforma en consulta sin filtro.
        if ((chasisCompleto == null) == (ultimosCuatro == null))
            throw invalido("Indica solo chasisCompleto o ultimosCuatro.");
        if (pagina < 0 || tamano < 1 || tamano > 100)
            throw invalido("La página debe ser cero o mayor y el tamaño entre 1 y 100.");
        if (ultimosCuatro != null) {
            String sufijo = ultimosCuatro.trim();
            if (sufijo.length() != 4) throw invalido("Ingresa exactamente los cuatro caracteres finales del chasis.");
            return repository.buscarPorTerminacion(sufijo, pagina, tamano);
        }
        String completo = chasisCompleto.trim();
        if (completo.isEmpty() || completo.length() > 120)
            throw invalido("Ingresa el chasis completo, de hasta 120 caracteres.");
        var id = repository.buscarIdPorChasis(completo);
        if (id.isEmpty()) return new PaginaBusquedaChasis(List.of(), pagina, tamano, 0, 0);
        if (pagina > 0) return new PaginaBusquedaChasis(List.of(), pagina, tamano, 1, 1);
        var ficha = repository.buscarPorId(id.get());
        var resultado = new ResultadoBusquedaChasis(ficha.id(), ficha.marca(), ficha.modelo(),
            ficha.color(), ficha.chasisCompleto());
        return new PaginaBusquedaChasis(List.of(resultado), pagina, tamano, 1, 1);
    }
    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
