-- Evita que un error operativo borre o reescriba el historial.
-- Una correccion debe registrarse mediante un nuevo evento.

CREATE FUNCTION impedir_edicion_historial_logistico() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Historial inmutable: registre un nuevo evento'
        USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER trg_movimiento_historial_inmutable
BEFORE UPDATE OR DELETE ON movimiento_motocicleta
FOR EACH ROW EXECUTE FUNCTION impedir_edicion_historial_logistico();

CREATE TRIGGER trg_solicitud_evento_inmutable
BEFORE UPDATE OR DELETE ON solicitud_ensamblaje_evento
FOR EACH ROW EXECUTE FUNCTION impedir_edicion_historial_logistico();

