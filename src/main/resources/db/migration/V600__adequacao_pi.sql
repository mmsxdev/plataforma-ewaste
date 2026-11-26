-- Ampliação compatível: os registros anteriores permanecem válidos, sem valores inventados.
ALTER TABLE solicitacao_item ADD COLUMN identificacao VARCHAR(150);
ALTER TABLE solicitacao ADD COLUMN ponto_descarte_id BIGINT REFERENCES ponto_descarte(id);
CREATE INDEX ix_solicitacao_ponto ON solicitacao(ponto_descarte_id);
