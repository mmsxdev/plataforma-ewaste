# Fundamentos da Administração — E-Waste

Entrega baseada nas pp.6–7 do PDF oficial. **Proposta para validação da equipe**, sem inventar cargos atribuídos, despesas pagas ou aprovação comercial da marca.

## Identidade e planejamento estratégico

Nome comercial proposto: **E-Waste** (marca já utilizada no projeto). Assinatura: “resíduos com destino”. O logotipo existente em `src/main/resources/pdf/logo.png` é reutilizado na página inicial; não há pesquisa de registro de marca, titularidade externa ou exclusividade comprovada.

- **Missão:** facilitar o descarte responsável de eletrônicos, conectando empresas a pontos de recebimento e coletoras e tornando cada solicitação acompanhável.
- **Visão:** ser uma referência acadêmica de solução simples e rastreável para organizar o descarte eletrônico na comunidade atendida.
- **Valores:** responsabilidade ambiental; clareza das informações; proteção de dados; acessibilidade; colaboração; compromisso com evidências.

Missão descreve a finalidade atual; Visão é uma aspiração, sem promessa de liderança de mercado; Valores orientam decisões concretas: alternativa textual aos mapas, documentos explicitamente simulados e restrição de acesso por empresa. Estes elementos estão visíveis na página inicial do protótipo.

## Organograma proposto

Nomes dos responsáveis **PENDENTES de confirmação**. Os donos técnicos citados historicamente no README não equivalem automaticamente a cargos de uma empresa. Acúmulo de funções e hierarquia devem ser acordados pelo grupo.

Fonte do organograma gráfico: [uml/organograma.puml](uml/organograma.puml); [PNG renderizado](uml/organograma.png). Atribuições nominais continuam pendentes.

```text
Coordenação de projeto [PENDENTE]
├── Produto e requisitos [PENDENTE] — escopo, critérios de aceitação e validação
├── Engenharia e dados [PENDENTE] — arquitetura, código, banco e revisão
├── Qualidade e operação [PENDENTE] — testes, implantação e recuperação
└── Administração e identidade [PENDENTE] — marca, estratégia e custos
```

Coordenação organiza dependências e decisão compartilhada; os papéis acima não presumem vínculo empregatício. Revisão cruzada mantém responsabilidade de cada integrante. Documento institucional lista grupos com cinco pessoas apesar de regra de 3–4: confirmação de exceção permanece pendente.

## Custos e premissas

Fonte editável: [custos-premissas.csv](custos-premissas.csv). Planilha calculável gerada: [custos.xlsx](custos.xlsx), com fórmulas e valores em cache conferidos por leitura openpyxl em 24/11/2026. Valores em BRL, **estimativas didáticas sem cotação**, sem comprovação de desembolso. Não há salários reais de integrantes.

Desenvolvimento é estimado por esforço total × taxa didática; o número de horas não atesta horas trabalhadas nem distribui contribuição individual. Operação mensal distingue base fixa e consumo variável; investimento inicial não deve ser somado novamente a cada mês. Equipamentos existentes são considerados recurso disponível com custo incremental zero, sem alegar que não têm custo econômico/depreciação.

PostgreSQL e uploads binários no banco compartilhado evitam contratar storage externo no protótipo. Isso aumenta backup/volume e tem limite educacional; crescimento de arquivos pode tornar armazenamento de objetos mais adequado. Reserva de contingência é premissa explícita, calculada sobre a base indicada, não uma despesa ocorrida. Cenários devem permitir mudar horas, taxa, consumo e meses sem editar totais manualmente.

## Referências

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. p.6–7.

## Conferência dos artefatos

A planilha calculou R$2640 de desenvolvimento com reserva, R$120/mês de operação e R$3000 no horizonte de três meses. Valores são estimativas didáticas, não despesas realizadas. Fórmulas e cache foram conferidos em `docs/custos.xlsx`; a prévia visual está em `target/academic/custos-preview.png`. O E2E de 24/11 verificou a visibilidade de identidade/Missão/Visão/Valores na home e o recorte móvel; [imagem](evidencias/01-home.png). Isso comprova integração visual, sem substituir a aprovação da proposta por integrantes ou professor.
