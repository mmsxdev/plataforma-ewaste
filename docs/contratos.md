# Contratos entre módulos (PR 0)

Tudo o que um módulo oferece aos outros está aqui. Mudar um contrato exige um PR próprio, do tipo `contrato(...)`, aprovado pelos donos afetados. Prefira **adicionar** a remover ou renomear.

## Banco de dados

Esquema inicial em `src/main/resources/db/migration/V1__schema_inicial.sql`, com o dono de cada tabela comentado. Entre módulos, a referência é **por id**; nunca mapeie com JPA a entidade de outro módulo.

| Tabela | Dono |
| --- | --- |
| `empresa`, `usuario` | Dev 1 — Miguel |
| `homologacao`, `documento_homologacao` | Dev 2 — Felipe |
| `solicitacao`, `solicitacao_item`, `alerta_solicitacao` | Dev 3 — Davi |
| `evento_rastreio`, `midia_destruida`, `documento_emitido` | Dev 4 — Guilherme |
| `notificacao`, `ponto_descarte`, `plano`, `assinatura`, `lancamento` | Dev 5 — Rafael |

## Ciclo da solicitação (`solicitacao.api.StatusSolicitacao`)

```
ABERTA -> ACEITA -> AGENDADA -> COLETADA -> CONCLUIDA
ABERTA -> CANCELADA | EXPIRADA
```

Só o módulo `solicitacao` altera o status. O Dev 4 pede `marcarColetada` e `marcarConcluida` via `TransicaoSolicitacao`.

## Interfaces (`<modulo>.api`)

| Interface | Implementa | Usa | Implementação atual |
| --- | --- | --- | --- |
| `usuario.api.UsuarioAtual` | Dev 1 | todos | Real: `IdentidadeService` (usuário autenticado, persistido e ativo) |
| `usuario.api.EmpresaConsulta` | Dev 1 | Dev 2, 3, 4, 5 | Real: `EmpresaConsultaService` (repositório JPA) |
| `homologacao.api.ColetorasAptas` | Dev 2 | Dev 3 | `ColetorasAptasProvisoria` (todas as coletoras são aptas) |
| `armazenamento.api.Armazenamento` | Dev 2 | Dev 2, Dev 4 | `ArmazenamentoProvisorio` (só em memória) |
| `solicitacao.api.SolicitacaoConsulta` | Dev 3 | Dev 4, Dev 5 | `SolicitacaoProvisoria` (não encontra nada) |
| `solicitacao.api.TransicaoSolicitacao` | Dev 3 | Dev 4 | `SolicitacaoProvisoria` (só registra no log) |

O login usa a implementação real `IdentidadeService`, com consulta por e-mail sem distinção de maiúsculas, BCrypt e bloqueio de contas inativas. As três classes provisórias de `usuario/servico` foram substituídas; as interfaces e os DTOs de `usuario/api` permanecem compatíveis. A tela de login continua sendo a padrão do Spring Security nesta etapa.

**Como funcionam as provisórias:** todas têm `@Fallback`. Quando o dono cria a implementação real, uma classe `@Service` comum que implementa a mesma interface, o Spring passa a injetar a real automaticamente. O dono então **apaga a provisória no mesmo PR**.

## Eventos de domínio (`shared.events`)

Publicar: `ApplicationEventPublisher.publishEvent(new XxxEvent(...))` dentro da transação do caso de uso.
Ouvir: `@TransactionalEventListener` (roda depois do commit). Se o listener grava no banco, use também `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

| Evento | Publicado por | Ouvido por |
| --- | --- | --- |
| `EmpresaCadastradaEvent(empresaId, tipo)` | Dev 1 | Dev 2 (cria homologação PENDENTE para coletoras) |
| `ColetoraHomologadaEvent(coletoraId)` | Dev 2 | Dev 5 |
| `ColetoraReprovadaEvent(coletoraId, motivo)` | Dev 2 | Dev 5 |
| `SolicitacaoCriadaEvent(solicitacaoId, geradoraId)` | Dev 3 | Dev 3 (roteamento), Dev 4 |
| `AlertasEnviadosEvent(solicitacaoId, coletoraIds)` | Dev 3 | Dev 4, Dev 5 (notifica as coletoras) |
| `SemColetoraNaRegiaoEvent(solicitacaoId, geradoraId)` | Dev 3 | Dev 5 (avisa a geradora) |
| `SolicitacaoAceitaEvent(solicitacaoId, geradoraId, coletoraId)` | Dev 3 | Dev 4, Dev 5 (notifica e lança TAXA_MATCH) |
| `SolicitacaoAgendadaEvent(solicitacaoId, geradoraId, agendadaPara)` | Dev 3 | Dev 4, Dev 5 |
| `ColetaRegistradaEvent(solicitacaoId, geradoraId)` | Dev 4 | Dev 5 |
| `DocumentoEmitidoEvent(documentoId, solicitacaoId, geradoraId, tipo)` | Dev 4 | Dev 5 (notifica e lança TAXA_DOCUMENTO) |

## Rotas

| Prefixo | Acesso | Módulos |
| --- | --- | --- |
| `/`, `/login`, `/cadastro/**`, `/mapa`, `/api/public/**`, `/verificar/**` | público | Dev 1, Dev 5, Dev 4 |
| `/geradora/**` | GERADORA | Dev 3 (`/geradora/solicitacoes`), Dev 4 (`/geradora/documentos`) |
| `/coletora/**` | COLETORA | Dev 2 (`/coletora/homologacao`), Dev 3 (`/coletora/oportunidades`), Dev 4 (`/coletora/coletas`), Dev 5 (`/coletora/assinatura`) |
| `/admin/**` | ADMIN | Dev 2 (`/admin/homologacoes`), Dev 5 (`/admin/pontos-descarte`, `/admin/financeiro`) |
| `/painel`, `/notificacoes`, `/api/notificacoes/**` | autenticado | Dev 1, Dev 5 |

## Cobrança

Os valores das tarifas ficam em `application.yml` (`app.tarifas.*`, criado pelo Dev 5). A `TAXA_MATCH` é paga pela coletora e a `TAXA_DOCUMENTO`, pela geradora. Índices únicos no banco impedem cobrança em dobro do mesmo evento.

## Usuários de exemplo (perfil dev)

Senha de todos: `senha123`

| E-mail | Perfil | Empresa |
| --- | --- | --- |
| `admin@teste.com` | ADMIN | — |
| `geradora@teste.com` | GERADORA | Geradora Exemplo Ltda (São Paulo) |
| `coletora@teste.com` | COLETORA | Recicla Centro (São Paulo, raio de 50 km) |
| `coletora2@teste.com` | COLETORA | EcoColeta Rio (Rio de Janeiro, a ~360 km: fora do raio) |
