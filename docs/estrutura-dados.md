# Relatório técnico — Estrutura de Dados II

## 1 Introdução

O PI exige uma árvore que receba dados oriundos do banco antes da manipulação pelo sistema (Faculdade SENAI FATESG, 2026, p. 3). O fluxo escolhido é a consulta pública dos pontos de coleta. Uma lista estática de exemplo e os índices internos do PostgreSQL não satisfazem essa integração.

## 2 Desenvolvimento

Arquivos: `src/main/java/br/com/ewaste/mapa/dominio/ArvoreAvl.java`, `mapa/repositorio/PontoColetaRepository.java`, `mapa/servico/PontosColetaService.java`, `mapa/api/PontosColeta.java` e `PontoResumo.java` (os últimos caminhos relativos ao pacote br/com/ewaste) (E-Waste, 2026).

O repositório JDBC obtém registros de `ponto_descarte`; o serviço insere os registros numa **AVL própria, genérica, com chave Long id**. A busca por ID usa a árvore. A listagem percorre em ordem e aplica os filtros de cidade/categoria para produzir a consulta pública em `/mapa`. O encadeamento é PostgreSQL → registros → inserir na AVL → buscar/percurso → filtros → resposta Thymeleaf. Não se trata de substituir o SGBD ou manter um segundo banco.

Proposta por Adel'son-Vel'skii e Landis (1962), a AVL é uma árvore binária de busca que mantém, em cada nó, alturas e diferença entre alturas dos filhos limitada a -1, 0 ou 1 após balanceamento. Rotação simples ou dupla restabelece esse limite quando necessário (Lima, [2026], p. 3). Inserção e busca têm custo O(log n) com balanceamento (Lima, [2026], p. 5); percurso ordenado custa O(n); construir a partir de n linhas por inserções custa O(n log n), com O(n) de memória. Filtro de texto/categoria no percurso continua O(n), não O(log n): a chave é ID, não cidade. Não alegamos ganho de desempenho contra uma consulta SQL indexada.

Operações demonstráveis: `inserir`, `buscar`, `emOrdem`, `altura`. A chave ID mantém registros com nomes repetidos distintos. Os nós em memória não persistem alterações no banco; o banco é a fonte da verdade. A construção por consulta evita cache indefinidamente desatualizado, com custo de leitura e memória adequado ao pequeno protótipo. Para escala maior, paginação/índices e estratégia de cache exigiriam decisão específica.

Roteiro: inserir pontos fictícios no banco de demonstração por meio autorizado; consultar `/mapa`; conferir a correspondência banco/resultado; aplicar cidade/categoria; localizar um ID e iniciar solicitação; demonstrar teste de balanceamento com inserções em ordens diferentes. Evidências de teste e nome final dos casos estão em [testes](testes.md). **A existência deste relatório não comprova execução.**

Escopo acadêmico: o usuário informou a aceitação do fluxo integrado de consulta, conforme equipe.md. Esta implementação foi escolhida para consulta, sem remoção de nós pela interface. Critérios docentes adicionais não transcritos não são inventados neste relatório.

## 3 Conclusão

A AVL torna explícita a passagem dos dados persistidos por uma árvore antes da consulta. A utilidade demonstrada é organização e busca ordenada no aplicativo, com custo e limites descritos. O critério final exige inspecionar a implementação e executar seus testes e o fluxo real; resultados pendentes não devem ser apresentados como aprovados.

## Referências

ADEL'SON-VEL'SKII, G. M.; LANDIS, E. M. An algorithm for organization of information. **Doklady Akademii Nauk SSSR**, Moscou, v. 146, n. 2, p. 263-266, 1962. Disponível em: https://www.mathnet.ru/eng/dan/v146/i2/p263. Acesso em: 24 nov. 2026. Texto em russo; título traduzido conforme o catálogo Math-Net.Ru.

E-WASTE. **ArvoreAvl e fluxo de consulta de pontos**. Goiânia, 2026. Código-fonte em Java, diretório src/main/java/br/com/ewaste/mapa. Disponível em: https://github.com/mmsxdev/plataforma-ewaste. Acesso em: 26 nov. 2026.

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. 9 p. Documento fornecido à equipe, arquivo PI2026-2 ENG4-T1.pdf.

LIMA, Luciano. **Árvores balanceadas**: AVL vs. rubro-negras. Goiânia: SENAI, [2026]. 14 p. Apostila da disciplina Estrutura de Dados II, aula 07.
