package pe.indumotos.application;

import pe.indumotos.domain.Motocicleta;

public interface MotocicletaRepository {
    boolean existeLlegada(long llegadaId);
    Motocicleta guardar(long llegadaId, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua,
        String creadorUsuario);
    Motocicleta buscarPorId(long id);
}
