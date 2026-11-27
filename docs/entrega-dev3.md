# Dev 3 — Solicitação e roteamento (Davi)

Módulos `solicitacao` e `roteamento`, rotas `/geradora/solicitacoes` e `/coletora/oportunidades`, tabelas `solicitacao`, `solicitacao_item` e `alerta_solicitacao`, migrações na faixa V300–V399. Contratos publicados em [contratos.md](contratos.md): `SolicitacaoConsulta`, `TransicaoSolicitacao` e os eventos de criação, alerta, aceite e agendamento.

## Como explicar o fluxo

1. A geradora preenche a solicitação: materiais, itens, local da coleta (CEP, mapa ou coordenadas). `SolicitacaoService.criar` grava a solicitação e os itens numa transação. Ponto inativo ou categoria incompatível é recusado sem gravação parcial.
2. Depois do commit, `RoteamentoListener` recebe `SolicitacaoCriadaEvent` (AFTER_COMMIT, transação própria). `FiltroRegiao` escolhe as coletoras homologadas cujo raio alcança o local, da mais perto para a mais longe; distância igual ao raio entra. Cada uma recebe um alerta. Sem nenhuma, a geradora vê "Nenhuma coletora apta na sua região".
3. Quando uma coletora é aprovada depois, `AlertaAoHomologar` gera alertas retroativos das solicitações ABERTAS dentro do raio dela.
4. A coletora alertada vê a oportunidade em `/coletora/oportunidades` e aceita. O aceite é um único `UPDATE ... WHERE id = :id AND status = 'ABERTA'`: retorna 1 para quem venceu e 0 para quem chegou depois. O banco garante que só uma coletora assume, sem trava na aplicação.
5. A vencedora agenda e pode reagendar a retirada. Daí em diante o módulo `rastreio` (Dev 4) registra coleta e destinação, usando `TransicaoSolicitacao`.
6. Todo status passa por `TransicoesSolicitacao`: ABERTA → ACEITA → AGENDADA → COLETADA → CONCLUIDA, e ABERTA → CANCELADA ou EXPIRADA. Qualquer outra transição lança `TransicaoInvalidaException`.

A geradora edita descrição e identificação dos itens somente enquanto a solicitação está ABERTA; depois do aceite a edição devolve HTTP 409.

## Roteiro manual

1. Perfil `dev`. Entre como `geradora@teste.com` (senha `senha123`, conta fictícia do seed) e abra **Solicitações**. A de 250 kg com 5 HDs está ABERTA e alertada; a de Campinas mostra que não há coletora apta na região.
2. Crie uma solicitação nova: um item completo, peso e local. Abra o detalhe e confira o histórico.
3. Edite a descrição enquanto ABERTA.
4. Entre como `coletora@teste.com`, abra **Oportunidades**, aceite a de 250 kg e agende a retirada.
5. Volte como geradora: a solicitação aparece AGENDADA e a edição já não é permitida.
6. Entre como `coletora2@teste.com` (Rio de Janeiro, fora do raio): a solicitação de São Paulo não aparece nas oportunidades dela.

## Testes

`./mvnw verify` executa, com PostgreSQL real:

- `AceiteConcorrenteIT`: duas coletoras aceitam ao mesmo tempo e exatamente uma vence (T11).
- `RegrasR1R2IT`: regras lógicas definidas no próprio teste, R1 (b ∧ h) → c, toda coletora apta no raio é alertada, e R2 (c ∧ d) → i, a alertada que aceita inicia o rastreio.
- `RoteamentoIT`, `FiltroRegiaoTest` e `AlertaAoHomologarTest`: raio, ordem por distância e alertas retroativos.
- `SolicitacaoIT`, `OportunidadeIT`, `AgendamentoIT` e `FluxoPiIT`: criação, propriedade, oportunidades, agendamento e fluxo completo.
- `TransicoesSolicitacaoTest` e `TransicaoSolicitacaoServiceTest`: máquina de estados.
- `SolicitacaoControllerTest`: validação do formulário, acesso por perfil e associação de erros para leitor de tela (T14, trazida pelo #28).

## Contribuição registrada

Pull requests de autoria de Davi Fraga no repositório, com data de integração na `main` quando houver. Esta lista registra autoria no GitHub; não mede horas trabalhadas.

| PR | Data | Entrega |
| --- | --- | --- |
| #9 | 09/11/2026 | Domínio, máquina de estados e implementações reais dos contratos |
| #10 | 09/11/2026 | Telas da geradora para pedir e acompanhar coletas |
| #12 | 09/11/2026 | Oportunidades alertadas para a coletora |
| #13 | 09/11/2026 | Aceite atômico: só a primeira coletora fica com a coleta |
| #14 | 09/11/2026 | Agendamento e reagendamento pela vencedora |
| #15 | 09/11/2026 | Testes das regras lógicas R1 e R2 com o fluxo real |
| #17 | 09/11/2026 | Desfecho correto quando o aceite não acontece |
| #11 | 10/11/2026 | Roteamento por raio ao criar a solicitação |
| #16 | 10/11/2026 | Integração de aceite e agendamento e seed de demonstração |
| #23 | fechado em 27/11/2026 | Referências ABNT do relatório de Estrutura de Dados II; commit incorporado pelo #25 |
| #26 | em revisão | Auditoria de acessibilidade T14 (RF09) |
| #28 | em revisão | Correções da auditoria T14 e reteste |

Revisão cruzada prevista em [equipe.md](equipe.md): com Guilherme, transições, histórico e rejeições. Roteiro de estudo e da apresentação: [roteiro-davi.md](roteiro-davi.md).
