package pe.indumotos.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VentasService {
    public record Tienda(long id,String nombre,long disponibles,long vendidas) {}
    public record Moto(long id,String chasis,String marca,String modelo,String color) {}
    public record Venta(long id,String chasis,String referenciaPago,String referencia,BigDecimal importe,
        String registrador,OffsetDateTime fecha) {}
    public record Pagina<T>(List<T> items,int pagina,long totalElementos) {}
    private final JdbcTemplate jdbc;
    public VentasService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public List<Tienda> compania() {
        return jdbc.query("SELECT t.id_ubicacion,t.nombre,"+
            "(SELECT count(*) FROM motocicleta_ubicacion_actual a WHERE a.id_ubicacion=t.id_ubicacion " +
            "AND NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=a.id_motocicleta) " +
            "AND NOT EXISTS(SELECT 1 FROM pago_venta pv WHERE pv.id_motocicleta=a.id_motocicleta AND pv.estado='PENDIENTE') " +
            "AND NOT EXISTS(SELECT 1 FROM envio_solicitud_motocicleta e WHERE e.id_motocicleta=a.id_motocicleta AND e.recibido_en IS NULL) " +
            "AND coalesce((SELECT d.estado FROM disponibilidad_evento d WHERE d.id_motocicleta=a.id_motocicleta " +
            "ORDER BY d.id_evento DESC LIMIT 1),'HABILITADA') <> 'BLOQUEADA' " +
            "AND NOT EXISTS(SELECT 1 FROM diferencia_datos f WHERE f.id_motocicleta=a.id_motocicleta AND f.estado='PENDIENTE')),"+
            "(SELECT count(*) FROM venta_motocicleta v WHERE v.id_tienda=t.id_ubicacion) " +
            "FROM ubicacion t WHERE t.tipo='TIENDA' AND t.activo ORDER BY t.nombre",
            (rs,n)->new Tienda(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4)));
    }

    public long tienda(String usuario,Long elegida) {
        var cuenta=jdbc.query("SELECT r.codigo,ut.id_tienda FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "LEFT JOIN usuario_tienda ut ON ut.id_usuario=u.id_usuario " +
            "WHERE u.nombre_usuario=? AND u.estado='ACTIVO'",
            (rs,n)->new Object[]{rs.getString(1),rs.getObject(2,Long.class)},usuario);
        if(cuenta.isEmpty()) throw error(HttpStatus.FORBIDDEN,"Cuenta inactiva");
        boolean admin="ADMIN".equals(cuenta.getFirst()[0]);
        Long asignada=(Long)cuenta.getFirst()[1];
        if(!admin && asignada==null) throw error(HttpStatus.CONFLICT,"Administración debe asignarte una tienda");
        if(admin && elegida==null) throw error(HttpStatus.BAD_REQUEST,"Elige una tienda");
        if(!admin && elegida!=null && !elegida.equals(asignada))
            throw error(HttpStatus.FORBIDDEN,"No tienes acceso a esa tienda");
        long id=admin?elegida:asignada;
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM ubicacion " +
            "WHERE id_ubicacion=? AND tipo='TIENDA' AND activo)",Boolean.class,id)))
            throw error(HttpStatus.BAD_REQUEST,"Tienda no válida");
        return id;
    }

    public Tienda detalle(String usuario,Long elegida) {
        long id=tienda(usuario,elegida);
        return compania().stream().filter(t->t.id()==id).findFirst().orElseThrow();
    }

    @Transactional
    public void asignar(long usuarioId,long tiendaId,String actor) {
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM ubicacion " +
            "WHERE id_ubicacion=? AND tipo='TIENDA' AND activo)",Boolean.class,tiendaId)))
            throw error(HttpStatus.BAD_REQUEST,"Tienda no válida");
        int n=jdbc.update("INSERT INTO usuario_tienda(id_usuario,id_tienda,id_asignador) " +
            "SELECT u.id_usuario,?,a.id_usuario FROM usuario u JOIN rol r ON r.id_rol=u.id_rol " +
            "JOIN usuario a ON a.nombre_usuario=? AND a.estado='ACTIVO' " +
            "WHERE u.id_usuario=? AND u.estado='ACTIVO' AND r.codigo IN ('COMERCIAL','FACTURACION') " +
            "ON CONFLICT(id_usuario) DO UPDATE SET id_tienda=EXCLUDED.id_tienda," +
            "id_asignador=EXCLUDED.id_asignador,asignada_en=CURRENT_TIMESTAMP",
            tiendaId,actor,usuarioId);
        if(n==0) throw error(HttpStatus.BAD_REQUEST,"Elige una cuenta activa de Comercial o Facturación");
    }

    public Pagina<Moto> stock(String usuario,Long elegida,int pagina) {
        long id=tienda(usuario,elegida); validarPagina(pagina);
        String from=" FROM motocicleta m JOIN motocicleta_ubicacion_actual a ON a.id_motocicleta=m.id_motocicleta " +
            "WHERE a.id_ubicacion=? AND NOT EXISTS(SELECT 1 FROM pago_venta pv WHERE pv.id_motocicleta=m.id_motocicleta AND pv.estado='PENDIENTE') AND NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=m.id_motocicleta) " +
            "AND NOT EXISTS(SELECT 1 FROM envio_solicitud_motocicleta e WHERE e.id_motocicleta=m.id_motocicleta AND e.recibido_en IS NULL) " +
            "AND coalesce((SELECT d.estado FROM disponibilidad_evento d WHERE d.id_motocicleta=m.id_motocicleta " +
            "ORDER BY d.id_evento DESC LIMIT 1),'HABILITADA') <> 'BLOQUEADA' " +
            "AND NOT EXISTS(SELECT 1 FROM diferencia_datos f WHERE f.id_motocicleta=m.id_motocicleta AND f.estado='PENDIENTE')";
        long total=jdbc.queryForObject("SELECT count(*)"+from,Long.class,id);
        var items=jdbc.query("SELECT m.id_motocicleta,m.chasis_completo,m.marca,m.modelo,m.color"+from+
            " ORDER BY m.marca,m.modelo,m.chasis_completo LIMIT 10 OFFSET ?",
            (rs,n)->new Moto(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),
            id,(long)pagina*10);
        return new Pagina<>(items,pagina,total);
    }

    public Pagina<Venta> ventas(String usuario,Long elegida,int pagina) {
        long id=tienda(usuario,elegida); validarPagina(pagina);
        long total=jdbc.queryForObject("SELECT count(*) FROM venta_motocicleta WHERE id_tienda=?",Long.class,id);
        var items=jdbc.query("SELECT v.id_venta,m.chasis_completo,v.referencia_pago,v.referencia_comprobante,v.importe," +
            "u.nombre_usuario,v.vendida_en FROM venta_motocicleta v " +
            "JOIN motocicleta m ON m.id_motocicleta=v.id_motocicleta " +
            "JOIN usuario u ON u.id_usuario=v.id_registrador WHERE v.id_tienda=? " +
            "ORDER BY v.vendida_en DESC,v.id_venta DESC LIMIT 10 OFFSET ?",
            (rs,n)->new Venta(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),
                rs.getString(6),rs.getObject(7,OffsetDateTime.class)),id,(long)pagina*10);
        return new Pagina<>(items,pagina,total);
    }

    @Transactional
    public void registrar(String usuario,Long elegida,long motoId,String referenciaPago,String referencia,BigDecimal importe) {
        registrar(usuario,elegida,motoId,referenciaPago,referencia,importe,null);
    }

    @Transactional
    public void registrar(String usuario,Long elegida,long motoId,String referenciaPago,String referencia,BigDecimal importe,Long pagoPermitido) {
        long id=tienda(usuario,elegida);
        if(motoId<=0 || referenciaPago==null || referenciaPago.isBlank() || referenciaPago.trim().length()>100 ||
            referencia==null || referencia.isBlank() || referencia.trim().length()>100 ||
            importe==null || importe.signum()<=0 || importe.scale()>2 || importe.precision()>12)
            throw error(HttpStatus.BAD_REQUEST,"Revisa la moto, confirmación de pago, comprobante e importe");
        if(jdbc.query("SELECT id_motocicleta FROM motocicleta WHERE id_motocicleta=? FOR UPDATE",
            (rs,n)->rs.getLong(1),motoId).isEmpty()) throw error(HttpStatus.NOT_FOUND,"Moto no encontrada");
        var pendientes=jdbc.query("SELECT id_pago FROM pago_venta WHERE id_motocicleta=? AND estado='PENDIENTE'",(rs,n)->rs.getLong(1),motoId);
        if(pendientes.stream().anyMatch(x->!x.equals(pagoPermitido)))throw error(HttpStatus.CONFLICT,"Hay un pago pendiente de revisión para esta moto");
        // HU-22: una moto con datos en revisión no se ofrece ni se vende hasta resolver la diferencia.
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM diferencia_datos " +
            "WHERE id_motocicleta=? AND estado='PENDIENTE')",Boolean.class,motoId)))
            throw error(HttpStatus.CONFLICT,"La moto tiene una diferencia de datos en revisión");
        boolean disponible=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " +
            "motocicleta_ubicacion_actual a WHERE a.id_motocicleta=? AND a.id_ubicacion=? " +
            "AND NOT EXISTS(SELECT 1 FROM venta_motocicleta v WHERE v.id_motocicleta=a.id_motocicleta) " +
            "AND NOT EXISTS(SELECT 1 FROM envio_solicitud_motocicleta e WHERE e.id_motocicleta=a.id_motocicleta AND e.recibido_en IS NULL) " +
            "AND coalesce((SELECT d.estado FROM disponibilidad_evento d WHERE d.id_motocicleta=a.id_motocicleta " +
            "ORDER BY d.id_evento DESC LIMIT 1),'HABILITADA') <> 'BLOQUEADA' " +
            "AND NOT EXISTS(SELECT 1 FROM diferencia_datos f WHERE f.id_motocicleta=a.id_motocicleta AND f.estado='PENDIENTE'))",
            Boolean.class,motoId,id));
        if(!disponible) throw error(HttpStatus.CONFLICT,"Moto vendida, en tránsito o fuera de esta tienda");
        try { jdbc.update("INSERT INTO venta_motocicleta(id_motocicleta,id_tienda,id_registrador,referencia_pago,referencia_comprobante,importe) " +
            "SELECT ?,?,id_usuario,?,?,? FROM usuario WHERE nombre_usuario=? AND estado='ACTIVO'",
            motoId,id,referenciaPago.trim(),referencia.trim(),importe,usuario); }
        catch(DuplicateKeyException ex) { throw error(HttpStatus.CONFLICT,"Moto o comprobante ya registrado"); }
    }

    private void validarPagina(int pagina) {
        if(pagina<0) throw error(HttpStatus.BAD_REQUEST,"Página no válida");
    }
    private ResponseStatusException error(HttpStatus status,String mensaje) {
        return new ResponseStatusException(status,mensaje);
    }
}
