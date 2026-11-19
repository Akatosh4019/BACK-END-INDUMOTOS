package pe.indumotos.application;

import java.util.List;
import java.util.Optional;
import pe.indumotos.domain.Motocicleta;

public interface MotocicletaRepository {
    boolean existeLlegada(long llegadaId);
    Optional<Long> buscarIdPorChasis(String chasisCompleto);
    Motocicleta guardar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario);
    Motocicleta buscarPorId(long id);
    Motocicleta actualizar(long id, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua);
    List<Motocicleta> listarPorLlegada(long llegadaId);
    PaginaMotocicletas buscarPorFiltros(FiltroMotocicletas filtro);
}
