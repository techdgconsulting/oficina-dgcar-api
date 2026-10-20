-- Completa somente os cenarios ficticios da V6, identificados por placa e data.
-- Nao atribui mecanicos a ordens reais ou a outras massas geradas pela API.
UPDATE execucoes e
SET mecanico_nome = demo.mecanico
FROM ordens_servico os
JOIN veiculos v ON v.id = os.veiculo_id
JOIN (VALUES
    ('BRA2E19', TIMESTAMP '2026-05-01 08:30:00', 'Carlos Silva'),
    ('RIO4F56', TIMESTAMP '2026-04-20 07:00:00', 'Rafael Costa'),
    ('SPO3A77', TIMESTAMP '2026-05-18 08:00:00', 'Marcos Santos'),
    ('MGA5B23', TIMESTAMP '2026-05-17 14:00:00', 'Carlos Silva'),
    ('GRU7C88', TIMESTAMP '2026-05-16 09:00:00', 'Rafael Costa'),
    ('ABC1D23', TIMESTAMP '2026-05-18 10:00:00', 'Marcos Santos'),
    ('XYZ9H01', TIMESTAMP '2026-05-17 16:00:00', 'Carlos Silva'),
    ('NPR8J45', TIMESTAMP '2026-05-15 11:00:00', 'Rafael Costa')
) AS demo(placa, criacao, mecanico)
    ON demo.placa = v.placa AND demo.criacao = os.data_criacao
WHERE e.ordem_servico_id = os.id
  AND (e.mecanico_nome IS NULL OR TRIM(e.mecanico_nome) = '');
