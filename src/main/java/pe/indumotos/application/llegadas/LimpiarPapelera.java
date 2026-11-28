package pe.indumotos.application.llegadas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimpiarPapelera {
    private final CorregirLlegada correcciones;

    public LimpiarPapelera(CorregirLlegada correcciones) { this.correcciones = correcciones; }

    @Scheduled(cron = "0 15 2 * * *", zone = "America/Lima")
    public void ejecutar() { correcciones.purgarVencidas(); }
}
