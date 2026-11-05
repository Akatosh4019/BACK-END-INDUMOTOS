package pe.indumotos.application;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.indumotos.domain.Llegada;
import pe.indumotos.domain.Motocicleta;

@Service
public class ConsultarDetalleLlegada {
    private final LlegadaRepository llegadas;
    private final MotocicletaRepository motocicletas;

    public ConsultarDetalleLlegada(LlegadaRepository llegadas, MotocicletaRepository motocicletas) {
        this.llegadas = llegadas;
        this.motocicletas = motocicletas;
    }

    public record Detalle(Llegada llegada, List<Motocicleta> fichas,
        int cantidadFichas, int diferenciaInformativa) {}

    public Detalle consultar(long llegadaId) {
        Llegada llegada = llegadas.buscarPorId(llegadaId);
        List<Motocicleta> fichas = motocicletas.listarPorLlegada(llegadaId);
        return new Detalle(llegada, fichas, fichas.size(),
            llegada.cantidadRecibida() - fichas.size());
    }
}
