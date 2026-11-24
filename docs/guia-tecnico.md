# Guia técnico e execução da demonstração

Índice complementar aos documentos existentes; fontes normativas em [requisitos](requisitos.md), decisões em [arquitetura](arquitetura.md), operação compartilhada em [banco-compartilhado-local](banco-compartilhado-local.md). Este guia não substitui os scripts de operador nem contém credenciais.

## 1 Estrutura e versões

Java 21, Spring Boot 3.5.16, Thymeleaf, Security, JPA/JDBC, PostgreSQL 16 e Flyway; versões exatas em `pom.xml`. Backend e frontend são um monólito modular; pacotes expõem `api` para comunicação entre módulos. O banco tem 18 tabelas de aplicação/infra e a tabela técnica do Flyway. Dicionário e DER: [banco-dados](banco-dados.md), [uml](uml/).

Fluxo importante: `PontoColetaRepository` → `PontosColetaService` → `ArvoreAvl` → consulta/busca → `PontosColetaController`; solicitação usa `mapa.api.PontosColeta`, sem importar implementação interna. `SolicitacaoController` constrói `Solicitacao` com empresa autenticada e chama `SolicitacaoService.criar(nova)`. O serviço grava e publica evento dentro da transação; listeners de roteamento/rastreio recebem AFTER_COMMIT e gravam em REQUIRES_NEW. A ordem entre listeners não deve ser inferida pela posição no diagrama.

## 2 Preparar ambiente sem confundir bancos

Conferir JDK, Docker e versão combinada do repositório. O banco comum da equipe tem dados a preservar. Para E2E use um banco **novo isolado**, porta/volume próprios, preparado pelo operador com migrations e seeds fictícios. `.env` e arquivos privados locais ficam ignorados; não cole credenciais em terminal compartilhado/prints.

Somente operador roda `infra/Migrate-Team.ps1 -ApprovedRevision <SHA-40-confirmado>`; o script confere revisão/estado e erros. `-IncludeDemoData` é exclusivo do banco fictício de demonstração, não da preparação cotidiana comum. Não usar repair, clean ou reset para contornar migração recusada; [operação](operacao-banco.md) detalha backup e recuperação. Comandos exatos de migração podem evoluir: conferir assinatura atual do script antes de executar.

O migrador compara HEAD com a revisão aprovada e examina **worktree, index e arquivos novos inclusive ignorados** nos caminhos protegidos (`src/main`, `pom.xml`, wrapper/.mvn e o próprio script). Documento/PDF local não relacionado e target residual não bloqueiam por si só. O build usa `git archive` da revisão aprovada em diretório temporário próprio, sem carregar target, `.env` ou arquivos soltos para dentro do snapshot; também confere se o build alterou as entradas. Só o JAR desse snapshot pode ser executado. Credenciais continuam na configuração privada original, sem serem copiadas ao snapshot. Não resolver bloqueio removendo arquivos ou mudando o SHA sem revisão; corrigir/aprovar a origem primeiro.

O guard foi exercitado em 11 cenários isolados ([migrador-cenarios.json](evidencias/fechamento-20261124/migrador-cenarios.json)), incluindo index/SQL ignorado e documentos não relacionados. Os cenários permitidos simulam a chamada Java; essa evidência de proteção não é uma migração real contra o banco comum e não deve ser apresentada como tal.

O perfil **team** usa `DB_URL`, `DB_APP_USER`, `DB_APP_PASSWORD`, não roda Flyway e valida esquema. O operador tem credencial distinta. Nesse perfil `ArmazenamentoCompartilhado` guarda bytes e metadados no PostgreSQL; fora dele, disco legado é preservado. JDBC/backup são compartilhados; upload não depende de SMB.

## 3 Duas instâncias de demonstração

Depois de preparar um JAR da revisão aprovada e configurar privadamente as variáveis para o banco isolado, abrir dois terminais na raiz:

```powershell
# Terminal A, banco fictício isolado já configurado:
java -jar target/plataforma-ewaste-0.0.1-SNAPSHOT.jar --spring.profiles.active=team --server.port=18080
# Terminal B, mesma configuração de banco fictício:
java -jar target/plataforma-ewaste-0.0.1-SNAPSHOT.jar --spring.profiles.active=team --server.port=18081
```

Conferir `/actuator/health` nas duas portas. Isso não cria banco nem compila o JAR. O aplicativo continua ligado ao loopback; compartilhar o banco com outro computador exige decisão de rede, IP, firewall e teste autorizado, ainda pendentes. Não inferir que um IP detectado é configuração definitiva.

## 4 Testes e ferramentas reproduzíveis

```powershell
.\mvnw.cmd -B spotless:check
.\mvnw.cmd -B verify
npm.cmd ci --prefix tools
npm.cmd run browser:install --prefix tools
npm.cmd test --prefix tools
$env:PI_DEMO_CONFIRM = 'FICTICIO'
npm.cmd run demo --prefix tools
```

Comandos Java devem usar banco Testcontainers descartável, não o comum. Ferramentas Node são dependências de desenvolvimento pinadas em tools/package-lock.json; instalação/Chromium não dependem do Codex. Opções de navegador, data e execução em [tools/README](../tools/README.md). Agendamento calculado 48h adiante em São Paulo; override só se for real e futuro. Cada rodada gera pasta única em `target/evidencias/`; não sobrescreve histórico.

O E2E de fechamento passou em 12/12 verificações com Playwright do projeto/Chromium gerenciado e banco isolado15432; JSON e PNG estão em docs/evidencias/fechamento-20261124. A regressão final passou em 331 testes Java (236 unitários + 95 IT), sem falhas/erros/pulos, com spotless:check aprovado; resumo atual em [testes](testes.md). Os totais anteriores permanecem históricos. Não alegar execução destes comandos apenas pela existência deste guia.

## 5 Artefatos e integridade

Diagramas: fontes `.puml` e PNG em docs/uml. Relatórios: fontes Markdown e PDFs em docs/pdf; `tools/gerar-relatorios.py --only banco-dados design-software` regenera só os afetados e mescla manifesto. Após estabilizar fontes e resultados, renderizar diagramas, exportar, conferir tipos SQL/PK/atributos, tabelas e cortes visualmente. Preservar EDII, Teoria, Administração e planilha se suas fontes não mudaram.

O manifesto SHA-256 detecta alterações nas fontes/diagramas incluídos; não é assinatura de autoria/aprovação docente. Evidências E2E antigas permanecem com data/escopo originais; a nova rodada usa pasta própria. RSA didático está no relatório de Teoria, separado de BCrypt e SHA-256 reais.

## 6 Demonstração técnica individual

Escolha história/CA em requisitos.md, localize o caso UML e serviço correspondente, mostre um teste aprovado e explique limite/decisão. Sugestões: AVL entre banco e ponto; ponto ativo/categoria; transação e rejeição sem gravação parcial; edição por empresa/estado; Procedure de destinação e Trigger; armazenamento binário entre instâncias. Não atribua contribuições a pessoas sem confirmação. Apoio: [perguntas-respostas](perguntas-respostas.md).
