package pe.indumotos.application;

import java.time.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultarHistorialMotocicleta {
    private static final ZoneId PERU = ZoneId.of("America/Lima");
    private final HistorialMotocicletaRepository repository;
    public ConsultarHistorialMotocicleta(HistorialMotocicletaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HistorialMotocicletaRepository.Pagina consultar(long id, LocalDate desde,
        LocalDate hasta, int pagina, int tamano) {
        if (id <= 0 || pagina < 0 || tamano < 1 || tamano > 100 || (long) pagina * tamano > Integer.MAX_VALUE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador o paginación no válidos.");
        if ((desde != null && (desde.getYear() < 1 || desde.getYear() > 9998))
            || (hasta != null && (hasta.getYear() < 1 || hasta.getYear() > 9998))
            || (desde != null && hasta != null && desde.isAfter(hasta)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa el intervalo de fechas.");
        var unidad = repository.buscarUnidad(id);
        if (unidad == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Motocicleta no encontrada.");
        return repository.consultar(unidad,
            desde == null ? null : desde.atStartOfDay(PERU).toOffsetDateTime(),
            hasta == null ? null : hasta.plusDays(1).atStartOfDay(PERU).toOffsetDateTime(), pagina, tamano);
    }
}
