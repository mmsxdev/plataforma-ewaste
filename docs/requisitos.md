# Requisitos e escopo do PI 2026-2

Fonte normativa acadêmica: Faculdade SENAI FATESG, **PI2026-2 ENG4-T1**, 9 páginas, semestre 2026-2. As páginas abaixo são físicas, contadas a partir da capa. Este documento registra requisitos; a comprovação está em [testes](testes.md) e [rastreabilidade](rastreabilidade.md).

## Problema, usuários e limites

Dificuldade de identificar locais e materiais aceitos, registrar o descarte e acompanhar sua situação (PDF, pp.1–3). O protótipo conecta geradoras a pontos de recebimento e coletoras e organiza os materiais e solicitações. A base existente é B2B: usuário responsável representa empresa GERADORA ou COLETORA; ADMIN é perfil reservado; as interfaces de administração de homologações/pontos e financeiro não estão completas nesta entrega. A aceitação desse recorte pelo professor está **pendente**: o PDF não exige CNPJ nem exclui pessoas físicas.

Preservamos Java 21, Spring Boot 3.5.16, Thymeleaf e PostgreSQL 16. Homologação, roteamento por raio, PDFs operacionais e monetização simulada são decisões anteriores, não requisitos mínimos do documento oficial. Mapa cartográfico, pagamentos, IA, publicação na internet e aplicativo nativo não são obrigações. A consulta textual de pontos atende ao objetivo de localizar informações sem depender de um provedor de mapas.

## Requisitos funcionais e histórias

| ID / PDF | História e comportamento | Critérios de aceitação |
| --- | --- | --- |
| RF01 / pp.2–3 | Como responsável, quero cadastrar minha empresa e autenticar minha conta. | CA01: cadastro válido grava empresa/usuário atomicamente, senha BCrypt; duplicidade/credencial inválida recusada; conta inativa não entra. |
| RF02 / p.2 | Como geradora, quero identificar materiais, tipo, quantidade e peso ao solicitar descarte. | CA02: todo novo item informado, com ou sem ponto, exige identificação; material nomeado e categoria válida aparecem após persistência; quantidade/peso inválidos são recusados; itens legados sem identificação continuam legíveis. |
| RF03 / pp.2–3 | Como geradora, quero classificar materiais em categorias. | CA03: Informática, Telefonia, Eletrodoméstico, Mídia de armazenamento, Bateria e Outro são consistentes no formulário, enum e banco. |
| RF04 / p.3 | Como usuário, quero consultar pontos ativos por cidade e categoria. | CA04: consulta informa nome, local, materiais e horário; filtros retornam apenas compatíveis; lista vazia tem mensagem; dados do banco passam pela AVL. |
| RF05 / p.3 | Como geradora, quero iniciar uma solicitação a partir de um ponto consultado. | CA05: ponto/categoria seguem para o formulário; ponto inativo ou incompatível é recusado; solicitação persistida referencia o ponto escolhido. Registro legado sem ponto é preservado. |
| RF06 / p.3 | Como geradora, quero acompanhar situação e histórico. | CA06: detalhe apresenta estado e eventos da própria solicitação; outra geradora não acessa; conclusão mantém a linha do tempo. |
| RF07 / p.3 | Como geradora, quero corrigir informações antes da execução. | CA07: dona pode editar descrição e identificação dos itens somente em ABERTA; outra empresa e estados posteriores são recusados; categoria, quantidade, local e ponto já roteados não são alterados por essa edição. |
| RF08 / p.3 | Como usuário, quero acessar somente dados autorizados. | CA08: papéis e propriedade restringem solicitações/documentos/evidências; logout e formulários usam proteção CSRF. |
| RF09 / p.3 | Como usuário, quero navegar de forma simples e acessível. | CA09: rótulos e mensagens compreensíveis, navegação por teclado, apresentação em tela pequena e alternativa textual ao mapa; validação de acessibilidade registrada, sem alegação automática de conformidade. |
| RF10 / decisão existente | Como coletora vencedora, quero aceitar, agendar, registrar retirada/destinação e concluir. | CA10: primeira coletora apta assume; transições inválidas recusadas; conclusão exige coleta, destinação e todas as mídias declaradas. |

## Regras de negócio

- RN01: e-mail normalizado e CNPJ não podem duplicar; cadastro de ADMIN não é público.
- RN02: identidade autenticada, perfil e empresa proprietária devem ser verificados em operações privadas.
- RN03: ponto escolhido deve estar ativo e aceitar a categoria; falha não cria solicitação parcial.
- RN04: solicitação segue ABERTA → ACEITA → AGENDADA → COLETADA → CONCLUIDA; ABERTA pode ser cancelada/expirada conforme domínio existente.
- RN05: descrição/identificação só podem ser corrigidas pela geradora dona enquanto ABERTA. Alteração do que já foi roteado requer fluxo próprio futuro.
- RN06: quantidade e peso devem ser positivos e respeitar precisão persistida. Mídias declaradas precisam estar totalmente registradas antes da conclusão.
- RN07: só a coletora vencedora altera a coleta; só a geradora dona baixa documentos. Documentos são simulados e não têm validade legal.

## Requisitos não funcionais

| ID | Exigência/decisão e critério verificável |
| --- | --- |
| RNF01 | Segurança (PDF p.3): BCrypt, autorização por papel e propriedade, CSRF; testes de rejeição. Segredos fora do Git. |
| RNF02 | Integridade: Flyway aditivo, FK/check/unique, transações e controle de concorrência; rollback não deixa operação parcial. |
| RNF03 | Usabilidade/acessibilidade (PDF p.3): CA09, sem presumir atendimento por existir HTML responsivo. |
| RNF04 | Reprodutibilidade (PDF p.5): comandos, versões, configuração e dados fictícios documentados. |
| RNF05 | Operação da equipe (decisão aprovada): PostgreSQL e uploads compartilhados, modo team sem migração automática e sem credenciais padrão. Endereço real e ensaio em duas máquinas pendentes. |
| RNF06 | Recuperação (PDF p.4): política de backup incluindo banco e arquivos, metas RTO/RPO justificadas e restauração medida; metas não são resultado obtido. |

## Dúvidas e inconsistências preservadas

1. Apresentação: p.7 diz **07/12/2025**; p.8 diz **07/12/2026**. Confirmar; não corrigimos a fonte.
2. Controle: p.7 diz **25/12/2026 e 02/12/2026**; a primeira data é posterior à entrega. Confirmar.
3. Grupo: p.8 exige 3–4 integrantes e lista grupos com cinco. Confirmar exceção para a equipe; não atribuir cargos automaticamente.
4. Confirmar recorte B2B; suficiência de um modo de solicitação (“entrega ou coleta”); abrangência da árvore (um fluxo real ou toda consulta) e exigência de implementação própria; critérios específicos EDII/BDII.
5. Estrutura de arquivos da p.6 é sugestão. Manter organização equivalente; PDFs acadêmicos ficam em docs/pdf, distintos dos modelos operacionais src/main/resources/pdf.

Prazo expresso: 23/11/2026 a 05/12/2026; entrega até 23h59 de 05/12 (p.8). Avaliação geral 50% grupo/50% individual; ausência desconta 10% individual (fórmula e acumulação a confirmar). Não enviamos mensagens aos professores.

## Referência

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. 9 p. Documento fornecido à equipe, arquivo PI2026-2 ENG4-T1.pdf.
