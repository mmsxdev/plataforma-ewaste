# Equipe, responsabilidades e decisões confirmadas

Registro de 24/11/2026, baseado nas confirmações do usuário nesta conversa. Esta página é a referência da composição, dos focos de trabalho e das decisões da equipe; não comprova execução passada nem substitui evidências de testes.

## Identificação acadêmica

Faculdade SENAI FATESG — Engenharia de Software, 4º período, turma 1, semestre 2026-2. Grupo 01: **Davi Fraga; Guilherme Gonçalves; Miguel Melo; Felipe Rohnelt; Rafael Camargo**. Composição confirmada; matrículas não devem constar. O usuário autorizou a definição das funções abaixo e aprovou nome E-Waste, logotipo existente, assinatura “resíduos com destino” e Missão, Visão e Valores de [Administração](administracao.md).

## Funções e áreas principais de foco

| Integrante | Função principal | Responsabilidades planejadas e foco de estudo | Revisão cruzada principal |
| --- | --- | --- | --- |
| Miguel Melo | Coordenação e design de software | Organizar dependências/entregas; manter arquitetura, histórias integradas e UML; identidade, autenticação e segurança. | Revisar com Rafael segurança, permissões e coerência dos relatórios. |
| Davi Fraga | Produto, solicitações e Estrutura de Dados II | Requisitos e critérios de aceitação; solicitação/roteamento; integração banco→AVL e demonstração de suas operações e limites. | Revisar com Guilherme transições, histórico e rejeições. |
| Guilherme Gonçalves | Rastreabilidade, documentos e Teoria da Computação | Coleta/destinação, documentos simulados e histórico; formalização lógica e demonstração matemática de criptografia, separada da segurança real. | Revisar com Davi sequência do fluxo e vínculo regra/teste. |
| Felipe Rohnelt | Qualidade, armazenamento e recuperação | Planejar/executar testes; conferir arquivos compartilhados; produzir/verificar cópias e ensaios de restauração. Disponibilizar a futura anfitriã. | Revisar com Rafael SQL, backup e recuperação; com Miguel o ensaio integrado. |
| Rafael Camargo | Banco de Dados II, administração e custos | Operar o banco/migrador autorizado; conferir modelo, SQL e privilégios; manter organograma, identidade e estimativas de custos. | Revisar com Felipe recuperação e com Miguel arquitetura e entrega. |

Miguel coordena prazos e dependências; os quatro focos técnicos se articulam com a coordenação, sem presumir relação empregatícia. Decisões relevantes de escopo e entrega são compartilhadas. **Todos deverão estudar e conseguir explicar o fluxo completo**, não apenas o próprio foco. Os focos complementam a divisão histórica de módulos do README; mudanças entre módulos continuam coordenadas por APIs/revisão.

As funções são uma distribuição de responsabilidade para o fechamento, autorizada pelo usuário. Não representam horas já trabalhadas, autoria de commits ou contribuição acadêmica concluída. Cada integrante deve registrar sua contribuição real e ensaiar a relação entre requisito, modelagem, código e teste antes da avaliação individual.

## Operação e mudança de anfitriã

**Rafael é o operador responsável pelo banco e pelas migrations; Felipe é o responsável pelas cópias e pelos ensaios de recuperação.** As credenciais de operador permanecem separadas das credenciais limitadas dos aplicativos e não são distribuídas no repositório.

O usuário escolheu **o computador de Felipe como futura anfitriã do PostgreSQL compartilhado** e máquina participante do ensaio de rede. A transferência ainda não foi executada. A instância atual em Miguel permanece preservada até backup, restauração isolada, validação de dados/bytes/privilégios e troca coordenada das conexões. Não operar duas cópias como bancos comuns divergentes. Nenhum novo IP, VPN ou regra de firewall foi confirmado/testado por esta decisão.

Como Felipe também será a anfitriã, guardar backup apenas em seu computador **não é cópia externa**. O destino protegido fora dele permanece pendente, assim como teste real entre computadores, acesso restrito e ensaio de recuperação no ambiente definitivo. [Operação](operacao-banco.md) e [implantação](implantacao.md) descrevem os limites e a sequência.

## Confirmações acadêmicas e entrega

O usuário informou em 24/11/2026 que os professores confirmaram as interpretações anteriormente registradas: composição com cinco integrantes, recorte B2B, fluxo de coleta e AVL integrada ao fluxo real de consulta. Isso é uma confirmação relatada pelo usuário, sem criar rubricas, notas ou declarações assinadas em nome dos docentes. Critérios específicos não transcritos continuam sem detalhamento adicional.

Calendário adotado por confirmação explícita do usuário: apresentação em **07/12/2026**; pontos de controle em **25/12/2026 e 02/12/2026**; entrega até **05/12/2026 às 23h59**. A data de 25/12 permanece posterior à entrega por confirmação, sem substituição por uma data presumida. O PDF original conserva a apresentação 07/12/2025 na p.7 e 07/12/2026 na p.8; a divergência de origem não foi apagada.

Modelo solicitado pelo usuário: **ABNT, capa, folha de rosto (contracapa) e sumário**. A revisão dos artefatos deve conferir esse formato, os cinco nomes e a turma. O link da pasta compartilhada para submissão ainda não foi fornecido. Não houve envio ao professor nem publicação de dados reais.

## Dados e custos

Uso exclusivamente acadêmico, com contas, empresas, materiais e mídias fictícios; **sem implantação em produção nem uso planejado de dados pessoais reais**. O prazo de retenção dos dados de demonstração ainda não foi definido. Esta decisão não elimina a análise acadêmica de dados pessoais/segurança nem constitui alegação geral de conformidade jurídica.

Taxa didática de desenvolvimento aprovada: **R$120/h**, mantendo 80 horas totais estimadas e reserva de 10%. Desenvolvimento base R$9.600 + reserva R$960 = **R$10.560**. Operação permanece **R$120/mês** e horizonte de três meses totaliza **R$10.920**. A taxa de suporte operacional permanece R$30/h. Não são salários reais, horas realizadas ou despesas comprovadas; fórmulas e premissas estão na [planilha](custos.xlsx) e no [CSV](custos-premissas.csv).
