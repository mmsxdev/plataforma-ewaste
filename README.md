# Plataforma E-Waste

Plataforma B2B que conecta empresas geradoras de lixo eletrônico a coletoras homologadas da mesma região. A primeira coletora que aceitar assume a coleta. Depois que a coleta é registrada, a plataforma libera o CDF, o laudo técnico e o certificado de destruição de mídias. Também há um mapa público de pontos de descarte.

> Projeto acadêmico. Integrações externas (SINIR/IBAMA, pagamentos) são **simuladas**, e os documentos gerados levam a marca "documento simulado".

## Stack

Java 21 · Spring Boot 3.5 (Web, Thymeleaf, Security, Data JPA, Validation) · PostgreSQL 16 · Flyway · OpenPDF · Leaflet · JUnit 5, Testcontainers, ArchUnit · GitHub Actions

## Pré-requisitos

- Git
- JDK 21 (ex.: [Temurin](https://adoptium.net/))
- Docker Desktop (aberto e rodando)
- IntelliJ IDEA Community ou VS Code com Extension Pack for Java
- DBeaver (opcional, para consultar o banco)

Você **não** precisa instalar o Maven: use o `mvnw` do projeto.

## Como rodar

```bash
git clone https://github.com/NOME-DA-ORG/plataforma-ewaste.git
cd plataforma-ewaste
cp .env.example .env            # Windows (cmd): copy .env.example .env
docker compose up -d            # sobe o PostgreSQL
./mvnw verify                   # compila e roda todos os testes (Windows: mvnw.cmd verify)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Abra http://localhost:8080. A verificação de saúde fica em http://localhost:8080/actuator/health e deve responder `{"status":"UP"}`.

## Entrega Dev 1 (P1)

Cadastro em `/cadastro/geradora` e `/cadastro/coletora`, login em `/login` e painel por perfil em `/painel`. O cadastro valida CNPJ, e-mail, senha, coordenadas e raio da coletora; grava empresa e usuário na mesma transação, protege a senha com BCrypt e publica `EmpresaCadastradaEvent` para o módulo de homologação. CNPJ e e-mail duplicados são recusados, inclusive com restrição de unicidade no banco. E-mails são comparados sem distinção de maiúsculas.

`UsuarioAtual` e `EmpresaConsulta` já usam implementações reais. A navbar e os painéis exibem os atalhos do perfil. Os destinos desses atalhos são implementados pelos demais desenvolvedores; nesta base ainda podem retornar 404. O cadastro de coletora não substitui sua homologação (Dev 2).

O layout reutilizável está em `templates/fragments/layout.html`, com os fragmentos `head(titulo)`, `navbar` e `rodape`. A navbar reserva `#aviso-notificacoes` para o Dev 5. O seletor compartilhado `static/js/shared/seletor-localizacao.js` usa `#mapa-localizacao`, `#latitude` e `#longitude`; o mapa requer internet para Leaflet/OpenStreetMap e oferece preenchimento manual das coordenadas.

Roteiro completo de revisão e apresentação: [docs/entrega-dev1.md](docs/entrega-dev1.md). A tela “Minha empresa”, acabamento extra das páginas de erro e E2E automatizado são P2 no plano.

No PowerShell, se o `java` padrão apontar para uma versão antiga, configure o JDK 21 antes do Maven:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21' # ajuste para sua instalação
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

**DBeaver:** nova conexão PostgreSQL em `localhost:5432`, banco `ewaste`, usuário `ewaste`, senha `ewaste`.

**Usuários de exemplo** (perfil dev, senha `senha123`): `admin@teste.com`, `geradora@teste.com`, `coletora@teste.com` e `coletora2@teste.com`. Detalhes em [docs/contratos.md](docs/contratos.md).

## Comandos do dia a dia

| Objetivo | Linux/Mac | Windows |
| --- | --- | --- |
| Subir o banco | `docker compose up -d` | igual |
| Parar o banco | `docker compose down` | igual |
| Apagar o banco e recomeçar do zero | `docker compose down -v` | igual |
| Formatar o código | `./mvnw spotless:apply` | `mvnw.cmd spotless:apply` |
| Build + todos os testes | `./mvnw verify` | `mvnw.cmd verify` |
| Só testes unitários | `./mvnw test` | `mvnw.cmd test` |
| Rodar a aplicação | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev` |

O build formata o código automaticamente (Spotless). No CI, código fora do formato reprova o PR.

## Módulos e donos

Cada pasta tem um dono. Só o dono altera os arquivos dela; os outros usam o que está no pacote `api` do módulo.

| Pacote (`br.com.ewaste.*`) | Responsabilidade | Dono | Rotas |
| --- | --- | --- | --- |
| `usuario`, `shared` | Cadastro, login, perfis, layout, segurança | Dev 1 — Miguel | `/`, `/login`, `/cadastro/**`, `/painel` |
| `homologacao`, `armazenamento` | Homologação de coletoras e arquivos | Dev 2 — Felipe | `/coletora/homologacao`, `/admin/homologacoes` |
| `solicitacao`, `roteamento` | Solicitação, alertas por região e aceite | Dev 3 — Davi | `/geradora/solicitacoes`, `/coletora/oportunidades` |
| `rastreio`, `documento` | Rastreio da coleta e documentos PDF | Dev 4 — Guilherme | `/coletora/coletas`, `/geradora/documentos`, `/verificar/**` |
| `notificacao`, `mapa`, `monetizacao` | Notificações, mapa público e cobrança | Dev 5 — Rafael | `/notificacoes`, `/mapa`, `/api/public/**`, `/admin/financeiro` |

Organização interna de cada módulo:

```
<modulo>/
├── api/          # o que outros módulos podem usar: interfaces + DTOs (records)
├── web/          # controllers e formulários
├── dominio/      # entidades JPA, enums, regras
├── servico/      # casos de uso (@Service) e implementações do api/
└── repositorio/  # interfaces Spring Data JPA
```

Templates em `src/main/resources/templates/<modulo>/` e scripts em `src/main/resources/static/js/<modulo>/`.

## Convenções

- **Regra de módulos:** um módulo só importa `outroModulo.api` ou `shared`. O teste `ArquiteturaTest` reprova o build se a regra for quebrada.
- **Entre módulos, referência por id** (`Long geradoraId`), nunca por entidade JPA de outro módulo.
- **Nomes:** classes em PascalCase (`SolicitacaoService`), métodos e variáveis em camelCase, tabelas e colunas em snake_case no singular (`solicitacao`, `peso_estimado_kg`), rotas em kebab-case (`/admin/pontos-descarte`). Domínio em português; sufixos técnicos em inglês (`Controller`, `Service`, `Repository`).
- **Migrações Flyway** (`src/main/resources/db/migration`): base `V1`; Miguel `V100–V199`; Felipe `V200–V299`; Davi `V300–V399`; Guilherme `V400–V499`; Rafael `V500–V599`. Nunca edite uma migração que já está na `main`: crie outra.
- **Dados de exemplo** (`db/seed`, só no perfil dev): migrações repetíveis `R__<nn>_seed_<modulo>.sql`, idempotentes (`ON CONFLICT DO NOTHING`).
- **Testes:** `*Test.java` para unitários (rodam em `test`) e `*IT.java` para integração com banco real via Testcontainers (rodam em `verify`, precisam do Docker aberto).

## Fluxo de trabalho com Git

```bash
git switch main && git pull origin main          # sempre partir da main atualizada
git switch -c feat/<seu-nome>/<tarefa>            # ex.: feat/davi/aceite-concorrente
# ... código + testes ...
git add -p
git commit -m "feat(solicitacao): cria formulário de nova solicitação"
git fetch origin && git rebase origin/main        # sincronizar (todo dia e antes do PR)
./mvnw verify
git push -u origin feat/<seu-nome>/<tarefa>
```

Depois abra o Pull Request no GitHub. O merge é por **Squash and merge**, depois de 1 aprovação e com o CI verde.

**Commits** seguem `tipo(módulo): descrição no imperativo`. Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`, `ci` e `contrato` (mudança em `shared/events` ou em um pacote `api`, que exige 2 aprovações).

**Branches** são curtas (1 a 2 dias), uma por tarefa, com o nome no formato `feat/<nome>/<tarefa>` ou `fix/<nome>/<tarefa>`.
