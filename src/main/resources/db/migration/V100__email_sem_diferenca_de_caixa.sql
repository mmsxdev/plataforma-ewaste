-- Protege também cadastros simultâneos e usuários cadastrados fora da aplicação.
CREATE UNIQUE INDEX usuario_email_minusculo_uq ON usuario (lower(email));
