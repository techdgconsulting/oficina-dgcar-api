ALTER TABLE ordens_servico ADD COLUMN data_entrada_status TIMESTAMP;

-- OS antigas nao possuem historico confiavel de entrada no status atual.
-- A primeira transicao passa a registrar o instante, sem inventar duracoes.
