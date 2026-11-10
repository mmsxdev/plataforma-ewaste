-- Dev 3 — Davi (roteamento).
-- Registra que o roteamento de uma solicitação já foi feito, mesmo quando nenhuma coletora estava
-- no raio (zero linhas em alerta_solicitacao). Assim um SolicitacaoCriadaEvent repetido não alerta
-- nem notifica de novo. A chave primária garante uma linha por solicitação.
CREATE TABLE roteamento_solicitacao (
    solicitacao_id BIGINT      PRIMARY KEY REFERENCES solicitacao (id),
    processado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);
