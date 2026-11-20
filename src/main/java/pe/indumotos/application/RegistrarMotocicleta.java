package pe.indumotos.application;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Motocicleta;

@Service
public class RegistrarMotocicleta {
    public static class ChasisDuplicado extends IllegalArgumentException {
        private final long fichaExistenteId;

        public ChasisDuplicado(long fichaExistenteId) {
            super("El chasis completo ya está registrado.");
            this.fichaExistenteId = fichaExistenteId;
        }

        public long fichaExistenteId() { return fichaExistenteId; }
    }

    public static class DatosInvalidos extends IllegalArgumentException {
        private final Map<String, String> errores;

        public DatosInvalidos(Map<String, String> errores) {
            super("Datos inválidos para registrar la ficha");
            this.errores = Map.copyOf(errores);
        }

        public Map<String, String> errores() { return errores; }
    }

    private final MotocicletaRepository repository;

    public RegistrarMotocicleta(MotocicletaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Motocicleta ejecutar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (llegadaId <= 0) errores.put("llegadaId", "Selecciona una llegada válida.");
        campoObligatorio(errores, "marca", marca, "Ingresa la marca.");
        campoObligatorio(errores, "modelo", modelo, "Ingresa el modelo.");
        campoObligatorio(errores, "color", color, "Ingresa el color.");
        campoObligatorio(errores, "chasisCompleto", chasisCompleto, "Ingresa el chasis completo.");
        campoObligatorio(errores, "numeroMotor", numeroMotor, "Ingresa el número de motor.");
        campoObligatorio(errores, "dua", dua, "Ingresa la DUA.");
        campoObligatorio(errores, "itemDua", itemDua, "Ingresa el ítem de DUA.");
        if (creadorUsuario == null || creadorUsuario.isBlank())
            errores.put("creadorUsuario", "Inicia sesión para registrar la ficha.");
        if (!errores.isEmpty()) throw new DatosInvalidos(errores);
        if (!repository.existeLlegada(llegadaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Llegada no encontrada");
        }
        repository.bloquearLlegada(llegadaId);
        if (repository.registroTerminado(llegadaId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El registro de esta guía ya terminó.");
        String chasis = chasisCompleto.trim();
        repository.buscarIdPorChasis(chasis).ifPresent(id -> { throw new ChasisDuplicado(id); });
        if (!repository.combinacionRegistrada(marca.trim(), modelo.trim(), color.trim()))
            throw new DatosInvalidos(Map.of("modelo", "Selecciona una combinación registrada en el catálogo."));
        try {
            return repository.guardar(llegadaId, marca.trim(), modelo.trim(), color.trim(),
                chasis, numeroMotor.trim(), dua.trim(), itemDua.trim(), creadorUsuario);
        } catch (DuplicateKeyException ex) {
            long id = repository.buscarIdPorChasis(chasis).orElseThrow(() -> ex);
            throw new ChasisDuplicado(id);
        }
    }

    private void campoObligatorio(Map<String, String> errores, String codigo, String valor,
        String mensaje) {
        if (valor == null || valor.isBlank()) errores.put(codigo, mensaje);
    }

    public Motocicleta consultar(long id) {
        return repository.buscarPorId(id);
    }
}
