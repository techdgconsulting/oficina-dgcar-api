ALTER TABLE clientes
ADD COLUMN IF NOT EXISTS status_cliente VARCHAR(20) NOT NULL DEFAULT 'ATIVO';

UPDATE clientes
SET status_cliente = 'ATIVO'
WHERE status_cliente IS NULL OR TRIM(status_cliente) = '';
