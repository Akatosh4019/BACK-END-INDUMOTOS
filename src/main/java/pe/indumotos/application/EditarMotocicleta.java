package pe.indumotos.application;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import pe.indumotos.domain.Motocicleta;

@Service
public class EditarMotocicleta {
    private final MotocicletaRepository repository;

    public EditarMotocicleta(MotocicletaRepository repository) {
        this.repository = repository;
    }

    public Motocicleta ejecutar(long id, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua) {
        repository.buscarPorId(id);
        Map<String, String> errores = new LinkedHashMap<>();
        obligatorio(errores, "marca", marca);
        obligatorio(errores, "modelo", modelo);
        obligatorio(errores, "color", color);
        obligatorio(errores, "chasisCompleto", chasisCompleto);
        obligatorio(errores, "numeroMotor", numeroMotor);
        obligatorio(errores, "dua", dua);
        obligatorio(errores, "itemDua", itemDua);
        if (!errores.isEmpty()) throw new RegistrarMotocicleta.DatosInvalidos(errores);
        String chasis = chasisCompleto.trim();
        repository.buscarIdPorChasis(chasis).filter(otroId -> otroId != id)
            .ifPresent(otroId -> { throw new RegistrarMotocicleta.ChasisDuplicado(otroId); });
        if (!repository.combinacionRegistrada(marca.trim(), modelo.trim(), color.trim()))
            throw new RegistrarMotocicleta.DatosInvalidos(Map.of(
                "modelo", "Selecciona una combinación registrada en el catálogo."));
        try {
            return repository.actualizar(id, marca.trim(), modelo.trim(), color.trim(),
                chasis, numeroMotor.trim(), dua.trim(), itemDua.trim());
        } catch (DuplicateKeyException ex) {
            long otroId = repository.buscarIdPorChasis(chasis).orElseThrow(() -> ex);
            throw new RegistrarMotocicleta.ChasisDuplicado(otroId);
        }
    }

    private void obligatorio(Map<String, String> errores, String campo, String valor) {
        if (valor == null || valor.isBlank()) errores.put(campo, "Este campo es obligatorio.");
    }
}
