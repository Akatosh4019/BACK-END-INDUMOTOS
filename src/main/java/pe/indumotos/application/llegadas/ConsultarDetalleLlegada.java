package pe.indumotos.application.llegadas;

import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.indumotos.domain.Llegada;
import pe.indumotos.domain.Motocicleta;
import pe.indumotos.application.inventario.MotocicletaRepository;

@Service
public class ConsultarDetalleLlegada {
    private final LlegadaRepository llegadas;
    private final MotocicletaRepository motocicletas;
    private final JdbcTemplate jdbc;

    public ConsultarDetalleLlegada(LlegadaRepository llegadas, MotocicletaRepository motocicletas) {
        this(llegadas, motocicletas, null);
    }

    @Autowired
    public ConsultarDetalleLlegada(LlegadaRepository llegadas, MotocicletaRepository motocicletas,
        JdbcTemplate jdbc) {
        this.llegadas = llegadas;
        this.motocicletas = motocicletas;
        this.jdbc = jdbc;
    }

    public record Detalle(Llegada llegada, List<Motocicleta> fichas,
        int cantidadFichas, int diferenciaInformativa,
        String confirmadoPor, OffsetDateTime confirmadoEn) {}

    public Detalle consultar(long llegadaId) {
        Llegada llegada = llegadas.buscarPorId(llegadaId);
        List<Motocicleta> fichas = motocicletas.listarPorLlegada(llegadaId);
        Object[] confirmacion = jdbc == null ? null : jdbc.query(
            "SELECT u.nombre_usuario, r.terminado_en FROM registro_guia r " +
            "LEFT JOIN usuario u ON u.id_usuario=r.terminado_por WHERE r.id_llegada=? AND r.terminado_en IS NOT NULL",
            rs -> rs.next() ? new Object[]{rs.getString(1),
                rs.getObject(2, OffsetDateTime.class)} : null, llegadaId);
        return new Detalle(llegada, fichas, fichas.size(),
            llegada.cantidadRecibida() - fichas.size(),
            confirmacion == null ? null : (String) confirmacion[0],
            confirmacion == null ? null : (OffsetDateTime) confirmacion[1]);
    }
}
