package pe.indumotos.domain;

import java.time.OffsetDateTime;

public record Motocicleta(Long id, Long llegadaId, String marca, String modelo,
    String color, String chasisCompleto, String numeroMotor, String dua,
    String itemDua, String creadorUsuario, OffsetDateTime creadoEn) {}
