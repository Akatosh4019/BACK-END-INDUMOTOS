-- Ejecutar despues de aplicar Flyway V1-V9:
-- psql -v ON_ERROR_STOP=1 -f verificar_logistica_dam.sql
-- Todo se revierte al final; no conserva datos ficticios.

BEGIN;

CREATE TEMP TABLE datos_previos ON COMMIT DROP AS
SELECT 'usuario' AS tabla, to_jsonb(t) AS fila FROM usuario t
UNION ALL SELECT 'llegada', to_jsonb(t) FROM llegada t
UNION ALL SELECT 'motocicleta', to_jsonb(t) FROM motocicleta t;

DO $$
DECLARE
    usuario_prueba BIGINT;
    llegada_prueba BIGINT;
    moto_prueba BIGINT;
    almacen_prueba BIGINT;
    tienda_prueba BIGINT;
    movimiento_prueba BIGINT;
    solicitud_prueba BIGINT;
    detalle_prueba BIGINT;
BEGIN
    INSERT INTO usuario(id_rol, nombre_completo, nombre_usuario, clave_hash)
    SELECT id_rol, 'Prueba SQL DAM', 'sql_dam_' || txid_current(), 'NO_ES_CREDENCIAL'
    FROM rol WHERE codigo = 'ADMIN'
    RETURNING id_usuario INTO STRICT usuario_prueba;

    INSERT INTO llegada(numero_guia, id_receptor, cantidad_declarada, cantidad_recibida)
    VALUES ('PRUEBA-DAM', usuario_prueba, 1, 1)
    RETURNING id_llegada INTO llegada_prueba;

    INSERT INTO motocicleta(
        id_llegada, marca, modelo, color, chasis_completo,
        numero_motor, dua, item_dua, id_creador
    ) VALUES (
        llegada_prueba, 'PRUEBA', 'MODELO', 'AZUL', 'SQL-DAM-' || txid_current(),
        'MOTOR-1', 'DUA-1', 'ITEM-1', usuario_prueba
    ) RETURNING id_motocicleta INTO moto_prueba;

    INSERT INTO ubicacion(codigo, nombre, tipo)
    VALUES ('ALM-' || txid_current(), 'Almacen de prueba', 'ALMACEN')
    RETURNING id_ubicacion INTO almacen_prueba;

    INSERT INTO ubicacion(codigo, nombre, tipo)
    VALUES ('TDA-' || txid_current(), 'Tienda de prueba', 'TIENDA')
    RETURNING id_ubicacion INTO tienda_prueba;

    IF EXISTS (
        SELECT 1 FROM motocicleta_ubicacion_actual
        WHERE id_motocicleta = moto_prueba
    ) THEN
        RAISE EXCEPTION 'Una moto nueva no debe recibir una ubicacion ficticia';
    END IF;

    BEGIN
        INSERT INTO ubicacion(codigo, nombre, tipo)
        VALUES (lower('ALM-' || txid_current()), 'Codigo repetido', 'ALMACEN');
        RAISE EXCEPTION 'Se acepto un codigo de ubicacion repetido';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;

    INSERT INTO movimiento_motocicleta(
        id_motocicleta, tipo, id_destino, id_registrador, ocurrido_en
    ) VALUES (
        moto_prueba, 'UBICACION', almacen_prueba, usuario_prueba,
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    );

    INSERT INTO movimiento_motocicleta(
        id_motocicleta, tipo, id_origen, id_destino, id_registrador, ocurrido_en
    ) VALUES (
        moto_prueba, 'TRASLADO', almacen_prueba, tienda_prueba,
        usuario_prueba, CURRENT_TIMESTAMP
    ) RETURNING id_movimiento INTO movimiento_prueba;

    IF NOT EXISTS (
        SELECT 1 FROM motocicleta_ubicacion_actual
        WHERE id_motocicleta = moto_prueba
          AND id_ubicacion = tienda_prueba
          AND id_movimiento = movimiento_prueba
    ) THEN
        RAISE EXCEPTION 'La vista no refleja el ultimo traslado';
    END IF;

    BEGIN
        UPDATE movimiento_motocicleta
        SET observacion = 'Intento de reescritura'
        WHERE id_movimiento = movimiento_prueba;
        RAISE EXCEPTION 'Se permitio modificar el historial';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        INSERT INTO movimiento_motocicleta(
            id_motocicleta, tipo, id_origen, id_destino, id_registrador, ocurrido_en
        ) VALUES (
            moto_prueba, 'TRASLADO', almacen_prueba, almacen_prueba,
            usuario_prueba, CURRENT_TIMESTAMP
        );
        RAISE EXCEPTION 'Se acepto un traslado hacia el mismo origen';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    INSERT INTO solicitud_ensamblaje(id_solicitante, id_destino, observacion)
    VALUES (usuario_prueba, tienda_prueba, 'Solicitud de prueba')
    RETURNING id_solicitud INTO solicitud_prueba;

    INSERT INTO solicitud_ensamblaje_detalle(
        id_solicitud, marca, modelo, color, cantidad
    ) VALUES (
        solicitud_prueba, 'PRUEBA', 'MODELO', 'AZUL', 1
    ) RETURNING id_detalle INTO detalle_prueba;

    BEGIN
        INSERT INTO solicitud_ensamblaje_detalle(
            id_solicitud, marca, modelo, color, cantidad
        ) VALUES (
            solicitud_prueba, 'PRUEBA', 'MODELO', 'AZUL', 0
        );
        RAISE EXCEPTION 'Se acepto una cantidad igual a cero';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    INSERT INTO solicitud_ensamblaje_evento(
        id_solicitud, estado, id_registrador
    ) VALUES (
        solicitud_prueba, 'PENDIENTE', usuario_prueba
    );

    BEGIN
        INSERT INTO solicitud_ensamblaje_evento(
            id_solicitud, estado, id_registrador, motivo
        ) VALUES (
            solicitud_prueba, 'RECHAZADA', usuario_prueba, '   '
        );
        RAISE EXCEPTION 'Se acepto un rechazo sin motivo';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    BEGIN
        DELETE FROM solicitud_ensamblaje_evento
        WHERE id_solicitud = solicitud_prueba;
        RAISE EXCEPTION 'Se permitio borrar el historial de estados';
    EXCEPTION WHEN check_violation THEN
        NULL;
    END;

    INSERT INTO solicitud_ensamblaje_unidad(
        id_detalle, id_motocicleta, id_asignador
    ) VALUES (
        detalle_prueba, moto_prueba, usuario_prueba
    );

    BEGIN
        INSERT INTO solicitud_ensamblaje_unidad(
            id_detalle, id_motocicleta, id_asignador
        ) VALUES (
            detalle_prueba, moto_prueba, usuario_prueba
        );
        RAISE EXCEPTION 'Se acepto una asignacion activa duplicada';
    EXCEPTION WHEN unique_violation THEN
        NULL;
    END;

    RAISE NOTICE 'Base logistica DAM verificada correctamente';
END $$;

-- Las tablas del Sprint 1 deben conservar exactamente sus filas originales.
DO $$
BEGIN
    IF EXISTS (
        SELECT tabla, fila FROM datos_previos
        EXCEPT ALL
        (
            SELECT 'usuario', to_jsonb(t) FROM usuario t
            UNION ALL SELECT 'llegada', to_jsonb(t) FROM llegada t
            UNION ALL SELECT 'motocicleta', to_jsonb(t) FROM motocicleta t
        )
    ) THEN
        RAISE EXCEPTION 'Se modificaron datos preexistentes del Sprint 1';
    END IF;
END $$;

ROLLBACK;
