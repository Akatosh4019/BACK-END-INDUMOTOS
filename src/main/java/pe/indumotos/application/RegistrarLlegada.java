package pe.indumotos.application;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
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

    public java.util.List<Llegada> buscar(String guia, int limite) {
        if(limite < 1 || limite > 50 || (guia != null && guia.length() > 120))
            throw new DatosInvalidos(Map.of("numeroGuia", "Usa una guía de hasta 120 caracteres y límite entre 1 y 50."));
        return repository.buscar(guia == null ? "" : guia.trim(), limite);
    }

    public Llegada consultar(long id) {
        return repository.buscarPorId(id);
    }
}
