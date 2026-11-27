# Apresentação dos relatórios acadêmicos

Formato definido pela equipe em 24/11/2026: ABNT, com capa, folha de rosto (também chamada de contracapa) e sumário. Grupo 01, Engenharia de Software, 4º período, turma 1; sem matrículas. Dados da equipe em [equipe](equipe.md).

Os cinco relatórios possuem capa, folha de rosto com natureza do trabalho, resumo, abstract, sumário gerado a partir dos títulos e páginas reais, corpo técnico, referências e apêndices quando houver. A capa não entra na contagem; as folhas são contadas desde a folha de rosto e os números aparecem a partir do texto. Não há assinatura, nota ou banca presumida.

O gerador usa A4, margens superior/esquerda de 3 cm e inferior/direita de 2 cm, fonte Arial 12 no corpo, espaçamento de 1,5 e tamanhos menores uniformes em tabelas, códigos e legendas. Referências e natureza do trabalho usam espaço simples. Títulos e indicativos são organizados hierarquicamente. Fontes Markdown continuam sendo as fontes técnicas; a exportação não altera código nem reexecuta testes.

Títulos sem indicativo numérico, como REFERÊNCIAS e APÊNDICE, são centralizados. Figuras e tabelas recebem identificação sequencial e título acima, com fonte abaixo. A legenda não mantém toda uma tabela longa no mesmo bloco: cabeçalhos se repetem e a tabela pode continuar na página seguinte. A conferência visual após exportação deve verificar títulos, primeira linha, continuidade e fontes, inclusive onde a paginação mudar.

As referências dos relatórios devem continuar relacionadas às afirmações utilizadas. O formato aplicado e a conferência visual não equivalem a avaliação ou aprovação docente. Não foi fornecido um modelo institucional adicional nem a URL da pasta de submissão. Diagramas largos possuem visão geral no PDF e arquivo PNG original para leitura detalhada; o dicionário textual preserva os atributos do banco.

## Fontes consultadas em 24/11/2026

- ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. **ABNT NBR 14724:2024: Informação e documentação — Trabalhos acadêmicos — Apresentação**. Versão corrigida em 1 abr. 2025. Disponível na [UFES](https://engenhariaedesenvolvimentosustentavel.ufes.br/sites/engenhariaedesenvolvimentosustentavel.ufes.br/files/field/anexo/abnt_nbr_14724.pdf).
- UNIVERSIDADE FEDERAL DO ESPÍRITO SANTO. **Normalização**. Orientações sobre NBR 14724:2024, NBR 10520:2023 e NBR 6023:2025. Disponível em: https://biblioteca.ufes.br/normalizacao.
- UNIVERSIDADE FEDERAL DO ESPÍRITO SANTO. **Normas**. Identificação da NBR 6027:2012 para sumário. Disponível em: https://sibpesquisa.ufes.br/normas.html.

## Revisão complementar de 26/11/2026

A revisão usa a NBR 10520:2023 para chamadas autor-data com maiúsculas e minúsculas e a NBR 6028:2021 para resumo e palavras-chave. Siglas e nomes próprios mantêm sua grafia, como AVL e PostgreSQL; palavras-chave comuns usam inicial minúscula, são separadas por ponto e vírgula e terminam em ponto. A extensão dos resumos deve ser conferida após mudanças de conteúdo, considerando a recomendação de 150 a 500 palavras para trabalhos acadêmicos e relatórios técnicos.

Nas referências de artigos, o destaque tipográfico identifica o periódico. O texto do artigo RSA é disponibilizado por um dos autores; os dados bibliográficos já utilizados são preservados e a localização editorial não identificada recebe `[S. l.]`, sem confundir o endereço do autor com o local da publicação. Observações sobre a revisão ficam neste guia, em vez de serem exportadas como itens da lista bibliográfica.

Fontes conferidas na revisão de 26/11/2026:

- ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. **ABNT NBR 10520:2023: Informação e documentação — Citações em documentos — Apresentação**. Texto disponibilizado pela Universidade Federal Fluminense: https://mccicm.uff.br/wp-content/uploads/sites/79/2026/03/ABNT_NBR_10520_-2023_Informacao-e-documentacao-%E2%80%94-Citacoes-em-documentos.pdf. Acesso em: 26 nov. 2026.
- ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. **ABNT NBR 6028:2021: Informação e documentação — Resumo, resenha e recensão — Apresentação**. Texto disponibilizado pela Universidade Federal da Paraíba: https://www.ufpb.br/secretariado/contents/documentos/2021_ABNT6028Resumo.pdf. Acesso em: 26 nov. 2026.
- UNIVERSIDADE FEDERAL DE VIÇOSA. **Normalização de trabalhos acadêmicos**. Orientações de apresentação atualizadas para NBR 14724:2024: https://bbt.caf.ufv.br/wp-content/uploads/2025/11/Normalizacao-de-trabalhos-academicos-2025-UFV.pdf. Acesso em: 26 nov. 2026.
- UNIVERSIDADE FEDERAL DO CEARÁ. **Revista de Ciências Sociais: submissão**. Orientação para título do artigo sem destaque e nome do periódico destacado: https://periodicos.ufc.br/revcienso/about/submissions. Acesso em: 26 nov. 2026.
- RIVEST, Ronald L.; SHAMIR, Adi; ADLEMAN, Leonard. A method for obtaining digital signatures and public-key cryptosystems. **Communications of the ACM**, [S. l.], v. 21, n. 2, p. 120-126, 1978. Cópia disponibilizada pelo autor: https://people.csail.mit.edu/rivest/Rsapaper.pdf. Acesso em: 26 nov. 2026.

### Fonte da disciplina ainda não conferida diretamente

O PR #23 de Davi, commit `5971031`, acrescentou a referência de Luciano Lima, **Árvores balanceadas: AVL vs. rubro-negras**, com data `[2026]`, 14 páginas e chamadas às páginas 3 e 5. Essa contribuição e sua referência são preservadas. O arquivo da apostila não foi localizado no repositório nesta revisão; portanto título, autoria, extensão, data inferida e correspondência dessas páginas ficam **não verificados diretamente** até a conferência da fonte. Não se inventam outros metadados nem se declara que o professor aprovou a normalização.

## Reprodução

`python -B -X utf8 tools/gerar-relatorios.py` atualiza os cinco relatórios. O seletor `--only` preserva relatórios não selecionados; mudanças comuns nas capas ou no formato exigem regenerar os cinco. O sumário usa múltiplas passagens do ReportLab; páginas não são digitadas manualmente. Fontes, diagramas e gerador têm hashes no manifesto em docs/pdf/fontes-sha256.json.
