package pe.indumotos.application;

import java.util.List;
import java.util.Optional;
import pe.indumotos.domain.Motocicleta;

public interface MotocicletaRepository {
    boolean existeLlegada(long llegadaId);
    Optional<Long> buscarIdPorChasis(String chasisCompleto);
    PaginaBusquedaChasis buscarPorTerminacion(String ultimosCuatro, int pagina, int tamano);
    Motocicleta guardar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario);
    Motocicleta buscarPorId(long id);
    List<Motocicleta> listarPorLlegada(long llegadaId);
    PaginaMotocicletas buscarPorFiltros(FiltroMotocicletas filtro);
}
