# Roteiro de coordenação e estudo — Miguel

Planejamento de 26/11/2026. Entrega em 05/12/2026 às 23h59 e apresentação em 07/12/2026. A presença e a avaliação individual seguem o documento oficial; não registrar estudo ou presença como realizado automaticamente.

## Trilha principal no portal

Portal: https://ensino-ewaste.rohnelt.dev . O usuário confirmou acesso e troca da senha em 26/11/2026. A senha não faz parte do repositório. O agente não teve acesso autenticado ao conteúdo após a troca; títulos internos e conclusões das aulas não foram presumidos.

| Dia | Atividade humana | Evidência de aprendizado a preparar |
| --- | --- | --- |
| 26/11 | Primeira das seis camadas, conforme ordem do portal | Resumir responsabilidade e apontar um exemplo real no repositório. |
| 27/11 | Segunda camada, conforme ordem do portal | Explicar entradas, saídas e comunicação com a camada anterior. |
| 28/11 | Terceira camada, conforme ordem do portal | Traçar um requisito até a persistência e localizar um teste. |
| 29/11 | Quarta camada, conforme ordem do portal | Explicar autenticação/autorização e uma rejeição por regra. |
| 30/11 | Quinta camada, conforme ordem do portal | Relacionar o fluxo implementado aos diagramas e contratos. |
| 01/12 | Sexta camada, conforme ordem do portal | Explicar o caminho completo e seus limites sem consultar o código. |

A ordem do portal prevalece; os resultados da terceira coluna são perguntas de revisão, não títulos inventados das aulas. Cada camada permanece sem comprovação de estudo até Miguel realizá-la.

## Aprofundamento do foco até 03/12

Revisar as aulas 0, 1 e 7 indicadas pela equipe, com atenção a arquitetura, segurança, login e UML. Em 02/12, revisar as aulas 0 e 1; em 03/12, revisar a aula 7 e fazer um ensaio geral. Essa distribuição é planejamento, não registro de aula assistida.

1. **Arquitetura:** explicar monólito modular e comunicação por `api`/IDs. Usar `docs/arquitetura.md`, `docs/contratos.md`, `src/main/java/br/com/ewaste/` e `ArquiteturaTest`.
2. **Login e identidade:** seguir `UsuarioController` → Spring Security → `IdentidadeService`, localizar BCrypt e o bloqueio de contas inativas. Conferir `SecurityConfig`, `AcessoIT` e `IdentidadeIT`.
3. **Autorização:** distinguir estar autenticado, possuir papel e ser dono do recurso. Explicar por que ocultar um link não protege o documento de outra coletora. Usar `HomologacaoControllersTest`, os controllers de homologação e o ensaio de navegador de homologação.
4. **UML e transações:** mostrar `classes.puml`, `sequencia.puml` e `sequencia-homologacao.puml`; apontar onde ocorre a publicação do evento, o commit e o consumo em `AlertaAoHomologar` com `REQUIRES_NEW`.
5. **Decisão de design:** justificar preservar os módulos, usar PostgreSQL/arquivos compartilhados e separar conta de aplicação de operador. Diferenciar ambiente local testado, relato de restauração no Felipe e VPS ainda não validada.

Perguntas de ensaio: quem decide se uma coletora está apta? Por que uma aprovação pode gerar oportunidade de um pedido antigo? O que ocorre fora do raio? Como evitar resposta antiga de CEP sobrescrevendo a localização? Por que o download fora do escopo retorna 404? Qual teste comprova cada resposta?

## Cobranças da coordenação

Foram solicitadas cobranças em 30/11, 02/12 e 04/12, às 14h de Brasília. Canal principal: presencial; Discord é alternativa, sem servidor/canal informado. O lembrete nesta conversa prepara a cobrança; não significa que alguém foi contatado.

| Dia | Texto de apoio para falar ou encaminhar |
| --- | --- |
| 30/11 | Pessoal, vamos conferir as entregas de hoje. Cada um mostra o PR ou arquivo da sua parte, o teste que rodou e o que ainda está travado. Precisamos deixar os contratos e a documentação coerentes com o código. |
| 02/12 | Pessoal, hoje precisamos fechar os diagramas e conferir o fluxo integrado. Cada um mostra como sua parte aparece na UML e qual teste comprova o comportamento. Vamos registrar os impedimentos e quem resolve cada um. |
| 04/12 | Pessoal, vamos fazer a conferência final: PDFs, código, evidências e roteiro da apresentação. Confiram a própria parte e ensaiem a explicação. A entrega é amanhã até 23h59; precisamos validar o acesso à pasta da professora. |

Não atribuir contribuição passada apenas por haver nome no organograma. Registrar o que cada pessoa apresentar e os links/evidências efetivamente entregues.
