package pe.indumotos.infrastructure;
import pe.indumotos.application.CuentaSeguridadRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcCuentaSeguridadRepository implements CuentaSeguridadRepository {
    private final JdbcTemplate jdbc;
    public JdbcCuentaSeguridadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private Cuenta query(String where,Object value){return jdbc.query("SELECT u.id_usuario,u.nombre_usuario,u.clave_hash,r.codigo,u.estado FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE "+where,
        rs->rs.next()?new Cuenta(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)):null,value);}
    public Cuenta buscar(String usuario){return query("u.nombre_usuario=?",usuario);}
    public Cuenta bloquear(long id){return query("u.id_usuario=? FOR UPDATE OF u",id);}
    public void cambiarClave(long id,String hash){jdbc.update("UPDATE usuario SET clave_hash=? WHERE id_usuario=?",hash,id);}
}
