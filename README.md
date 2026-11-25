# Plataforma E-Waste

Protótipo acadêmico B2B para o Projeto Integrador do 4º período de Engenharia de Software da Faculdade SENAI FATESG, 2026-2. Conecta empresas geradoras a pontos de recebimento e coletoras homologadas. Permite identificar/classificar materiais, consultar pontos por cidade/categoria, solicitar coleta e acompanhar histórico. A primeira coletora apta que aceitar assume a operação. Ao concluir coleta, destinação e mídias previstas, emite CDF, laudo e certificado simulados. O recorte B2B é preservado da base e sua aceitação foi informada pelo usuário em 24/11/2026; registro das confirmações em docs/equipe.md.

> Projeto acadêmico. Não há integração SINIR/IBAMA ou cobrança implementada, e os documentos gerados levam a marca "documento simulado".

## Stack

Java 21 · Spring Boot 3.5 (Web, Thymeleaf, Security, Data JPA, Validation) · PostgreSQL 16 · Flyway · OpenPDF · Leaflet · JUnit 5, Testcontainers, ArchUnit · GitHub Actions

## Pré-requisitos

- Git
- JDK 21 (ex.: [Temurin](https://adoptium.net/))
- Docker Desktop na anfitriã e para Testcontainers; clientes da aplicação não precisam subir PostgreSQL
- IntelliJ IDEA Community ou VS Code com Extension Pack for Java
- DBeaver (opcional, para consultar o banco)

Você **não** precisa instalar o Maven: use o `mvnw` do projeto.

## Como rodar

A equipe utiliza um único PostgreSQL na anfitriã. Os clientes precisam de Java 21 e conexão LAN/VPN; Docker é necessário na anfitriã e para executar os testes de integração isolados.

```powershell
git clone https://github.com/mmsxdev/plataforma-ewaste.git
cd plataforma-ewaste
# Somente se ainda não existir .env:
Copy-Item .env.team.example .env
# Preencha DB_URL da anfitriã e DB_APP_USER/DB_APP_PASSWORD recebidos por canal privado.
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=team'
```

O cliente não sobe outro banco. A preparação da anfitriã, inspeção dos volumes, migrations e firewall estão no [guia de banco compartilhado](docs/banco-compartilhado-local.md). Para ensaios de schema, use Testcontainers ou banco descartável isolado; não a instância comum.

Abra http://localhost:8080 e confira `/actuator/health`. Na execução registrada, foram usadas portas locais 18080/18081 para duas aplicações no mesmo computador. Isso não comprova acesso de outra máquina.

## Equipe: PostgreSQL e arquivos compartilhados

Guia vigente: [docs/banco-compartilhado-local.md](docs/banco-compartilhado-local.md). Cada cliente recebe configuração privada baseada em `.env.team.example` e usa `DB_URL`, `DB_APP_USER`, `DB_APP_PASSWORD`. O perfil team desliga Flyway e usa armazenamento binário PostgreSQL; o mesmo banco compartilha referências e bytes dos arquivos. Dados legados/arquivos de disco são preservados e só importados explicitamente. Não publicar segredos, dump ou `.env`.

```powershell
# Cliente, após receber .env privado e o operador preparar o esquema:
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=team'
# Somente operador, com DB_URL, DB_USER e DB_PASSWORD privados de migração:
.\infra\Migrate-Team.ps1 -ApprovedRevision '<SHA_COMBINADO_40_HEX>'
```

O script confere a revisão, empacota sem DevTools, executa o perfil migrate e encerra; não é o perfil de uso cotidiano. Endereço definitivo, regras de rede e ensaio em duas máquinas permanecem pendentes. O bootstrap em loopback comprova somente a máquina anfitriã; não autoriza declarar conectividade de toda a equipe.

## Documentação do PI e demonstração

- [Equipe, funções, turma e decisões confirmadas](docs/equipe.md)
- [Requisitos, histórias, critérios e dúvidas do PDF](docs/requisitos.md)
- [Matriz de rastreabilidade e estados de comprovação](docs/rastreabilidade.md)
- [Arquitetura e privacidade](docs/arquitetura.md) e [contratos](docs/contratos.md)
- [UML e DER — fontes](docs/uml/) e [banco/dicionário de 18 tabelas](docs/banco-dados.md)
- [Estrutura de Dados II — AVL integrada](docs/estrutura-dados.md)
- [Teoria da Computação — lógica e criptografia didática](docs/teoria-computacao.md)
- [Administração e identidade](docs/administracao.md), [premissas de custos](docs/custos-premissas.csv)
- [Testes e resultados](docs/testes.md) e [implantação/demonstração](docs/implantacao.md)
- [Guia de uso](docs/guia-sistema.md), [guia técnico](docs/guia-tecnico.md) e [perguntas para apresentação](docs/perguntas-respostas.md)
- [Bootstrap reproduzível do E2E](tools/README.md)
- [PDFs acadêmicos](docs/pdf/) — fontes Markdown acima; não confundir com PDFs simulados gerados pelo aplicativo.

Fluxo de demonstração: consultar `/mapa` → selecionar ponto/categoria → cadastrar solicitação identificando materiais → consultar situação/histórico → corrigir descrição/identificação em ABERTA. Rejeição demonstrável: ponto incompatível ou edição após aceite. A árvore AVL recebe dados do banco antes dos filtros/busca; `V601` agrega View, Function, Procedure e Trigger aplicados ao fluxo. Executar os casos e conferir resultados antes de declarar entrega concluída. Divergências de datas/grupos do documento oficial estão preservadas em requisitos.md.

A futura anfitriã do PostgreSQL será a máquina de Felipe; a instância atual em Miguel permanece preservada até transferência e validação reais. Rafael será operador do banco e Felipe responsável pelos backups/recuperação; o destino de cópia fora da nova anfitriã ainda está pendente. Dados do protótipo são exclusivamente fictícios, sem produção.

## Entrega Dev 1 (P1)

Cadastro em `/cadastro/geradora` e `/cadastro/coletora`, login em `/login` e painel por perfil em `/painel`. O cadastro valida CNPJ, e-mail, senha, coordenadas e raio da coletora; grava empresa e usuário na mesma transação, protege a senha com BCrypt e publica `EmpresaCadastradaEvent` para o módulo de homologação. CNPJ e e-mail duplicados são recusados, inclusive com restrição de unicidade no banco. E-mails são comparados sem distinção de maiúsculas.

`UsuarioAtual` e `EmpresaConsulta` já usam implementações reais. A navbar e os painéis exibem os atalhos disponíveis do perfil. Envio/análise administrativa de homologação, financeiro e notificações não têm interface completa; não são oferecidos como links navegáveis nesta demonstração. O cadastro de coletora não substitui sua homologação (Dev 2).

O layout reutilizável está em `templates/fragments/layout.html`, com os fragmentos `head(titulo)`, `navbar` e `rodape`. A navbar reserva `#aviso-notificacoes` para o Dev 5. O seletor compartilhado `static/js/shared/seletor-localizacao.js` usa `#mapa-localizacao`, `#latitude` e `#longitude`; o mapa requer internet para Leaflet/OpenStreetMap e oferece preenchimento manual das coordenadas.

Roteiro completo de revisão e apresentação: [docs/entrega-dev1.md](docs/entrega-dev1.md). A tela “Minha empresa”, acabamento extra das páginas de erro eram P2 no plano anterior; o E2E desta adequação está registrado em docs/testes.md.

No PowerShell, se o `java` padrão apontar para uma versão antiga, configure o JDK 21 antes do Maven:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21' # ajuste para sua instalação
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=team'
```

**DBeaver/pgAdmin:** no modo individual, conecte em `localhost:5432`; no modo compartilhado, use o IP privado da anfitriã em todas as máquinas, inclusive nela. Banco `ewaste`, usuário limitado definido em `DB_APP_USER` e senha definida em `DB_APP_PASSWORD`. Para o desenvolvimento da equipe com **os mesmos dados**, siga [banco compartilhado na rede local](docs/banco-compartilhado-local.md). No perfil team, uploads e PDFs ficam em BYTEA no PostgreSQL, sem exigir pasta SMB.

**Usuários de exemplo** (perfil dev, senha `senha123`): `admin@teste.com`, `geradora@teste.com`, `coletora@teste.com` e `coletora2@teste.com`. Detalhes em [docs/contratos.md](docs/contratos.md).

## Comandos do dia a dia

| Objetivo | Linux/Mac | Windows |
| --- | --- | --- |
| Subir o banco (somente anfitriã) | `docker compose up -d` | igual |
| Parar o banco (manutenção combinada) | `docker compose down` | igual |
| Inspecionar containers e volumes antes de qualquer limpeza | `docker compose ps -a` / `docker volume ls` | igual |
| Formatar o código | `./mvnw spotless:apply` | `mvnw.cmd spotless:apply` |
| Build + todos os testes | `./mvnw verify` | `mvnw.cmd verify` |
| Só testes unitários | `./mvnw test` | `mvnw.cmd test` |
| Rodar a aplicação | `./mvnw spring-boot:run -Dspring-boot.run.profiles=team` | `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=team` |

O build formata o código automaticamente (Spotless). No CI, código fora do formato reprova o PR.

## Módulos e donos

A tabela abaixo registra a divisão técnica histórica de módulos. As funções de fechamento, focos acadêmicos e revisões cruzadas atuais estão em [docs/equipe.md](docs/equipe.md); todos estudam o fluxo completo. Alterações em módulos de colegas são coordenadas, usando os pacotes `api`; a atribuição de foco não comprova autoria ou contribuição passada.

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

Documentação complementar: [normalização dos relatórios](docs/normalizacao.md) e [transferência planejada para Felipe](docs/transferencia-felipe.md).
