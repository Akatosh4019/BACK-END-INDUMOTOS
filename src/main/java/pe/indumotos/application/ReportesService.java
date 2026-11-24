package pe.indumotos.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Reportes de solo lectura sobre ventas registradas por Facturación y el stock actual. */
@Service
public class ReportesService {
    private static final String ZONA = "America/Lima";
    private final JdbcTemplate jdbc;

    public ReportesService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public enum Periodo {
        SEMANA("7 days", "day", 7), MES("30 days", "day", 30), ANIO("12 months", "month", 12);
        final String intervalo; final String unidad; final int puntos;
        Periodo(String intervalo, String unidad, int puntos) {
            this.intervalo = intervalo; this.unidad = unidad; this.puntos = puntos;
        }
    }

    public record Modelo(String marca, String modelo, long cantidad, BigDecimal importe) {}
    public record Tienda(String tienda, long cantidad, BigDecimal importe) {}
    public record Punto(LocalDate fecha, long cantidad) {}
    public record Stock(String marca, String modelo, long total, long enAlmacen, long enTienda, long enCamino) {}
    public record ReporteVentas(String periodo, LocalDate desde, LocalDate hasta, long unidadesVendidas,
        BigDecimal importeTotal, List<Modelo> masVendidos, List<Modelo> menosVendidos, List<Tienda> porTienda,
        List<Punto> serie, List<Stock> stock) {}

    public static Periodo periodo(String valor) {
        try { return Periodo.valueOf((valor == null ? "MES" : valor.trim()).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periodo no válido: usa SEMANA, MES o ANIO");
        }
    }

    public ReporteVentas ventas(Periodo p) {
        // Inicio del periodo en hora de Lima: hoy y los días/meses anteriores completos.
        String inicio = "(date_trunc('" + p.unidad + "', CURRENT_TIMESTAMP AT TIME ZONE '" + ZONA + "') - interval '"
            + p.intervalo + "' + interval '1 " + p.unidad + "')";
        String enPeriodo = " (v.vendida_en AT TIME ZONE '" + ZONA + "') >= " + inicio + " ";

        var resumen = jdbc.queryForObject("SELECT count(*), coalesce(sum(importe),0) FROM venta_motocicleta v WHERE"
            + enPeriodo, (rs, n) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2)});
        LocalDate desde = jdbc.queryForObject("SELECT CAST(" + inicio + " AS DATE)", LocalDate.class);
        LocalDate hasta = jdbc.queryForObject("SELECT CAST(CURRENT_TIMESTAMP AT TIME ZONE '" + ZONA + "' AS DATE)",
            LocalDate.class);

        String porModelo = "SELECT m.marca,m.modelo,count(*),sum(v.importe) FROM venta_motocicleta v " +
            "JOIN motocicleta m ON m.id_motocicleta=v.id_motocicleta WHERE" + enPeriodo + "GROUP BY m.marca,m.modelo ";
        List<Modelo> mas = jdbc.query(porModelo + "ORDER BY count(*) DESC, sum(v.importe) DESC, m.marca, m.modelo LIMIT 5",
            (rs, n) -> new Modelo(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4)));
        // Los menos vendidos incluyen modelos activos del catálogo o con stock que no vendieron nada.
        List<Modelo> menos = jdbc.query("WITH ventas AS (" + porModelo + "), modelos AS (" +
            "SELECT DISTINCT marca,modelo FROM catalogo_motocicleta WHERE activo UNION " +
            "SELECT DISTINCT m.marca,m.modelo FROM motocicleta m WHERE NOT EXISTS " +
            "(SELECT 1 FROM venta_motocicleta x WHERE x.id_motocicleta=m.id_motocicleta)) " +
            "SELECT o.marca,o.modelo,coalesce(v.count,0),coalesce(v.sum,0) FROM modelos o " +
            "LEFT JOIN ventas v ON lower(v.marca)=lower(o.marca) AND lower(v.modelo)=lower(o.modelo) " +
            "ORDER BY 3, o.marca, o.modelo LIMIT 5",
            (rs, n) -> new Modelo(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4)));
        List<Tienda> tiendas = jdbc.query("SELECT t.nombre,count(*),sum(v.importe) FROM venta_motocicleta v " +
            "JOIN ubicacion t ON t.id_ubicacion=v.id_tienda WHERE" + enPeriodo +
            "GROUP BY t.nombre ORDER BY count(*) DESC, t.nombre",
            (rs, n) -> new Tienda(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)));
        List<Punto> serie = jdbc.query("SELECT CAST(g AS DATE), count(v.id_venta) FROM generate_series(" + inicio +
            ", date_trunc('" + p.unidad + "', CURRENT_TIMESTAMP AT TIME ZONE '" + ZONA + "'), interval '1 " + p.unidad +
            "') g LEFT JOIN venta_motocicleta v ON date_trunc('" + p.unidad + "', v.vendida_en AT TIME ZONE '" + ZONA +
            "')=g GROUP BY g ORDER BY g", (rs, n) -> new Punto(rs.getObject(1, LocalDate.class), rs.getLong(2)));
        List<Stock> stock = jdbc.query("SELECT m.marca,m.modelo,count(*)," +
            "count(*) FILTER (WHERE u.tipo='ALMACEN' AND e.id_envio IS NULL)," +
            "count(*) FILTER (WHERE u.tipo='TIENDA' AND e.id_envio IS NULL)," +
            "count(e.id_envio) FROM motocicleta m " +
            "LEFT JOIN motocicleta_ubicacion_actual a ON a.id_motocicleta=m.id_motocicleta " +
            "LEFT JOIN ubicacion u ON u.id_ubicacion=a.id_ubicacion " +
            "LEFT JOIN envio_solicitud_motocicleta e ON e.id_motocicleta=m.id_motocicleta AND e.recibido_en IS NULL " +
            "WHERE NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=m.id_motocicleta) " +
            "GROUP BY m.marca,m.modelo ORDER BY count(*) DESC, m.marca, m.modelo",
            (rs, n) -> new Stock(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
                rs.getLong(6)));
        return new ReporteVentas(p.name(), desde, hasta, (Long) resumen[0], (BigDecimal) resumen[1],
            mas, menos, tiendas, serie, stock);
    }
}
