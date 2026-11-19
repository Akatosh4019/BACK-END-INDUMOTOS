package pe.indumotos.application;

import java.util.List;
import pe.indumotos.domain.Llegada;

public interface LlegadaRepository {
    Llegada guardar(String numeroGuia, String receptorUsuario, int cantidadDeclarada,
        int cantidadRecibida, String observacion);
    Llegada buscarPorId(long id);
    List<Llegada> buscarPorGuia(String numeroGuia);
}
