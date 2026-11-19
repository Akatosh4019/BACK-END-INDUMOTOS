package pe.indumotos.application;

import pe.indumotos.domain.Llegada;

public interface LlegadaRepository {
    Llegada guardar(String numeroGuia, String receptorUsuario, int cantidadDeclarada,
        int cantidadRecibida, String observacion);
    Llegada buscarPorId(long id);
    java.util.List<Llegada> buscar(String guia, int limite);
}
