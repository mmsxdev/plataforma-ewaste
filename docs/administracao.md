# Fundamentos da Administração — E-Waste

Entrega baseada nas pp.6–7 do PDF oficial. Identidade aprovada pela equipe e funções definidas por autorização do usuário em 24/11/2026. Custos continuam estimativas didáticas, não despesas pagas ou salários reais. Referência canônica: [equipe e decisões](equipe.md).

## Identidade e planejamento estratégico

Nome comercial aprovado pela equipe: **E-Waste** (marca já utilizada no projeto). Assinatura: “resíduos com destino”. O logotipo existente em `src/main/resources/pdf/logo.png` é reutilizado na página inicial; não há pesquisa de registro de marca, titularidade externa ou exclusividade comprovada.

- **Missão:** facilitar o descarte responsável de eletrônicos, conectando empresas a pontos de recebimento e coletoras e tornando cada solicitação acompanhável.
- **Visão:** ser uma referência acadêmica de solução simples e rastreável para organizar o descarte eletrônico na comunidade atendida.
- **Valores:** responsabilidade ambiental; clareza das informações; proteção de dados; acessibilidade; colaboração; compromisso com evidências.

Missão descreve a finalidade atual; Visão é uma aspiração, sem promessa de liderança de mercado; Valores orientam decisões concretas: alternativa textual aos mapas, documentos explicitamente simulados e restrição de acesso por empresa. Estes elementos estão visíveis na página inicial do protótipo.

## Organograma e responsabilidades definidas

Grupo 01, Engenharia de Software, 4º período, turma 1: Davi Fraga, Guilherme Gonçalves, Miguel Melo, Felipe Rohnelt e Rafael Camargo. Matrículas dispensadas. Funções definidas com a autorização do usuário; não são alegações sobre contribuições já realizadas.

Fonte gráfica: [uml/organograma.puml](uml/organograma.puml); [PNG](uml/organograma.png). Responsabilidades e revisão cruzada detalhadas em [equipe.md](equipe.md).

```text
Coordenação e Design — Miguel Melo
├── Produto, solicitações e EDII — Davi Fraga
├── Rastreabilidade, documentos e Teoria — Guilherme Gonçalves
├── Qualidade, armazenamento e recuperação — Felipe Rohnelt
└── Banco de Dados II, Administração e custos — Rafael Camargo
```

Miguel coordena dependências e a entrega; os focos colaboram com essa coordenação, sem vínculo empregatício presumido. Todos estudam o projeto completo e participam de revisão cruzada. Rafael é operador do banco/migrations; Felipe responde pelas cópias e ensaios de recuperação. A futura anfitriã será o computador de Felipe, mas a transferência e o ensaio de rede ainda não foram executados; backup nesse mesmo computador não conta como cópia externa. A participação de cinco integrantes foi confirmada pelo usuário como aceita pelos professores, mantendo-se a divergência original do PDF registrada em requisitos.md.

## Custos e premissas

Fonte editável: [custos-premissas.csv](custos-premissas.csv). Planilha calculável gerada: [custos.xlsx](custos.xlsx), com fórmulas e valores em cache conferidos por leitura openpyxl em 24/11/2026. Valores em BRL, **estimativas didáticas sem cotação**, sem comprovação de desembolso. Não há salários reais de integrantes.

Desenvolvimento é estimado por esforço total × taxa didática; o número de horas não atesta horas trabalhadas nem distribui contribuição individual. Operação mensal distingue base fixa e consumo variável; investimento inicial não deve ser somado novamente a cada mês. Equipamentos existentes são considerados recurso disponível com custo incremental zero, sem alegar que não têm custo econômico/depreciação.

PostgreSQL e uploads binários no banco compartilhado evitam contratar storage externo no protótipo. Isso aumenta backup/volume e tem limite educacional; crescimento de arquivos pode tornar armazenamento de objetos mais adequado. Reserva de contingência é premissa explícita, calculada sobre a base indicada, não uma despesa ocorrida. Cenários devem permitir mudar horas, taxa, consumo e meses sem editar totais manualmente.

## Referências

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. p.6–7.

## Conferência dos artefatos

A premissa aprovada é 80 horas de desenvolvimento a R$120/h: base R$9.600, reserva de 10% de R$960 e total de R$10.560. Operação permanece R$120/mês e o horizonte de três meses totaliza R$10.920. Suporte operacional continua estimado em R$30/h. Os novos valores foram conferidos nas fórmulas e no cache do arquivo exportado após edição pontual da taxa de desenvolvimento. Valores são estimativas didáticas, não despesas realizadas. Fórmulas e cache foram conferidos em `docs/custos.xlsx`; a prévia visual está em `target/academic/custos-preview.png`. O E2E de 24/11 verificou a visibilidade de identidade/Missão/Visão/Valores na home e o recorte móvel; [imagem](evidencias/01-home.png). Isso comprova integração visual; a equipe aprovou a identidade em 24/11/2026. Não constitui aprovação docente do artefato nem registro comercial da marca.
