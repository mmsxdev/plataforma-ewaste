-- Dados de exemplo (perfil dev). Dono: Dev 3 — Davi.
-- Empresas pelo CNPJ dos seeds R__01 (Miguel) e R__02 (Felipe), nunca por id fixo:
--   11222333000181 = Geradora Exemplo (Av. Paulista)  22333444000181 = Recicla Centro (raio 50 km)
-- A solicitação não tem chave natural, então a descrição identifica cada uma (idempotente).
-- Inseridas por SQL, o roteamento (que reage ao evento de criação) NÃO roda: os alertas vão aqui.
--
-- Roteiro manual: a de 250 kg com 5 HDs está ABERTA e alertada para a Recicla Centro
-- (coletora@teste.com), pronta para o aceite. A de Campinas fica fora do raio de todas as
-- coletoras, para mostrar o aviso "Nenhuma coletora apta na sua região".

WITH geradora AS (SELECT id FROM empresa WHERE cnpj = '11222333000181'),
     coletora AS (SELECT id FROM empresa WHERE cnpj = '22333444000181'),
     dados (descricao, peso, contem_midias, qtd_midias, endereco, cidade, uf, latitude, longitude,
            status, da_coletora, aceita_ha, agendada_daqui) AS (
       VALUES ('Computadores e 5 HDs do antigo datacenter', 250.00, TRUE, 5, 'Av. Paulista, 1500',
               'São Paulo', 'SP', -23.5629, -46.6544, 'ABERTA', FALSE, NULL::interval, NULL::interval),
              ('Monitores e impressoras do escritório', 120.00, FALSE, 0, 'Rua Augusta, 900',
               'São Paulo', 'SP', -23.5530, -46.6560, 'ACEITA', TRUE, interval '1 day', NULL),
              ('Notebooks e celulares corporativos', 80.00, TRUE, 12, 'Rua Vergueiro, 2000',
               'São Paulo', 'SP', -23.5780, -46.6400, 'AGENDADA', TRUE, interval '2 days', interval '2 days'),
              ('Baterias de nobreak da filial de Campinas', 300.00, FALSE, 0, 'Av. Norte-Sul, 500',
               'Campinas', 'SP', -22.9056, -47.0608, 'ABERTA', FALSE, NULL, NULL))
INSERT INTO solicitacao (geradora_id, descricao, peso_estimado_kg, contem_midias, qtd_midias, endereco,
                         cidade, uf, latitude, longitude, status, coletora_id, aceita_em, agendada_para,
                         criada_em)
SELECT g.id, d.descricao, d.peso, d.contem_midias, d.qtd_midias, d.endereco, d.cidade, d.uf,
       d.latitude, d.longitude, d.status,
       CASE WHEN d.da_coletora THEN c.id END,
       now() - d.aceita_ha,
       now() + d.agendada_daqui,
       now() - interval '3 days'
FROM dados d
CROSS JOIN geradora g
CROSS JOIN coletora c
WHERE NOT EXISTS (SELECT 1 FROM solicitacao s WHERE s.geradora_id = g.id AND s.descricao = d.descricao);

-- Itens (só para as solicitações do seed que ainda não têm itens)
INSERT INTO solicitacao_item (solicitacao_id, tipo_residuo, quantidade, peso_kg)
SELECT s.id, i.tipo_residuo, i.quantidade, i.peso_kg
FROM (VALUES ('Computadores e 5 HDs do antigo datacenter', 'MIDIA_ARMAZENAMENTO', 5, 5.00),
             ('Computadores e 5 HDs do antigo datacenter', 'INFORMATICA', 20, 245.00),
             ('Monitores e impressoras do escritório', 'INFORMATICA', 15, 120.00),
             ('Notebooks e celulares corporativos', 'INFORMATICA', 10, 25.00),
             ('Notebooks e celulares corporativos', 'TELEFONIA', 40, 8.00),
             ('Notebooks e celulares corporativos', 'MIDIA_ARMAZENAMENTO', 12, 1.20),
             ('Baterias de nobreak da filial de Campinas', 'BATERIA', 30, 300.00))
         AS i (descricao, tipo_residuo, quantidade, peso_kg)
JOIN solicitacao s ON s.descricao = i.descricao
JOIN empresa g ON g.id = s.geradora_id AND g.cnpj = '11222333000181'
WHERE NOT EXISTS (SELECT 1 FROM solicitacao_item x WHERE x.solicitacao_id = s.id);

-- Alertas da Recicla Centro para as 3 solicitações de São Paulo (dentro do raio de 50 km).
-- Distância calculada com a mesma fórmula de Haversine do sistema (raio da Terra 6371 km).
INSERT INTO alerta_solicitacao (solicitacao_id, coletora_id, distancia_km, enviado_em)
SELECT s.id, c.id,
       round((2 * 6371 * asin(sqrt(
             power(sin(radians(c.latitude - s.latitude) / 2), 2)
           + cos(radians(s.latitude)) * cos(radians(c.latitude))
           * power(sin(radians(c.longitude - s.longitude) / 2), 2))))::numeric, 2),
       s.criada_em
FROM solicitacao s
JOIN empresa g ON g.id = s.geradora_id AND g.cnpj = '11222333000181'
JOIN empresa c ON c.cnpj = '22333444000181'
WHERE s.descricao IN ('Computadores e 5 HDs do antigo datacenter',
                      'Monitores e impressoras do escritório',
                      'Notebooks e celulares corporativos')
ON CONFLICT (solicitacao_id, coletora_id) DO NOTHING;
