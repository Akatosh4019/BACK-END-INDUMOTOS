package pe.indumotos.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistroGuia {
    public record Borrador(int posicion, String marca, String modelo, String color,
        String chasisCompleto, String numeroMotor, String dua, String itemDua) {}
    public record Estado(boolean terminado, int recibidas, int registradas,
        List<Borrador> borradores) {}

    private final JdbcTemplate jdbc;
    private final RegistrarMotocicleta registrar;
    private final UbicacionRepository ubicaciones;

    public RegistroGuia(JdbcTemplate jdbc, RegistrarMotocicleta registrar, UbicacionRepository ubicaciones) {
        this.jdbc = jdbc;
        this.registrar = registrar;
        this.ubicaciones = ubicaciones;
    }

    public Estado consultar(long id) {
        Integer recibidas = jdbc.query("SELECT cantidad_recibida FROM llegada WHERE id_llegada=?",
            rs -> rs.next() ? rs.getInt(1) : null, id);
        if (recibidas == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guía no encontrada.");
        int registradas = jdbc.queryForObject("SELECT count(*) FROM motocicleta WHERE id_llegada=?", Integer.class, id);
        boolean terminado = Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM llegada WHERE id_llegada=? AND estado_registro='CONFIRMADA')",
            Boolean.class, id));
        List<Borrador> borradores = jdbc.query("SELECT posicion,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua " +
            "FROM borrador_motocicleta WHERE id_llegada=? ORDER BY posicion", (rs, row) ->
            new Borrador(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)), id);
        return new Estado(terminado, recibidas, registradas, borradores);
    }

    @Transactional
    public Estado guardar(long id, int posicion, Borrador b, String usuario) {
        int recibidas = bloquear(id);
        if (terminado(id)) throw conflicto("Esta guía ya está terminada.");
        int registradas = contarRegistradas(id);
        if (posicion < 1 || posicion > recibidas - registradas || b == null)
            throw invalido("La posición no corresponde a una moto pendiente.");
        String[] campos = {b.marca(),b.modelo(),b.color(),b.chasisCompleto(),b.numeroMotor(),b.dua(),b.itemDua()};
        for (String campo : campos) if (campo == null || campo.isBlank() || campo.length() > 120)
            throw invalido("Completa los siete campos; máximo 120 caracteres cada uno.");
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM catalogo_motocicleta " +
            "WHERE lower(btrim(marca))=lower(?) AND lower(btrim(modelo))=lower(?) AND lower(btrim(color))=lower(?))",
            Boolean.class,b.marca().trim(),b.modelo().trim(),b.color().trim())))
            throw invalido("Selecciona una marca, modelo y color del catálogo.");
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM motocicleta WHERE upper(btrim(chasis_completo))=upper(btrim(?)))",
            Boolean.class,b.chasisCompleto().trim())) || Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM borrador_motocicleta WHERE id_llegada=? AND posicion<>? " +
                "AND upper(btrim(chasis_completo))=upper(btrim(?)))",Boolean.class,id,posicion,b.chasisCompleto().trim())))
            throw conflicto("El chasis ya figura en otra ficha o borrador.");
        jdbc.update("INSERT INTO borrador_motocicleta(id_llegada,posicion,marca,modelo,color,chasis_completo,numero_motor,dua,item_dua,guardado_por) " +
            "SELECT ?,?,?,?,?,?,?,?,?,id_usuario FROM usuario WHERE nombre_usuario=? " +
            "ON CONFLICT(id_llegada,posicion) DO UPDATE SET marca=excluded.marca,modelo=excluded.modelo,color=excluded.color," +
            "chasis_completo=excluded.chasis_completo,numero_motor=excluded.numero_motor,dua=excluded.dua,item_dua=excluded.item_dua," +
            "guardado_por=excluded.guardado_por,actualizado_en=CURRENT_TIMESTAMP",
            id,posicion,b.marca().trim(),b.modelo().trim(),b.color().trim(),b.chasisCompleto().trim(),
            b.numeroMotor().trim(),b.dua().trim(),b.itemDua().trim(),usuario);
        return consultar(id);
    }

    @Transactional
    public Estado finalizar(long id, long almacenId, String usuario) {
        int recibidas = bloquear(id);
        if (terminado(id)) throw conflicto("Esta guía ya está terminada.");
        var almacen = ubicaciones.sede(almacenId);
        if (almacen == null || !"ALMACEN".equals(almacen.tipo()))
            throw invalido("Selecciona el almacén que recibió la guía.");
        Estado estado = consultar(id);
        int pendientes = recibidas - estado.registradas();
        if (pendientes < 0 || estado.borradores().size() != pendientes)
            throw invalido("Guarda todos los bloques pendientes antes de finalizar.");
        for (int i = 0; i < pendientes; i++) {
            Borrador b = estado.borradores().get(i);
            if (b.posicion() != i + 1) throw invalido("Falta un bloque pendiente.");
            registrar.ejecutar(id,b.marca(),b.modelo(),b.color(),b.chasisCompleto(),
                b.numeroMotor(),b.dua(),b.itemDua(),usuario);
        }
        var motos = jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_llegada=? ORDER BY id_motocicleta",
            (rs, fila) -> rs.getLong(1), id);
        for (long motoId : motos) {
            var actual = ubicaciones.actual(motoId);
            if (actual == null) ubicaciones.registrar(motoId, almacenId, usuario);
            else if (actual.destinoId() != almacenId)
                throw conflicto("Una moto de la guía figura en otro lugar. Revisa su ubicación antes de finalizar.");
        }
        jdbc.update("UPDATE llegada SET id_almacen_recepcion=? WHERE id_llegada=?", almacenId, id);
        jdbc.update("DELETE FROM borrador_motocicleta WHERE id_llegada=?",id);
        jdbc.update("INSERT INTO registro_guia(id_llegada,terminado_en,terminado_por) " +
            "SELECT ?,CURRENT_TIMESTAMP,id_usuario FROM usuario WHERE nombre_usuario=? " +
            "ON CONFLICT(id_llegada) DO UPDATE SET terminado_en=excluded.terminado_en,terminado_por=excluded.terminado_por",id,usuario);
        jdbc.update("UPDATE llegada SET estado_registro='CONFIRMADA' WHERE id_llegada=?",id);
        return consultar(id);
    }

    private int bloquear(long id) {
        Integer cantidad = jdbc.query("SELECT cantidad_recibida FROM llegada WHERE id_llegada=? FOR UPDATE",
            rs -> rs.next() ? rs.getInt(1) : null,id);
        if (cantidad == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Guía no encontrada.");
        return cantidad;
    }
    private int contarRegistradas(long id) {
        return jdbc.queryForObject("SELECT count(*) FROM motocicleta WHERE id_llegada=?",Integer.class,id);
    }
    private boolean terminado(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM llegada " +
            "WHERE id_llegada=? AND estado_registro='CONFIRMADA')",Boolean.class,id));
    }
    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT,mensaje);
    }
    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,mensaje);
    }
}
