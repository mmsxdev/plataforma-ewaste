# Entrega Dev 1 - Miguel

Escopo: P1 da seção “Escopo para 2 semanas” do plano de projeto (página 12), com os critérios de cadastro e segurança das páginas 14 e 15. A base e os contratos já estavam presentes no repositório.

## Implementado

- Layout público, navbar por perfil, login e painéis GERADORA, COLETORA e ADMIN.
- Cadastro público apenas de geradora/coletora; o perfil é definido pela rota. Admin continua provisionado pelo seed de desenvolvimento.
- CNPJ numérico com dígitos verificadores, dados obrigatórios, e-mail válido, senha de 8 a 72 caracteres e até 72 bytes, limites de coordenadas, UF e raio positivo para coletoras.
- Normalização de CNPJ e e-mail, BCrypt e transação única para empresa/usuário. Publicação de `EmpresaCadastradaEvent` dentro da transação, para consumo após commit pelo Dev 2.
- Mensagens de duplicidade e índice de e-mail sem distinção de maiúsculas na nova migração V100. V1 preservada.
- Login com banco, contas inativas bloqueadas, destino `/painel`, logout por POST com CSRF e proteção dos prefixos por perfil.
- Implementações reais dos contratos `UsuarioAtual` e `EmpresaConsulta`, sem alteração dos DTOs e interfaces compartilhados.
- Seletor Leaflet reutilizável e entrada manual de coordenadas. O formulário não devolve a senha no HTML após erro.
- Testes de CNPJ, serviço, identidade, formulários, matriz de acesso, persistência, unicidade e autenticação real.

## Revisão manual

1. Use JDK 21, abra o Docker Desktop e execute `docker compose up -d`.
2. Execute `mvnw.cmd verify`. Os testes de integração usam um PostgreSQL separado via Testcontainers.
3. Inicie `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev` (no PowerShell, coloque o argumento `-D...` entre aspas).
4. Acesse `http://localhost:8080` sem sessão. Confira início, login e os dois cadastros em tela de computador e celular.
5. Cadastre uma geradora: CNPJ `04.252.011/0001-10`, razão social, telefone, endereço, cidade, UF, coordenadas, responsável, e-mail próprio e senha com pelo menos 8 caracteres. O CNPJ é apenas uma massa de teste; não representa vínculo com a empresa real.
6. Faça login com a nova conta. Confira `/painel`, atalhos de geradora e logout. Em `/coletora/homologacao` e `/admin/homologacoes`, o acesso deve ser 403.
7. Tente repetir CNPJ/e-mail; confira as mensagens. Tente CNPJ inválido, senha curta, coordenadas fora do intervalo e cadastro de coletora sem raio: nenhum deve gravar.
8. Cadastre a coletora com outro CNPJ válido, selecione a base no mapa (ou digite coordenadas), informe o raio e faça login. Confira os atalhos de homologação, oportunidades e coletas; em `/geradora/solicitacoes`, o acesso deve ser 403.
9. Entre como `admin@teste.com`, senha `senha123`, e confira o painel administrativo. Contas de exemplo só são criadas no perfil `dev`.
10. Inspecione a tabela `usuario`: `senha_hash` deve conter BCrypt, nunca a senha original. Após cadastrar uma coletora, confira a homologação PENDENTE criada pelo listener real do Dev 2.

## Integração com o time

Use `head(titulo)`, `navbar` e `rodape` de `fragments/layout.html` nos templates. O elemento `#aviso-notificacoes` fica reservado para o Dev 5. O script de localização precisa de `#mapa-localizacao`, `#latitude`, `#longitude` e pode usar `#localizacao-status`.

Os links dos painéis apontam para as rotas contratadas. A infraestrutura de armazenamento e a criação da homologação pendente do Dev 2 já estão integradas; as telas e demais fluxos dependem das entregas dos respectivos módulos. O teste CadastroIT valida o cadastro de coletora e a homologação pendente após commit.

Revisão por outra pessoa, CI remoto, integração na main e tags de versão são etapas de entrega da equipe. Integração diária e ensaio em grupo precisam acontecer nos respectivos marcos. O trabalho local não comprova essas etapas.

P2: “Minha empresa”, páginas de erro com acabamento adicional e Playwright, após v0.2 e com o fluxo integrado aprovado.
