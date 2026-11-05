package pe.indumotos.application;

import java.util.List;

public interface PermisoRepository {
    record Cuenta(long id, String nombreUsuario, String rolCodigo, String estado, String rolEstado) {}
    record Asignacion(String codigo, boolean porRol, String excepcion) {}

    Cuenta buscarPorNombre(String nombreUsuario);
    Cuenta buscarPorId(long id);
    boolean existeRolActivo(String codigo);
    boolean existePermiso(String codigo);
    List<Asignacion> asignaciones(long usuarioId);
    void cambiarRol(long usuarioId, String rolCodigo);
    void guardarExcepcion(long usuarioId, String codigo, String decision);
    void retirarExcepcion(long usuarioId, String codigo);
}
