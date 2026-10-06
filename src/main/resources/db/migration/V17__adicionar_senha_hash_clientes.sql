ALTER TABLE clientes
ADD COLUMN IF NOT EXISTS senha_hash VARCHAR(255);

UPDATE clientes
SET senha_hash = '$2b$10$31cLVHKKdhoECOAyXXrlHeCu2t3hFrV4tGXCPSCFEeRkM.EXv5gky'
WHERE regexp_replace(documento, '\D', '', 'g') IN (
    '52398614808',
    '31856742008',
    '67493821003',
    '45102983075',
    '78932145628',
    '92456178076'
);
