-- Las cuentas existentes conservan su nombre completo. No se inventan datos personales.
ALTER TABLE usuario
    ADD COLUMN nombres VARCHAR(80),
    ADD COLUMN apellidos VARCHAR(80),
    ADD COLUMN correo VARCHAR(254),
    ADD COLUMN telefono VARCHAR(9),
    ADD COLUMN dni VARCHAR(8),
    ADD CONSTRAINT ck_usuario_telefono CHECK (telefono IS NULL OR telefono ~ '^[0-9]{9}$'),
    ADD CONSTRAINT ck_usuario_dni CHECK (dni IS NULL OR dni ~ '^[0-9]{8}$');

CREATE UNIQUE INDEX uq_usuario_dni ON usuario(dni) WHERE dni IS NOT NULL;
