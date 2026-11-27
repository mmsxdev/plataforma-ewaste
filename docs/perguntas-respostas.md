# Perguntas para revisão e apresentação

Material de estudo derivado do código e relatórios. As respostas explicam decisões, não comprovam autoria individual nem aprovação acadêmica. Cada integrante deve conferir os testes efetivamente executados em [testes](testes.md).

| Pergunta | Resposta e evidência |
| --- | --- |
| Qual problema o sistema resolve? | Organiza materiais, pontos e solicitações de descarte com acompanhamento. Fonte PDF pp.1–3; requisitos.md. O recorte B2B foi preservado e confirmado pelos professores em 24/11/2026; equipe.md registra a origem. |
| Por que preservar Java/Spring/PostgreSQL? | Já há fluxo integrado e testes; a stack atende autenticação, persistência, SQL acadêmico e interface sem reescrita. Arquitetura.md e pom.xml. |
| Onde entra a árvore exigida? | Registros JDBC de pontos entram na ArvoreAvl antes de busca/percurso/filtros, em PontosColetaService. Não são apenas índices internos do banco. Estrutura-dados.md e testes AVL/fluxo. |
| Qual custo e limite da AVL? | Inserção/busca balanceadas O(log n); percurso O(n); reconstrução por consulta O(n log n). Filtro de cidade/categoria percorre a árvore e não vira O(log n) só por usar AVL. Sem promessa de superar SQL indexado. |
| Como evita acesso a pedido de outra empresa? | Papel na rota e propriedade no serviço/repositório; ocultar botão não basta. SecurityConfig, SolicitacaoService e casos de acesso. |
| ADMIN precisa estar ligado a uma empresa? | Não. usuario.empresa_id admite NULL, usado pelo ADMIN; UML precisa mostrar 0..1 empresa por usuário. Perfis de geradora/coletora usam vínculo de negócio. |
| Ponto escolhido equivale a coletora vencedora? | Não. Ponto é destino informado e tem categorias aceitas; coletora assume pelo roteamento/aceite B2B. Essa separação e a experiência de solicitação precisam ficar claras na interface. |
| Por que não editar tudo depois do pedido? | Categoria/local/quantidade já influenciam roteamento. A correção disponível preserva esses dados e limita texto/identificação a ABERTA pela geradora dona, com proteção concorrente. |
| O que ocorre quando o ponto fica inativo? | Ele deixa de aceitar novos pedidos, mas a referência histórica deve continuar consultável. Não preencher legado com valores inventados. Consultar contrato buscarParaHistorico e testes da rodada vigente. |
| Evento é publicado depois do commit? | A publicação ocorre dentro do método transacional. O listener @TransactionalEventListener recebe após o commit e usa REQUIRES_NEW para sua escrita. São objetos/transações distintos; ordem entre listeners não é uma garantia. |
| Quais recursos de BDII têm uso real? | View vw_acompanhamento_solicitacao, Function ponto_aceita_tipo, Procedure registrar_destinacao_pi e Trigger trg_auditar_status_solicitacao_pi. SQL-recursos.md mostra chamadas, regras e limites. |
| O Trigger identifica o usuário humano? | Registra current_user SQL e transação. Essa conta técnica pode atender várias instâncias; não equivale ao usuário autenticado nem a trilha imutável contra DBA. |
| Como demonstrar ACID? | Operações compostas em transação, restrições, rollback e concorrência testados; PostgreSQL mantém commit/WAL. Não presumir que disco externo faz parte da transação só porque o serviço usa @Transactional. |
| Por que armazenar arquivos no banco em team? | Compartilhar bytes e referências entre instâncias e incluir no backup do protótipo. Custo: volume/WAL/restauração; não é recomendação de escala. ArmazenamentoCompartilhado e V200. |
| Duas instâncias provam dois computadores? | Não. As evidências são duas JVMs no mesmo Windows. Felipe será a futura anfitriã e a segunda máquina do ensaio; a transferência, rede e firewall ainda precisam de teste real. |
| Backup de 6,233 s comprova RTO2h? | Não. Foi um ensaio local específico com banco novo; não comprova cópia externa, retenção, desastre ou prazo futuro. Metas RPO24h/RTO2h continuam propostas. |
| BCrypt é descriptografado? | Não. É hash de senha para verificação. SHA-256 confere integridade do arquivo, sem cifrá-lo. RSA didático demonstra números e chaves, sem proteger dados reais no aplicativo. |
| O aplicativo tem homologação administrativa, financeiro e notificações prontos? | Homologação tem telas de envio, análise, aprovação e reprovação desde e765791. Suspensão não possui ação de interface; financeiro e notificações ainda não têm fluxo completo. HomologacaoService e AlertaAoHomologar ligam aprovação a oportunidades retroativas no raio. |
| Os relatórios já foram aprovados pelo professor? | Não. PDFs gerados/testes locais são evidências técnicas. Composição/turma e funções foram definidas; o formato solicitado é ABNT com capa, folha de rosto e sumário. A conferência formal dos PDFs está registrada em normalizacao.md; a submissão ainda não ocorreu e a apostila citada pelo Davi ainda não teve seus metadados conferidos diretamente. Datas foram confirmadas pela equipe sem apagar as divergências originais. |
| Quais resultados devo citar? | A rodada identificada em testes.md, com seus XML/JSON e escopo. A rodada de fechamento passou com 331 Java (236 unitários + 95 IT), 12 E2E, 6 Node e 11 cenários do migrador. Os 320 Java anteriores são históricos; cenários de ferramentas não são somados ao Java. |

## Confirmações e limites que devem ser explicados

Os professores confirmaram o grupo de cinco integrantes e recortes B2B/coleta/AVL. Apresentação adotada em 07/12/2026; controles em 25/12/2026 e 02/12/2026; entrega 05/12/2026 até 23h59. O controle posterior à entrega foi confirmado literalmente, sem presumir outra data. A contradição original 2025/2026 não foi removida do PDF. Não foram criadas rubricas ou notas em nome dos docentes.

As funções autorizadas estão em [equipe.md](equipe.md): Rafael opera o banco e Felipe cuida de backups/recuperação e disponibilizará a futura anfitriã. A transferência e o ensaio entre computadores permanecem pendentes; o backup precisa de destino fora da máquina de Felipe, ainda não definido. A apresentação usa só dados fictícios e não haverá produção; prazo de retenção da demonstração não foi informado. Link da pasta de submissão ainda não fornecido. Nenhum contato ou envio externo foi realizado.

A aprovação não altera pedidos já aceitos: `AlertaAoHomologar` consulta somente ABERTAS, confirma aptidão e raio e evita repetir alerta existente. A publicação de `ColetoraHomologadaEvent` ocorre na transação da aprovação; o consumo ocorre após commit em outra transação. Isso difere de executar todo o fluxo numa única transação.
