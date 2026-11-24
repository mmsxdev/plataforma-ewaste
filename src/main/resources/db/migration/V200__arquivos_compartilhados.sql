-- Referências existentes *_ref permanecem estáveis. Importação de uploads legados é explícita.
CREATE TABLE arquivo_compartilhado (
    caminho VARCHAR(300) PRIMARY KEY,
    nome_original TEXT,
    content_type VARCHAR(100) NOT NULL CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    tamanho BIGINT NOT NULL CHECK (tamanho BETWEEN 1 AND 10485760),
    conteudo BYTEA NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_arquivo_tamanho CHECK (octet_length(conteudo) = tamanho)
);
