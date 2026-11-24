# Perguntas para revisão e apresentação

Material de estudo derivado do código e relatórios. As respostas explicam decisões, não comprovam autoria individual nem aprovação acadêmica. Cada integrante deve conferir os testes efetivamente executados em [testes](testes.md).

| Pergunta | Resposta e evidência |
| --- | --- |
| Qual problema o sistema resolve? | Organiza materiais, pontos e solicitações de descarte com acompanhamento. Fonte PDF pp.1–3; requisitos.md. O recorte B2B foi preservado e precisa aceitação do professor. |
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
| Duas instâncias provam dois computadores? | Não. Evidência histórica é duas JVMs no mesmo Windows. Rede/segunda máquina e firewall seguem pendentes até teste real. |
| Backup de 6,233 s comprova RTO2h? | Não. Foi um ensaio local específico com banco novo; não comprova cópia externa, retenção, desastre ou prazo futuro. Metas RPO24h/RTO2h continuam propostas. |
| BCrypt é descriptografado? | Não. É hash de senha para verificação. SHA-256 confere integridade do arquivo, sem cifrá-lo. RSA didático demonstra números e chaves, sem proteger dados reais no aplicativo. |
| O aplicativo tem homologação administrativa, financeiro e notificações prontos? | Não há interface completa para esses recursos nesta revisão. Tabelas, contratos e papel ADMIN não provam fluxo; links indisponíveis não devem prometer essas ações. |
| Os relatórios já foram aprovados pelo professor? | Não. PDFs gerados/testes locais são evidências técnicas. Permanecem identificação/cargos, revisão institucional e dúvidas do PDF, sem data corrigida silenciosamente. |
| Quais resultados devo citar? | A rodada identificada em testes.md, com seus XML/JSON e escopo. A rodada de fechamento passou com 331 Java (236 unitários + 95 IT), 12 E2E, 6 Node e 11 cenários do migrador. Os 320 Java anteriores são históricos; cenários de ferramentas não são somados ao Java. |

## Perguntas ainda dirigidas aos professores

Confirmar recorte B2B, modalidades entrega/coleta, abrangência da árvore, rubricas EDII/BDII e divergências de ano da apresentação, controle posterior à entrega e tamanho do grupo. Nomes/cargos devem ser confirmados pela equipe. Nenhum contato externo é realizado por este material.
