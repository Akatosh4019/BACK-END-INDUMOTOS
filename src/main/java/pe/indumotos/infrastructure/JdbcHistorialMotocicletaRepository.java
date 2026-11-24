package pe.indumotos.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.indumotos.application.HistorialMotocicletaRepository;

@Repository
public class JdbcHistorialMotocicletaRepository implements HistorialMotocicletaRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcHistorialMotocicletaRepository(JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
    }
    @Override public Unidad buscarUnidad(long id) {
        return jdbc.query("SELECT id_motocicleta,chasis_completo,marca,modelo,color FROM motocicleta WHERE id_motocicleta=:id",
            Map.of("id",id), (r,n)->new Unidad(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)))
            .stream().findFirst().orElse(null);
    }
    @Override public Pagina consultar(Unidad unidad, OffsetDateTime desde, OffsetDateTime hasta,
        int pagina, int tamano) {
        var fuentes = new ArrayList<>(List.of("llegadas","fichas","ubicaciones","asignaciones","seguimiento"));
        var consultas = new ArrayList<>(List.of(sql("base")));
        // Son módulos reales de las otras ramas oficiales; no se crean tablas ni eventos ficticios.
        agregar("envio_solicitud_motocicleta","envios",fuentes,consultas);
        if (existe("venta_motocicleta")) {
            String columna = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_attribute WHERE attrelid=to_regclass('venta_motocicleta') AND attname='id_registrador' AND NOT attisdropped)",Map.of(),Boolean.class))
                ? "id_registrador" : "id_vendedor";
            consultas.add(sql("ventas").formatted(columna)); fuentes.add("ventas");
        }
        agregar("disponibilidad_evento","disponibilidad",fuentes,consultas);
        agregar("diferencia_datos","diferencias",fuentes,consultas);
        String cte = "WITH eventos AS ("+String.join(" UNION ALL ",consultas)+") ";
        String filtro = " WHERE true";
        var params = new MapSqlParameterSource("id",unidad.id());
        if(desde!=null){filtro+=" AND fecha>=:desde";params.addValue("desde",desde);}
        if(hasta!=null){filtro+=" AND fecha<:hasta";params.addValue("hasta",hasta);}
        long total = jdbc.queryForObject(cte+"SELECT count(*) FROM eventos"+filtro,params,Long.class);
        params.addValue("limite",tamano).addValue("desplazamiento",(long)pagina*tamano);
        var eventos = jdbc.query(cte+"SELECT * FROM eventos"+filtro+" ORDER BY fecha,orden,clave LIMIT :limite OFFSET :desplazamiento",params,
            (r,n)->new Evento(r.getString("clave"),r.getObject("fecha",OffsetDateTime.class),r.getString("tipo"),r.getString("descripcion"),r.getString("registrado_por"),r.getString("origen"),r.getString("destino"),r.getObject("solicitud_id",Long.class)));
        return new Pagina(unidad,List.copyOf(eventos),pagina,tamano,total,(int)((total+tamano-1)/tamano),List.copyOf(fuentes));
    }
    private boolean existe(String tabla) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass(:tabla) IS NOT NULL",Map.of("tabla",tabla),Boolean.class));
    }
    private void agregar(String tabla,String fuente,List<String> fuentes,List<String> consultas) {
        if(existe(tabla)){consultas.add(sql(fuente));fuentes.add(fuente);}
    }
    private String sql(String nombre) {
        try { return new ClassPathResource("historial/"+nombre+".sql").getContentAsString(StandardCharsets.UTF_8); }
        catch(IOException ex){throw new IllegalStateException("No se pudo leer la consulta de historial: "+nombre,ex);}
    }
}
