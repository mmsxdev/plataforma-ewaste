# Apresentação dos relatórios acadêmicos

Formato definido pelo usuário em 24/11/2026: ABNT, com capa, folha de rosto (chamada de contracapa na conversa) e sumário. Grupo 01, Engenharia de Software, 4º período, turma 1; sem matrículas. Dados da equipe em [equipe](equipe.md).

Os cinco relatórios possuem capa, folha de rosto com natureza do trabalho, resumo, abstract, sumário gerado a partir dos títulos e páginas reais, corpo técnico, referências e apêndices quando houver. A capa não entra na contagem; as folhas são contadas desde a folha de rosto e os números aparecem a partir do texto. Não há assinatura, nota ou banca presumida.

O gerador usa A4, margens superior/esquerda de 3 cm e inferior/direita de 2 cm, fonte Arial 12 no corpo, espaçamento de 1,5 e tamanhos menores uniformes em tabelas, códigos e legendas. Referências e natureza do trabalho usam espaço simples. Títulos e indicativos são organizados hierarquicamente. Fontes Markdown continuam sendo as fontes técnicas; a exportação não altera código nem reexecuta testes.

As referências dos relatórios devem continuar relacionadas às afirmações utilizadas. O formato aplicado e a conferência visual não equivalem a avaliação ou aprovação docente. Não foi fornecido um modelo institucional adicional nem a URL da pasta de submissão. Diagramas largos possuem visão geral no PDF e arquivo PNG original para leitura detalhada; o dicionário textual preserva os atributos do banco.

## Fontes consultadas em 24/11/2026

- ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. **ABNT NBR 14724:2024: Informação e documentação — Trabalhos acadêmicos — Apresentação**. Versão corrigida em 1 abr. 2025. Disponível na [UFES](https://engenhariaedesenvolvimentosustentavel.ufes.br/sites/engenhariaedesenvolvimentosustentavel.ufes.br/files/field/anexo/abnt_nbr_14724.pdf).
- UNIVERSIDADE FEDERAL DO ESPÍRITO SANTO. **Normalização**. Orientações sobre NBR 14724:2024, NBR 10520:2023 e NBR 6023:2025. Disponível em: https://biblioteca.ufes.br/normalizacao.
- UNIVERSIDADE FEDERAL DO ESPÍRITO SANTO. **Normas**. Identificação da NBR 6027:2012 para sumário. Disponível em: https://sibpesquisa.ufes.br/normas.html.

## Reprodução

`python -B -X utf8 tools/gerar-relatorios.py` atualiza os cinco relatórios. O seletor `--only` preserva relatórios não selecionados; mudanças comuns nas capas ou no formato exigem regenerar os cinco. O sumário usa múltiplas passagens do ReportLab; páginas não são digitadas manualmente. Fontes, diagramas e gerador têm hashes no manifesto em docs/pdf/fontes-sha256.json.
