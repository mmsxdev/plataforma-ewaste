-- Dados de exemplo (perfil dev). Dono: Dev 2 — Felipe.
-- As duas coletoras do R__01 ficam APROVADAS e com licenças válidas, para o roteamento (Dev 3)
-- ter coletoras aptas: a Recicla Centro (id 2) dentro do raio da geradora e a EcoColeta Rio (id 3)
-- fora dele. Os arquivos citados em arquivo_ref são fictícios (não existem em ./uploads).

INSERT INTO homologacao (id, empresa_id, status, analisado_por, analisado_em)
VALUES (1, 2, 'APROVADA', 1, now()),
       (2, 3, 'APROVADA', 1, now())
ON CONFLICT (id) DO NOTHING;

-- Licença ambiental e cadastro IBAMA são os documentos obrigatórios para ficar apta
INSERT INTO documento_homologacao (id, homologacao_id, tipo, arquivo_ref, nome_original, validade)
VALUES (1, 1, 'LICENCA_AMBIENTAL', 'seed/licenca-recicla-centro.pdf', 'licenca.pdf', DATE '2030-12-31'),
       (2, 1, 'CADASTRO_IBAMA',    'seed/ibama-recicla-centro.pdf',   'ibama.pdf',   DATE '2030-12-31'),
       (3, 1, 'ISO_27001',         'seed/iso-recicla-centro.pdf',     'iso.pdf',     DATE '2030-12-31'),
       (4, 2, 'LICENCA_AMBIENTAL', 'seed/licenca-ecocoleta.pdf',      'licenca.pdf', DATE '2030-12-31'),
       (5, 2, 'CADASTRO_IBAMA',    'seed/ibama-ecocoleta.pdf',        'ibama.pdf',   NULL)
ON CONFLICT (id) DO NOTHING;

-- Ajusta as sequências para que as próximas homologações não colidam com os ids acima
SELECT setval(pg_get_serial_sequence('homologacao', 'id'), GREATEST((SELECT max(id) FROM homologacao), 1));
SELECT setval(pg_get_serial_sequence('documento_homologacao', 'id'), GREATEST((SELECT max(id) FROM documento_homologacao), 1));
