package pe.indumotos.application.llegadas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExtraerBorradoresGuia {
    private static final Pattern CHASIS = Pattern.compile("(?i)Chasis[^A-Z0-9\\r\\n]{0,4}([A-Z0-9]{12,24})");
    private static final Pattern MOTOR = Pattern.compile("(?i)Motor[^A-Z0-9\\r\\n]{0,4}([A-Z0-9]{8,30})");
    private static final Pattern MARCA = Pattern.compile("(?i)Marca[^A-Z0-9\\r\\n]{0,4}([A-Z][A-Z0-9 ]{1,25}?)(?=\\s*Chasis|\\r?\\n|$)");
    private static final Pattern MODELO = Pattern.compile("(?i)Modelo[^A-Z0-9\\r\\n]{0,4}([A-Z][A-Z0-9 .]{1,25}?)(?=\\s*Color|\\s*Marca|\\r?\\n|$)");
    private static final Pattern COLOR = Pattern.compile("(?i)Color[^A-Z0-9\\r\\n]{0,4}([A-Z][A-Z, /]{1,25}?)(?=\\s*Marca|\\s*Chasis|\\r?\\n|$)");
    private static final Pattern DUA = Pattern.compile("\\b(\\d{3}-\\d{4}-\\d{2}-\\d{5,6}[-:]\\d{2})");
    private static final Pattern ITEM = Pattern.compile("(?i)(?:IT|1T)[ .:_-]*(\\d{1,4})");

    public record Sugerencia(int pagina, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua) {}
    public record Resultado(int fotosAnalizadas, List<Sugerencia> sugerencias) {}

    private final RegistrarLlegada llegadas;
    private final GuiaLlegadaRepository imagenes;
    private final JdbcTemplate jdbc;

    public ExtraerBorradoresGuia(RegistrarLlegada llegadas, GuiaLlegadaRepository imagenes, JdbcTemplate jdbc) {
        this.llegadas = llegadas;
        this.imagenes = imagenes;
        this.jdbc = jdbc;
    }

    @Transactional
    public Resultado extraer(long llegadaId) {
        llegadas.consultar(llegadaId);
        var guardadas = jdbc.query("SELECT fotos_analizadas FROM ocr_lectura_guia WHERE id_llegada=?",
            (rs,n) -> rs.getInt(1), llegadaId);
        if (!guardadas.isEmpty()) {
            var sugerencias = jdbc.query("SELECT pagina,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua " +
                "FROM ocr_sugerencia_guia WHERE id_llegada=? ORDER BY posicion",
                (rs,n) -> new Sugerencia(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),
                    rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)), llegadaId);
            return new Resultado(guardadas.getFirst(), sugerencias);
        }
        var fotos = imagenes.listar(llegadaId);
        if (fotos.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Esta guía no tiene fotos para autocompletar.");
        List<Sugerencia> sugerencias = new ArrayList<>();
        Set<String> chasisVistos = new HashSet<>();
        for (var foto : fotos) {
            String texto = reconocer(imagenes.buscar(llegadaId, foto.id()));
            for (Sugerencia sugerencia : analizar(texto, foto.orden())) {
                if (chasisVistos.add(sugerencia.chasisCompleto())) sugerencias.add(sugerencia);
            }
        }
        jdbc.update("INSERT INTO ocr_lectura_guia(id_llegada,fotos_analizadas) VALUES (?,?)",
            llegadaId,fotos.size());
        for (int i=0; i<sugerencias.size(); i++) {
            var s=sugerencias.get(i);
            jdbc.update("INSERT INTO ocr_sugerencia_guia(id_llegada,posicion,pagina,marca,modelo,color," +
                "chasis_completo,numero_motor,dua,item_dua) VALUES (?,?,?,?,?,?,?,?,?,?)",
                llegadaId,i+1,s.pagina(),s.marca(),s.modelo(),s.color(),s.chasisCompleto(),
                s.numeroMotor(),s.dua(),s.itemDua());
        }
        return new Resultado(fotos.size(), sugerencias);
    }

    private String reconocer(GuiaLlegadaRepository.Imagen foto) {
        Path temporal = null;
        Process proceso = null;
        try {
            temporal = Files.createTempFile("indumotos-guia-", "." +
                ("image/png".equals(foto.tipoMime()) ? "png" : "jpg"));
            Files.write(temporal, foto.contenido());
            proceso = new ProcessBuilder("tesseract", temporal.toString(), "stdout", "-l", "spa")
                .redirectErrorStream(true).start();
            if (!proceso.waitFor(30, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La lectura de la guía tardó demasiado. Inténtalo de nuevo.");
            }
            byte[] salida = proceso.getInputStream().readNBytes(1_000_001);
            if (proceso.exitValue() != 0 || salida.length > 1_000_000)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No se pudo leer el texto de una foto de la guía.");
            return new String(salida, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El lector de guías no está disponible.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Se interrumpió la lectura de la guía.", ex);
        } finally {
            if (proceso != null && proceso.isAlive()) proceso.destroyForcibly();
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException ignored) { }
            }
        }
    }

    List<Sugerencia> analizar(String texto, int pagina) {
        List<Sugerencia> resultado = new ArrayList<>();
        Matcher chasis = CHASIS.matcher(texto);
        while (chasis.find()) {
            int inicio = Math.max(0, chasis.start() - 220);
            int fin = Math.min(texto.length(), chasis.end() + 180);
            String antes = texto.substring(inicio, chasis.start());
            String despues = texto.substring(chasis.end(), fin);
            resultado.add(new Sugerencia(pagina,
                ultimo(MARCA, antes), ultimo(MODELO, antes), ultimo(COLOR, antes),
                chasis.group(1).toUpperCase(), primero(MOTOR, despues), primero(DUA, despues),
                primero(ITEM, despues)));
        }
        return resultado;
    }

    private String ultimo(Pattern patron, String texto) {
        Matcher matcher = patron.matcher(texto);
        String valor = "";
        while (matcher.find()) valor = matcher.group(1).trim();
        return valor;
    }

    private String primero(Pattern patron, String texto) {
        Matcher matcher = patron.matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
