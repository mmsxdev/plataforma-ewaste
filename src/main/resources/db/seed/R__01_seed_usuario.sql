-- Dados de exemplo (perfil dev). Dono: Dev 1 — Miguel.
-- Todos os usuários abaixo têm a senha: senha123
-- coletora2 fica a ~360 km da geradora, de propósito: serve para testar o raio de atendimento.

INSERT INTO empresa (id, tipo, razao_social, cnpj, telefone, endereco, cidade, uf, latitude, longitude, raio_atendimento_km)
VALUES (1, 'GERADORA', 'Geradora Exemplo Ltda', '11222333000181', '11999990001', 'Av. Paulista, 1000', 'São Paulo', 'SP', -23.5614, -46.6559, NULL),
       (2, 'COLETORA', 'Recicla Centro Coletas Ltda', '22333444000181', '11999990002', 'Rua da Mooca, 500', 'São Paulo', 'SP', -23.5505, -46.6000, 50),
       (3, 'COLETORA', 'EcoColeta Rio Ltda', '33444555000181', '21999990003', 'Av. Brasil, 2000', 'Rio de Janeiro', 'RJ', -22.8800, -43.2500, 40)
ON CONFLICT (id) DO NOTHING;

INSERT INTO usuario (id, empresa_id, nome, email, senha_hash, perfil)
VALUES (1, NULL, 'Admin da Plataforma', 'admin@teste.com', '$2a$10$E7IKAPT6ueBRTLZtSfzPa.JGZchN/P9uJtxB7GA.RhMo813xRO2ve', 'ADMIN'),
       (2, 1, 'Gabriela Geradora', 'geradora@teste.com', '$2a$10$E7IKAPT6ueBRTLZtSfzPa.JGZchN/P9uJtxB7GA.RhMo813xRO2ve', 'GERADORA'),
       (3, 2, 'Carlos Coletora', 'coletora@teste.com', '$2a$10$E7IKAPT6ueBRTLZtSfzPa.JGZchN/P9uJtxB7GA.RhMo813xRO2ve', 'COLETORA'),
       (4, 3, 'Rita Coletora Rio', 'coletora2@teste.com', '$2a$10$E7IKAPT6ueBRTLZtSfzPa.JGZchN/P9uJtxB7GA.RhMo813xRO2ve', 'COLETORA')
ON CONFLICT (id) DO NOTHING;

-- Ajusta as sequências para que os próximos cadastros não colidam com os ids acima
SELECT setval(pg_get_serial_sequence('empresa', 'id'), GREATEST((SELECT max(id) FROM empresa), 1));
SELECT setval(pg_get_serial_sequence('usuario', 'id'), GREATEST((SELECT max(id) FROM usuario), 1));
