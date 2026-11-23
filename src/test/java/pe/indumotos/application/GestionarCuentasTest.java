package pe.indumotos.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GestionarCuentasTest {
    private final Memoria repositorio = new Memoria();
    private final PermisoService permisos = mock(PermisoService.class);
    private final GestionarCuentas servicio = new GestionarCuentas(repositorio, permisos);

    @BeforeEach
    void datos() {
        repositorio.agregar(1, "admin", "ADMIN", "ACTIVO");
        repositorio.agregar(2, "miguel", "INVENTARIOS", "ACTIVO");
        when(permisos.consultar(anyLong())).thenAnswer(i -> new PermisoService.Accesos(i.getArgument(0),
            "x", "INVENTARIOS", List.of(
                new PermisoService.PermisoEfectivo("FICHAS_REGISTRAR", true, null, true),
                new PermisoService.PermisoEfectivo("LLEGADAS_REGISTRAR", false, "PERMITIR", true),
                new PermisoService.PermisoEfectivo("FICHAS_EDITAR", true, "DENEGAR", false))));
    }

    @Test
    void consultaMuestraEstadoRolPermisosEfectivosYExcepciones() {
        var detalle = servicio.consultar(2);
        assertEquals("ACTIVO", detalle.estado());
        assertEquals("INVENTARIOS", detalle.rolCodigo());
        assertEquals(List.of("FICHAS_REGISTRAR", "LLEGADAS_REGISTRAR"), detalle.permisosEfectivos());
        assertEquals(2, detalle.excepciones().size());
        assertNull(detalle.ultimoCambioEstado());
    }

    @Test
    void cuentaInexistenteDevuelve404() {
        var error = assertThrows(ResponseStatusException.class, () -> servicio.consultar(99));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
            assertThrows(ResponseStatusException.class, () -> servicio.historial(99)).getStatusCode());
    }

    @Test
    void desactivarYReactivarConservaLaCuentaYRegistraElHistorial() {
        var inactiva = servicio.cambiarEstado(2, "inactivo", "  Dejó la empresa  ", "admin");
        assertEquals("INACTIVO", inactiva.estado());
        assertEquals("Dejó la empresa", inactiva.ultimoCambioEstado().motivo());
        assertEquals("admin", inactiva.ultimoCambioEstado().actor());
        assertEquals(1, repositorio.sesionesCerradas.get(2L));

        var activa = servicio.cambiarEstado(2, "ACTIVO", "Regresó a la empresa", "admin");
        assertEquals("ACTIVO", activa.estado());
        var historial = servicio.historial(2);
        assertEquals(2, historial.size());
        assertEquals("ACTIVO", historial.getFirst().estadoNuevo());
        assertEquals("INACTIVO", historial.get(1).estadoNuevo());
        assertEquals(2, repositorio.sesionesCerradas.get(2L));
    }

    @Test
    void noSePuedeDesactivarLaPropiaCuenta() {
        repositorio.agregar(3, "admin2", "ADMIN", "ACTIVO");
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(1, "INACTIVO", "Prueba", "admin"));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("propia"));
    }

    @Test
    void debeQuedarAlMenosUnAdministradorActivo() {
        repositorio.agregar(3, "admin2", "ADMIN", "INACTIVO");
        repositorio.agregar(4, "soporte", "ADMIN", "ACTIVO");
        // Con dos administradores activos sí se puede desactivar uno.
        assertEquals("INACTIVO", servicio.cambiarEstado(4, "INACTIVO", "Rotación", "admin").estado());

        // Queda solo "admin": otra cuenta con permiso delegado no puede dejar el sistema sin administración.
        repositorio.agregar(5, "jefe", "INVENTARIOS", "ACTIVO");
        var ultimo = assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(1, "INACTIVO", "Rotación", "jefe"));
        assertEquals(HttpStatus.CONFLICT, ultimo.getStatusCode());
        assertEquals("Debe quedar al menos un administrador activo", ultimo.getReason());
        assertEquals("ACTIVO", repositorio.buscar(1).estado());
    }

    @Test
    void rechazaMismoEstadoMotivoVacioEstadoInvalidoYActorInactivo() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(2, "ACTIVO", "Sin cambio", "admin")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(2, "INACTIVO", "   ", "admin")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(2, "BORRADO", "Motivo", "admin")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(2, "INACTIVO", "x".repeat(501), "admin")).getStatusCode());
        repositorio.cuentas.put(1L, cuenta(1, "admin", "ADMIN", "INACTIVO"));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> servicio.cambiarEstado(2, "INACTIVO", "Motivo", "admin")).getStatusCode());
        assertTrue(repositorio.eventos.isEmpty());
    }

    private static CuentaRepository.Cuenta cuenta(long id, String usuario, String rol, String estado) {
        return new CuentaRepository.Cuenta(id, "Nombre " + usuario, usuario, rol, rol, estado, null,
            OffsetDateTime.now(), OffsetDateTime.now());
    }

    private static final class Memoria implements CuentaRepository {
        private record Evento(long cuenta, EventoEstado datos) {}
        final Map<Long, Cuenta> cuentas = new LinkedHashMap<>();
        final List<Evento> eventos = new ArrayList<>();
        final Map<Long, Integer> sesionesCerradas = new LinkedHashMap<>();

        void agregar(long id, String usuario, String rol, String estado) { cuentas.put(id, cuenta(id, usuario, rol, estado)); }

        @Override public Cuenta buscar(long id) { return cuentas.get(id); }
        @Override public Cuenta bloquear(long id) { return cuentas.get(id); }

        @Override public Long idActivoPorNombre(String nombre) {
            return cuentas.values().stream().filter(c -> c.nombreUsuario().equals(nombre) && c.estado().equals("ACTIVO"))
                .map(Cuenta::id).findFirst().orElse(null);
        }

        @Override public long administradoresActivos() {
            return cuentas.values().stream().filter(c -> c.rolCodigo().equals("ADMIN") && c.estado().equals("ACTIVO")).count();
        }

        @Override public void cambiarEstado(long id, String estado) {
            var c = cuentas.get(id);
            cuentas.put(id, new Cuenta(c.id(), c.nombreCompleto(), c.nombreUsuario(), c.rolCodigo(), c.rolNombre(),
                estado, c.tiendaNombre(), c.creadoEn(), OffsetDateTime.now()));
            sesionesCerradas.merge(id, 1, Integer::sum);
        }

        @Override public void registrarEvento(long id, String anterior, String nuevo, String motivo, long actorId) {
            eventos.add(new Evento(id, new EventoEstado(anterior, nuevo, motivo,
                cuentas.get(actorId).nombreUsuario(), OffsetDateTime.now())));
        }

        @Override public List<EventoEstado> historial(long id) {
            return eventos.reversed().stream().filter(e -> e.cuenta() == id).map(Evento::datos).toList();
        }
    }
}
