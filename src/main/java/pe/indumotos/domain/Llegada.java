package pe.indumotos.domain;

import java.time.OffsetDateTime;

public record Llegada(Long id, String numeroGuia, OffsetDateTime fechaLlegada,
    Long receptorId, String receptorUsuario, int cantidadDeclarada,
    int cantidadRecibida, String observacion) {}
