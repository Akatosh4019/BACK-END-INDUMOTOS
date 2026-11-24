package pe.indumotos.application;
public interface CuentaSeguridadRepository {
    record Cuenta(long id,String usuario,String hash,String rol,String estado) {}
    Cuenta buscar(String usuario);
    Cuenta bloquear(long id);
    void cambiarClave(long id,String hash);
}
