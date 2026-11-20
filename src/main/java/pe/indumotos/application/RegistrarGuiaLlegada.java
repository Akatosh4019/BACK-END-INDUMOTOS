package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.indumotos.domain.Llegada;

@Service
public class RegistrarGuiaLlegada {
    public static final int MAX_IMAGENES = 3;
    public static final int MAX_BYTES = 5 * 1024 * 1024;

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
