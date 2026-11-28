package pe.indumotos.application.llegadas;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Llegada;

@Service
public class RegistrarGuiaLlegada {
    public static final int MAX_IMAGENES = 3;
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MIN_LADO = 600;
    private static final long MAX_PIXELES = 20_000_000;

    public record ImagenNueva(String nombreArchivo, String tipoMime, byte[] contenido) {}

    private final RegistrarLlegada llegadas;
    private final GuiaLlegadaRepository imagenes;

    public RegistrarGuiaLlegada(RegistrarLlegada llegadas, GuiaLlegadaRepository imagenes) {
        this.llegadas = llegadas;
        this.imagenes = imagenes;
    }

    @Transactional
    public Llegada registrar(String numeroGuia, String receptor, int declarada, int recibida,
        String observacion, List<ImagenNueva> archivos) {
        if (archivos == null || archivos.isEmpty() || archivos.size() > MAX_IMAGENES)
            throw invalido("Adjunta entre una y tres fotos de la guía.");
        if (numeroGuia == null || numeroGuia.isBlank() || numeroGuia.trim().length() > 120)
            throw invalido("El número de guía debe tener entre 1 y 120 caracteres.");
        if (observacion != null && observacion.trim().length() > 1000)
            throw invalido("La observación no puede superar 1000 caracteres.");
        for (ImagenNueva archivo : archivos) validar(archivo);
        Llegada llegada = llegadas.ejecutar(numeroGuia, receptor, declarada, recibida, observacion);
        for (int i = 0; i < archivos.size(); i++) {
            ImagenNueva archivo = archivos.get(i);
            imagenes.guardar(llegada.id(), i + 1, nombreSeguro(archivo, i + 1),
                archivo.tipoMime(), archivo.contenido());
        }
        return llegada;
    }

    public List<GuiaLlegadaRepository.Resumen> listar(long llegadaId) {
        llegadas.consultar(llegadaId);
        return imagenes.listar(llegadaId);
    }

    public GuiaLlegadaRepository.Imagen consultar(long llegadaId, long imagenId) {
        llegadas.consultar(llegadaId);
        return imagenes.buscar(llegadaId, imagenId);
    }

    private void validar(ImagenNueva archivo) {
        if (archivo == null || archivo.contenido() == null || archivo.contenido().length == 0
            || archivo.contenido().length > MAX_BYTES)
            throw invalido("Cada foto debe pesar entre 1 byte y 5 MB.");
        byte[] bytes = archivo.contenido();
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
            && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        boolean png = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89
            && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
            && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
        if (!(jpeg && "image/jpeg".equals(archivo.tipoMime())
            || png && "image/png".equals(archivo.tipoMime())))
            throw invalido("Solo se permiten fotos JPG o PNG válidas.");
        if (jpeg && (bytes.length < 4 || (bytes[bytes.length - 2] & 0xff) != 0xff
            || (bytes[bytes.length - 1] & 0xff) != 0xd9))
            throw invalido("La foto JPG está incompleta o dañada.");
        if (png && (bytes.length < 20 || bytes[bytes.length - 8] != 0x49
            || bytes[bytes.length - 7] != 0x45 || bytes[bytes.length - 6] != 0x4e
            || bytes[bytes.length - 5] != 0x44))
            throw invalido("La foto PNG está incompleta o dañada.");
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (entrada == null) throw invalido("No se pudo abrir la foto de la guía.");
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw invalido("No se pudo interpretar la foto de la guía.");
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                String formato = lector.getFormatName();
                if (!(jpeg && "JPEG".equalsIgnoreCase(formato)
                    || png && "PNG".equalsIgnoreCase(formato)))
                    throw invalido("El contenido de la foto no coincide con su tipo.");
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho < MIN_LADO || alto < MIN_LADO || (long) ancho * alto > MAX_PIXELES)
                    throw invalido("La foto debe medir al menos 600 px por lado y no superar 20 megapíxeles.");
                BufferedImage imagen = lector.read(0);
                if (imagen == null) throw invalido("La foto no se puede decodificar.");
                if (!tieneContraste(imagen)) {
                    imagen.flush();
                    throw invalido("La foto está vacía o tiene demasiado poco contraste.");
                }
                imagen.flush();
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalido("La foto está dañada o no se puede decodificar.");
        }
    }

    private boolean tieneContraste(BufferedImage imagen) {
        int paso = Math.max(1, Math.min(imagen.getWidth(), imagen.getHeight()) / 150);
        long suma = 0;
        long sumaCuadrados = 0;
        long muestras = 0;
        for (int y = 0; y < imagen.getHeight(); y += paso) {
            for (int x = 0; x < imagen.getWidth(); x += paso) {
                int color = imagen.getRGB(x, y);
                int luz = (77 * ((color >> 16) & 255) + 150 * ((color >> 8) & 255)
                    + 29 * (color & 255)) >> 8;
                suma += luz;
                sumaCuadrados += (long) luz * luz;
                muestras++;
            }
        }
        double media = (double) suma / muestras;
        return (double) sumaCuadrados / muestras - media * media >= 100;
    }

    private String nombreSeguro(ImagenNueva archivo, int orden) {
        String nombre = archivo.nombreArchivo() == null ? "" : archivo.nombreArchivo();
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1)
            .replaceAll("[\\p{Cntrl}]", "").trim();
        if (nombre.isBlank()) nombre = "guia-" + orden +
            ("image/png".equals(archivo.tipoMime()) ? ".png" : ".jpg");
        return nombre.length() > 180 ? nombre.substring(0, 180) : nombre;
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
