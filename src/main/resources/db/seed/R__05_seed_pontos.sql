-- Locais inteiramente fictícios; não representam serviços de coleta reais.
INSERT INTO ponto_descarte (nome,endereco,cidade,uf,latitude,longitude,tipos_aceitos,horario)
SELECT 'Ponto Escola Circular — fictício','Rua de Demonstração, 100','Goiânia','GO',-16.6869,-49.2648,
       'INFORMATICA,TELEFONIA,MIDIA_ARMAZENAMENTO','Demonstração: segunda a sexta, 9h–17h'
WHERE NOT EXISTS (SELECT 1 FROM ponto_descarte WHERE nome = 'Ponto Escola Circular — fictício');
INSERT INTO ponto_descarte (nome,endereco,cidade,uf,latitude,longitude,tipos_aceitos,horario)
SELECT 'Ponto Baterias Circular — fictício','Avenida de Exemplo, 200','Goiânia','GO',-16.68,-49.26,
       'BATERIA','Demonstração: segunda a sexta, 10h–16h'
WHERE NOT EXISTS (SELECT 1 FROM ponto_descarte WHERE nome = 'Ponto Baterias Circular — fictício');
