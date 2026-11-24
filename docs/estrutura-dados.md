# Relatório técnico — Estrutura de Dados II

## 1 Introdução

O PI exige uma árvore que receba dados oriundos do banco antes da manipulação pelo sistema (PDF oficial, p.3). O fluxo escolhido é a consulta pública dos pontos de coleta. Uma lista estática de exemplo e os índices internos do PostgreSQL não satisfazem essa integração.

## 2 Desenvolvimento

Arquivos: `src/main/java/br/com/ewaste/mapa/dominio/ArvoreAvl.java`, `mapa/repositorio/PontoColetaRepository.java`, `mapa/servico/PontosColetaService.java`, `mapa/api/PontosColeta.java` e `PontoResumo.java` (os últimos caminhos relativos ao pacote br/com/ewaste).

O repositório JDBC obtém registros de `ponto_descarte`; o serviço insere os registros numa **AVL própria, genérica, com chave Long id**. A busca por ID usa a árvore. A listagem percorre em ordem e aplica os filtros de cidade/categoria para produzir a consulta pública em `/mapa`. O encadeamento é PostgreSQL → registros → inserir na AVL → buscar/percurso → filtros → resposta Thymeleaf. Não se trata de substituir o SGBD ou manter um segundo banco.

Uma AVL é uma árvore binária de busca que mantém, em cada nó, alturas e diferença entre alturas dos filhos limitada a -1, 0 ou 1 após balanceamento. Rotação simples ou dupla restabelece esse limite quando necessário. Inserção e busca têm custo O(log n) com balanceamento; percurso ordenado custa O(n); construir a partir de n linhas por inserções custa O(n log n), com O(n) de memória. Filtro de texto/categoria no percurso continua O(n), não O(log n): a chave é ID, não cidade. Não alegamos ganho de desempenho contra uma consulta SQL indexada.

Operações demonstráveis: `inserir`, `buscar`, `emOrdem`, `altura`. A chave ID mantém registros com nomes repetidos distintos. Os nós em memória não persistem alterações no banco; o banco é a fonte da verdade. A construção por consulta evita cache indefinidamente desatualizado, com custo de leitura e memória adequado ao pequeno protótipo. Para escala maior, paginação/índices e estratégia de cache exigiriam decisão específica.

Roteiro: inserir pontos fictícios no banco de demonstração por meio autorizado; consultar `/mapa`; conferir a correspondência banco/resultado; aplicar cidade/categoria; localizar um ID e iniciar solicitação; demonstrar teste de balanceamento com inserções em ordens diferentes. Evidências de teste e nome final dos casos estão em [testes](testes.md). **A existência deste relatório não comprova execução.**

Limite acadêmico: confirmar com o professor se um fluxo integrado relevante basta, se espera todos os dados de todas as consultas e se exige remoção/balanceamento dessa operação. Esta implementação foi escolhida para consulta, sem remoção de nós pela interface.

## 3 Conclusão

A AVL torna explícita a passagem dos dados persistidos por uma árvore antes da consulta. A utilidade demonstrada é organização e busca ordenada no aplicativo, com custo e limites descritos. O critério final exige inspecionar a implementação e executar seus testes e o fluxo real; resultados pendentes não devem ser apresentados como aprovados.

## Referências

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. p.3.

E-WASTE. **ArvoreAvl e fluxo de consulta de pontos**. Código-fonte deste repositório, diretório src/main/java/br/com/ewaste/mapa. 2026. A referência corresponde à implementação, não substitui bibliografia teórica indicada pelo professor. Revisão bibliográfica/normalização ABNT final: pendente de conferência da equipe.

ADEL'SON-VEL'SKII, G. M.; LANDIS, E. M. **An algorithm for organization of information**. Doklady Akademii Nauk SSSR, v.146, n.2, p.263–266, 1962. Artigo original em russo, título conforme catálogo Math-Net.Ru. Disponível em: https://www.mathnet.ru/eng/dan/v146/i2/p263. Acesso em: 24 nov. 2026. Metadados bibliográficos conferidos no catálogo; a explicação operacional deste relatório é acompanhada pelo código próprio e seus testes.
