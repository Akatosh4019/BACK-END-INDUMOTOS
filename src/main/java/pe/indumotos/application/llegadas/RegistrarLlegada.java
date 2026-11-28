package pe.indumotos.application.llegadas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Llegada;

@Service
public class RegistrarLlegada {
    public static class DatosInvalidos extends IllegalArgumentException {
        private final Map<String, String> errores;

        public DatosInvalidos(Map<String, String> errores) {
            super("Datos inválidos para registrar la llegada");
            this.errores = Map.copyOf(errores);
        }

        public Map<String, String> errores() { return errores; }
    }

    private final LlegadaRepository repository;

    public RegistrarLlegada(LlegadaRepository repository) {
        this.repository = repository;
    }

    public Llegada ejecutar(String numeroGuia, String receptorUsuario, int cantidadDeclarada,
        int cantidadRecibida, String observacion) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (numeroGuia == null || numeroGuia.isBlank())
            errores.put("numeroGuia", "Ingresa el número de guía.");
        if (receptorUsuario == null || receptorUsuario.isBlank())
            errores.put("usuarioReceptor", "Inicia sesión para registrar la llegada.");
        if (cantidadDeclarada <= 0)
            errores.put("cantidadDeclarada", "Ingresa una cantidad declarada mayor que cero.");
        if (cantidadRecibida <= 0)
            errores.put("cantidadRecibida", "Ingresa una cantidad recibida mayor que cero.");
        if (!errores.isEmpty()) throw new DatosInvalidos(errores);
        return repository.guardar(numeroGuia.trim(), receptorUsuario, cantidadDeclarada,
            cantidadRecibida, observacion == null || observacion.isBlank() ? null : observacion.trim());
    }

    public Llegada consultar(long id) {
        return repository.buscarPorId(id);
    }

    public List<Llegada> buscarPorGuia(String numeroGuia) {
        if (numeroGuia == null || numeroGuia.isBlank() || numeroGuia.length() > 120)
            throw new DatosInvalidos(Map.of("numeroGuia", "Ingresa un número de guía válido."));
        return repository.buscarPorGuia(numeroGuia.trim());
    }

    public record PaginaLlegadas(List<Llegada> items, int pagina, int tamano, long totalElementos,
        long totalPaginas) {}

    public PaginaLlegadas listar(String numeroGuia, int pagina) {
        return listar(numeroGuia, pagina, "");
    }

    public PaginaLlegadas listar(String numeroGuia, int pagina, String estado) {
        return listar(numeroGuia, pagina, estado, null, null, "");
    }

    public PaginaLlegadas listar(String numeroGuia, int pagina, String estado,
        LocalDate fecha, Integer cantidad, String receptor) {
        String filtro = numeroGuia == null ? "" : numeroGuia.trim();
        String usuario = receptor == null ? "" : receptor.trim();
        if (filtro.length() > 120 || pagina < 0 || (long) pagina * 10 > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro o página no válidos");
        if (usuario.length() > 120 || (cantidad != null && cantidad <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsable o cantidad no válidos");
        String estadoSeguro = estado == null ? "" : estado.trim().toUpperCase(java.util.Locale.ROOT);
        if (!estadoSeguro.isEmpty() && !estadoSeguro.equals("PENDIENTE") && !estadoSeguro.equals("CONFIRMADA"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de guía no válido");
        boolean filtrosAdicionales = fecha != null || cantidad != null || !usuario.isEmpty();
        long total = filtrosAdicionales ? repository.contarFiltradas(filtro, estadoSeguro, fecha, cantidad, usuario)
            : estadoSeguro.isEmpty() ? repository.contar(filtro) : repository.contarPorEstado(filtro, estadoSeguro);
        List<Llegada> items = filtrosAdicionales
            ? repository.listarFiltradas(filtro, estadoSeguro, fecha, cantidad, usuario, 10, (long) pagina * 10)
            : estadoSeguro.isEmpty() ? repository.listar(filtro, 10, (long) pagina * 10)
            : repository.listarPorEstado(filtro, estadoSeguro, 10, (long) pagina * 10);
        return new PaginaLlegadas(items,
            pagina, 10, total, (total + 9) / 10);
    }
}
