package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PermisoServiceTest {
    private final Memoria repository = new Memoria();
    private final PermisoService service = new PermisoService(repository);

    @Test
    void rolVigenteYExcepcionesSeAplicanEnCadaConsulta() {
        assertTrue(service.permitido("miguel", "FICHAS_REGISTRAR"));
        assertFalse(service.permitido("miguel", "LLEGADAS_REGISTRAR"));

        service.guardarExcepcion(2, "LLEGADAS_REGISTRAR", "PERMITIR");
        assertTrue(service.permitido("miguel", "LLEGADAS_REGISTRAR"));

        service.guardarExcepcion(2, "FICHAS_REGISTRAR", "DENEGAR");
        assertFalse(service.permitido("miguel", "FICHAS_REGISTRAR"));

        service.cambiarRol(2, "ALMACEN");
        assertFalse(service.permitido("miguel", "FICHAS_REGISTRAR"));
        assertTrue(service.permitido("miguel", "LLEGADAS_REGISTRAR"));

        service.retirarExcepcion(2, "FICHAS_REGISTRAR");
        assertFalse(service.permitido("miguel", "FICHAS_REGISTRAR"));
        service.retirarExcepcion(2, "LLEGADAS_REGISTRAR");
        assertTrue(service.permitido("miguel", "LLEGADAS_REGISTRAR"));
    }

    @Test
    void denegarPrevaleceSobreRolYCuentaInactivaNuncaTieneAcceso() {
        assertTrue(service.permitido("admin", "USUARIOS_CREAR"));
        service.guardarExcepcion(1, "USUARIOS_CREAR", "DENEGAR");
        assertFalse(service.permitido("admin", "USUARIOS_CREAR"));
        repository.activo = false;
        assertFalse(service.permitido("miguel", "FICHAS_REGISTRAR"));
    }

    @Test
    void soloGerenciaGestionaCatalogoAunqueSeIntenteAsignarExcepcion() {
        assertTrue(service.permitido("gerente", "CATALOGO_GESTIONAR"));
        assertFalse(service.permitido("miguel", "CATALOGO_GESTIONAR"));
        assertThrows(ResponseStatusException.class,
            () -> service.guardarExcepcion(2, "CATALOGO_GESTIONAR", "PERMITIR"));
        assertThrows(ResponseStatusException.class,
            () -> service.guardarExcepcion(1, "CATALOGO_GESTIONAR", "PERMITIR"));
    }

    @Test
    void administracionNoOperaElNegocioAunqueSuRolLoTuvieraAsignado() {
        // El rol ADMIN de la memoria aún conserva CATALOGO_GESTIONAR, como antes de V35.
        assertFalse(service.permitido("admin", "CATALOGO_GESTIONAR"));
        assertTrue(service.permitido("admin", "USUARIOS_CREAR"));
        assertFalse(service.permitido("gerente", "USUARIOS_CREAR"));
        assertThrows(ResponseStatusException.class,
            () -> service.guardarExcepcion(3, "PERMISOS_ASIGNAR", "PERMITIR"));
    }

    @Test
    void privilegiosAdministrativosNoSeConcedenAUnRolOperativo() {
        assertThrows(ResponseStatusException.class,
            () -> service.guardarExcepcion(2, "USUARIOS_CREAR", "PERMITIR"));
        assertThrows(ResponseStatusException.class,
            () -> service.guardarExcepcion(2, "PERMISOS_ASIGNAR", "PERMITIR"));
        assertThrows(ResponseStatusException.class,
            () -> service.cambiarRol(2, "INEXISTENTE"));
    }

    private static class Memoria implements PermisoRepository {
        private final Map<Long, String> roles = new HashMap<>(Map.of(1L, "ADMIN", 2L, "INVENTARIOS", 3L, "GERENTE"));
        private final Map<String, String> excepciones = new HashMap<>();
        private boolean activo = true;
        private final List<String> codigos = List.of("USUARIOS_CREAR", "PERMISOS_ASIGNAR",
            "LLEGADAS_REGISTRAR", "FICHAS_REGISTRAR", "INVENTARIO_CONSULTAR", "CATALOGO_GESTIONAR");

        @Override public Cuenta buscarPorNombre(String nombre) {
            return switch (nombre) {
                case "admin" -> buscarPorId(1);
                case "miguel" -> buscarPorId(2);
                case "gerente" -> buscarPorId(3);
                default -> null;
            };
        }
        @Override public Cuenta buscarPorId(long id) {
            if (!roles.containsKey(id)) return null;
            return new Cuenta(id, id == 1 ? "admin" : id == 2 ? "miguel" : "gerente", roles.get(id),
                activo ? "ACTIVO" : "INACTIVO", "ACTIVO");
        }
        @Override public boolean existeRolActivo(String codigo) {
            return List.of("ADMIN", "ALMACEN", "INVENTARIOS", "GERENTE").contains(codigo);
        }
        @Override public boolean existePermiso(String codigo) { return codigos.contains(codigo); }
        @Override public List<Asignacion> asignaciones(long usuarioId) {
            return codigos.stream().map(c -> new Asignacion(c,
                switch (roles.get(usuarioId)) {
                    case "ADMIN" -> List.of("USUARIOS_CREAR", "PERMISOS_ASIGNAR", "INVENTARIO_CONSULTAR", "CATALOGO_GESTIONAR").contains(c);
                    case "ALMACEN" -> List.of("LLEGADAS_REGISTRAR", "INVENTARIO_CONSULTAR").contains(c);
                    case "GERENTE" -> List.of("INVENTARIO_CONSULTAR", "CATALOGO_GESTIONAR").contains(c);
                    default -> List.of("FICHAS_REGISTRAR", "INVENTARIO_CONSULTAR").contains(c);
                }, excepciones.get(usuarioId + ":" + c))).toList();
        }
        @Override public void cambiarRol(long id, String codigo) { roles.put(id, codigo); }
        @Override public void guardarExcepcion(long id, String codigo, String decision) {
            excepciones.put(id + ":" + codigo, decision);
        }
        @Override public void retirarExcepcion(long id, String codigo) {
            excepciones.remove(id + ":" + codigo);
        }
    }
}
