# Recursos SQL demonstráveis — Banco de Dados II

Fonte acadêmica: `PI2026-2 ENG4-T1.pdf`, páginas 3–4. Implementação adicional em
`src/main/resources/db/migration/V601__recursos_academicos.sql`; depende da V600.
Não editar migrations antigas nem experimentar branches no banco comum da equipe.

## View: acompanhamento sem duplicar contagens

`vw_acompanhamento_solicitacao` reúne solicitação, nomes das empresas e do ponto,
quantidade de linhas/itens, documentos e data do último evento. Agregações laterais
separadas impedem que dois documentos multipliquem a soma de itens. Usa
`security_invoker=true` no PostgreSQL 16: não concede ao leitor privilégios que ele
não possui nas tabelas. A view não implementa isolamento por proprietário; a
autorização por empresa continua sendo responsabilidade dos serviços autenticados.
Usuários SQL administrativos devem ser limitados a responsáveis autorizados.

Consulta de leitura para demonstração sobre dados fictícios:

```sql
SELECT solicitacao_id, status, ponto_nome, linhas_itens, quantidade_total,
       documentos_emitidos, ultimo_evento_em
FROM vw_acompanhamento_solicitacao
ORDER BY solicitacao_id;
```

## Function: compatibilidade do material

`ponto_aceita_tipo(BIGINT, VARCHAR)` retorna booleano. Somente pontos ativos que
listem explicitamente a categoria em `tipos_aceitos` aceitam o material. O formato
é CSV dos nomes do enum `TipoResiduo`, por exemplo `INFORMATICA,TELEFONIA`.
Aceita espaços e diferenças de maiúsculas; categoria inexistente, ponto inexistente,
NULL ou lista vazia retornam falso. `OUTRO` não significa aceitar qualquer material.
A função é `STABLE` e executa com os privilégios do chamador.

```sql
SELECT id, nome, ponto_aceita_tipo(id, 'INFORMATICA'::varchar) AS aceita_informatica
FROM ponto_descarte;
```

## Stored Procedure: destinação atômica

`registrar_destinacao_pi(BIGINT, BIGINT, TEXT)` é chamada pelo método
`RastreioService.registrarDestinacao`, após as verificações de propriedade e
validação que já existiam. A procedure também exige estado `COLETADA`, ator ativo
do perfil `COLETORA` pertencente à empresa vencedora e descrição não vazia de até
500 caracteres. Ela bloqueia a solicitação com `FOR UPDATE`, verifica se já há
destinação e grava um único evento `DESTINADA`. Duas chamadas concorrentes pela
procedure são serializadas pelo bloqueio. Não é uma garantia contra administradores
que gravem diretamente nas tabelas ignorando o fluxo.

Não há `COMMIT` interno: a operação participa da transação do serviço. Os erros de
uso direto têm SQLSTATE `22023` (descrição), `P0002` (solicitação inexistente),
`23514` (estado/duplicação) ou `42501` (ator sem autorização). A aplicação mantém as
mensagens de validação anteriores para o fluxo web. A posse de credenciais SQL da
aplicação não equivale à identidade humana do login; não distribuir essas
credenciais a terceiros nem expor o servidor à internet.

Chamadas que alteram dados são demonstradas em Testcontainers, não no banco comum:

```sql
-- IDs são criados pelo teste RecursosSqlIT, não valores reais da equipe:
-- CALL registrar_destinacao_pi(id_ficticio, usuario_ficticio, 'Reciclagem fictícia');
```

## Trigger: auditoria transacional de estado

`trg_auditar_status_solicitacao_pi` chama `auditar_status_solicitacao_pi()` após uma
alteração efetiva de `solicitacao.status`. Grava em `solicitacao_auditoria` o estado
anterior/novo, a conta SQL, o identificador da transação e o instante. Não duplica
eventos da linha do tempo. Regravar o mesmo estado não cria auditoria. Se a
transação for revertida, a auditoria também é revertida.

A conta SQL registrada é técnica e pode ser compartilhada por instâncias; não
identifica, por si só, o integrante nem o usuário autenticado no aplicativo. O
registro não é uma trilha imutável contra o administrador do banco. O acesso à
tabela deve acompanhar a política de usuários e permissões da infraestrutura.

```sql
SELECT solicitacao_id, status_anterior, status_novo, usuario_banco,
       transacao_id, registrado_em
FROM solicitacao_auditoria
ORDER BY id;
```

## Testes e limites das evidências

`src/test/java/br/com/ewaste/banco/RecursosSqlIT.java` contém cenários para os quatro
recursos: compatibilidade, contagens com múltiplos joins, destinação válida/inválida,
concorrência e rollback da procedure e da auditoria. O teste usa PostgreSQL 16
descartável via `TestcontainersConfiguration`; não lê `DB_URL` do banco comum para
preparar suas tabelas. O TRUNCATE de preparação só é adequado nesse ambiente
descartável.

Comando de validação proposto, a executar na etapa de testes:

```powershell
.\mvnw.cmd -B '-Dit.test=RecursosSqlIT' verify
```

O comando não substitui o `verify` completo. A existência desses testes não
comprova aprovação; consultar os resultados efetivamente produzidos na entrega.
Atomicidade e isolamento demonstrados se referem ao PostgreSQL. Arquivos em disco
ou um compartilhamento de rede não passam a integrar a mesma transação por causa
de `@Transactional`; o backend de armazenamento e o backup devem documentar seus
próprios limites.
