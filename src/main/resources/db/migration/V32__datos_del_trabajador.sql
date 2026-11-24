ALTER TABLE usuario ADD COLUMN documento VARCHAR(20), ADD COLUMN telefono VARCHAR(30);
CREATE UNIQUE INDEX ux_usuario_documento ON usuario (upper(documento)) WHERE documento IS NOT NULL;
