# Roteiro de estudo e apresentação — Davi

Planejamento de 27/11/2026. Entrega em 05/12/2026 às 23h59 e apresentação em 07/12/2026. A presença e a avaliação individual seguem o documento oficial; estudo, ensaio e presença não ficam registrados como realizados até Davi executá-los.

Foco definido em [equipe.md](equipe.md): requisitos e critérios de aceitação, solicitação e roteamento, integração banco → AVL. Entrega técnica do módulo em [entrega-dev3.md](entrega-dev3.md).

## Trilha no portal

Portal: https://ensino-ewaste.rohnelt.dev . A senha não faz parte do repositório.

| Período | Atividade | Evidência de aprendizado a preparar |
| --- | --- | --- |
| 26/11 a 01/12 | Trilha principal, na ordem do portal | Explicar cada camada com um exemplo real do repositório. |
| até 03/12 | Aulas 2, 3 e 6 | Relacionar cada aula a um arquivo ou teste do fluxo de solicitação. |
| até 04/12 | Rotações da AVL e aceite atômico | Explicar os dois sem ler, usando o código e os testes indicados abaixo. |

## Demonstração individual

Conforme o [roteiro individual](testes.md#roteiro-individual): uma história, a UML e a implementação dela, uma decisão explicada e um teste com resultado obtido.

**Principal: RF10 / CA10, aceite da coleta.**

1. História: a primeira coletora apta assume a coleta; as demais são recusadas ([requisitos.md](requisitos.md)).
2. UML: caso de uso "Aceitar e agendar coleta" (`docs/uml/casos-uso.puml`) e a atividade "Coletora apta aceita e agenda" (`docs/uml/atividades.puml`).
3. Implementação: `OportunidadeController` → `AceiteService` → `SolicitacaoRepository.aceitarSeAberta`.
4. Decisão: o aceite é um único `UPDATE solicitacao SET status = 'ACEITA', coletora_id = :coletoraId ... WHERE id = :id AND status = 'ABERTA'`. O banco só deixa uma das atualizações encontrar a linha ainda ABERTA: quem recebe 1 venceu, quem recebe 0 chegou depois. Uma leitura seguida de gravação na aplicação deixaria duas coletoras lerem ABERTA ao mesmo tempo.
5. Teste: `AceiteConcorrenteIT.duasColetorasAoMesmoTempoExatamenteUmaVence` (T11), com PostgreSQL real.

**Alternativa: RF09 / CA09, acessibilidade (T14).** Auditoria, correção e reteste na seção "Auditoria de acessibilidade (T14)" de [testes.md](testes.md), trazida pelos PRs #26 e #28. Decisão: com o mapa em foco, Enter marca o centro, complementando as coordenadas manuais do #25. Teste: `SolicitacaoControllerTest.errosFicamAssociadosAosCamposParaLeitorDeTela` (#28), escrito antes da correção.

## Estrutura de Dados II

Relatório: [estrutura-dados.md](estrutura-dados.md). Código: `src/main/java/br/com/ewaste/mapa/dominio/ArvoreAvl.java`. Diagrama: `docs/uml/sequencia.puml` mostra o caminho PostgreSQL → inserir na AVL → percurso em ordem → filtros.

1. Fator de balanceamento: altura da subárvore esquerda menos a da direita. Fora de -1 a 1, a árvore rotaciona.
2. Esquerda pesada: caso LL, rotação simples à direita; caso LR, rotação à esquerda no filho e depois à direita. Direita pesada é o espelho (RR e RL).
3. Custo: busca e inserção O(log n) porque a altura fica logarítmica; percurso em ordem O(n); filtro por cidade ou categoria continua O(n), porque a chave é o ID.
4. Limite: a árvore é montada a cada consulta a partir do banco, que continua sendo a fonte da verdade; não há ganho alegado contra uma consulta SQL indexada.

## Perguntas de ensaio

1. O que acontece se duas coletoras clicarem em aceitar no mesmo instante? Qual teste prova?
2. Por que o roteamento roda depois do commit (AFTER_COMMIT) e numa transação própria?
3. Uma coletora aprovada hoje recebe alerta de uma solicitação criada ontem? Onde isso acontece?
4. Uma solicitação a exatamente 50 km de uma coletora com raio de 50 km gera alerta?
5. Por que a geradora não pode mais editar depois do aceite? Que resposta HTTP recebe?
6. Quais transições de status existem e qual exceção uma transição proibida lança?
7. Em que caso a AVL faz rotação dupla? Desenhe a partir de três inserções.
8. Como alguém que usa só o teclado marca o local da coleta?
