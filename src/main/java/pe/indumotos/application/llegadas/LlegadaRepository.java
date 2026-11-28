package pe.indumotos.application.llegadas;

import java.util.List;
import java.time.LocalDate;
import pe.indumotos.domain.Llegada;

public interface LlegadaRepository {
    Llegada guardar(String numeroGuia, String receptorUsuario, int cantidadDeclarada,
        int cantidadRecibida, String observacion);
    Llegada buscarPorId(long id);
    List<Llegada> buscarPorGuia(String numeroGuia);
    List<Llegada> listar(String numeroGuia, int limite, long desplazamiento);
    long contar(String numeroGuia);
    List<Llegada> listarPorEstado(String numeroGuia, String estado, int limite, long desplazamiento);
    long contarPorEstado(String numeroGuia, String estado);
    List<Llegada> listarFiltradas(String numeroGuia, String estado, LocalDate fecha,
        Integer cantidad, String receptor, int limite, long desplazamiento);
    long contarFiltradas(String numeroGuia, String estado, LocalDate fecha,
        Integer cantidad, String receptor);
}
