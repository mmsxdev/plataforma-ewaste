ALTER TABLE midia_destruida ADD COLUMN evidencia_ref VARCHAR(300);

CREATE UNIQUE INDEX ux_rastreio_evento_inicial
    ON evento_rastreio (solicitacao_id, tipo)
    WHERE tipo IN ('CRIADA', 'ACEITA');
