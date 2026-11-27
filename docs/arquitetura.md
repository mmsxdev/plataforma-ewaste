# Arquitetura

Monólito modular em Spring Boot: um único deploy, com o código dividido em módulos de dono único (pacotes em `br.com.ewaste`).

- Telas renderizadas no servidor com Thymeleaf; Leaflet é usado no seletor de localização dos cadastros, com entrada manual alternativa. Pontos são consultados em lista textual. Não há contador de notificações funcional nesta entrega.
- Cada módulo expõe para os outros apenas o pacote `api` (interfaces + records). O teste `ArquiteturaTest` garante isso.
- Criação/roteamento/rastreio usam eventos de domínio em `shared/events`, publicados com `ApplicationEventPublisher` dentro da transação e consumidos com `@TransactionalEventListener` após commit. Contratos reservam notificações/cobrança, mas não há implementação completa dessas extensões.
- Segurança por prefixo de rota: `/geradora/**`, `/coletora/**` e `/admin/**`, cada um restrito ao perfil correspondente.
- Banco PostgreSQL com esquema versionado por Flyway. O Hibernate só valida (`ddl-auto: validate`).

Fluxo principal: homologação libera a coletora → a geradora cria a solicitação → o roteamento alerta as coletoras aptas no raio → a primeira aceita e agenda → a coleta e as mídias são registradas → os documentos (CDF, laudo, certificado) são emitidos → a geradora baixa os documentos no painel. A consulta textual de pontos integra o início da solicitação; mapa cartográfico não é obrigatório. A homologação tem telas de upload/solicitação de análise pela coletora e de aprovação/reprovação pelo ADMIN. Suspensão não tem ação na interface. Financeiro e notificações ainda não possuem interface completa.

## Adequação ao PI oficial

O percurso acadêmico acrescenta consulta textual de pontos em `/mapa`, com dados persistidos → AVL → busca/percurso → filtros de cidade/categoria. A ação de solicitar mantém ponto e categoria no formulário. O ponto informado e a coletora que assume o roteamento são conceitos distintos. Solicitações legadas sem ponto continuam válidas. Identificação do material complementa tipo/quantidade/peso sem criar outro cadastro duplicado. A geradora pode corrigir descrição/identificação somente em ABERTA; campos que alterariam o roteamento são preservados. UML em [uml/](uml/) representa o fluxo de solicitação e os limites dos componentes.

Decisões: preservar monólito modular e PostgreSQL, pois atendem às exigências sem reescrita; usar AVL própria na consulta de pontos para tornar visível a exigência EDII; preservar campos antigos e acrescentar nullable na V600; usar Function de compatibilidade e Procedure de destinação (V601) com efeito real, View de acompanhamento e Trigger de auditoria técnica. Explicação/limites em [banco-dados.md](banco-dados.md) e [sql-recursos.md](sql-recursos.md).

## Operação compartilhada e armazenamento

No perfil team, instâncias locais de aplicativo apontam para o mesmo PostgreSQL e usam `ArmazenamentoCompartilhado`, que persiste bytes/metadados em `arquivo_compartilhado` (V200). A transação SQL pode abranger referências e bytes nesse perfil. Fora de team, disco é preservado e não participa de rollback SQL; importação legada é explícita. Arquivos no banco simplificam compartilhamento e backup do protótipo, com custo de volume/WAL/recuperação; avaliar object storage para outra escala. Não dependemos de SMB.

Somente operador aplica Flyway; team exige configuração privada, valida o esquema e não migra. HTTP fica local em cada computador, JDBC chega ao anfitrião autorizado. Privilégios do papel técnico não substituem autorização do usuário final por empresa. Conexão de duas máquinas e política de rede efetiva: pendentes de ensaio; nenhuma alegação de disponibilidade alta ou implantação pública.

## Privacidade e dados pessoais

| Dado / finalidade | Armazenamento | Quem deve acessar / proteção |
| --- | --- | --- |
| Nome/e-mail do responsável / conta e autenticação | usuario | Próprio fluxo autenticado e administrador autorizado; unicidade, autorização e minimização; não expor lista pública de contas. |
| senha_hash / verificação de senha | usuario | Serviço de identidade; BCrypt, nunca senha em texto; não é criptografia reversível. |
| Telefone/endereço/localização / contato e logística | empresa, solicitacao | Empresa dona, coletora autorizada e administração conforme função; controlar exportação e acesso ao host/backup. |
| Fotos, números de série, documentos / evidências | referências em tabelas e bytes no PostgreSQL team; disco fora de team | Donos autorizados por serviço; tipos/tamanho/conteúdo validados; não servir pasta pública irrestrita. Evitar rostos/dados pessoais na massa fictícia. |
| Estados e auditoria / acompanhamento e integridade | evento_rastreio, solicitacao_auditoria | Usuários autorizados e operador; conta SQL técnica registrada não identifica pessoa humana nem torna trilha imutável contra DBA. |

Mesmo em B2B, nome/e-mail e possivelmente localização/imagem podem identificar pessoas naturais. A LGPD demanda análise conforme finalidade e contexto; a exceção para fins exclusivamente acadêmicos do art.4º II b mantém aplicação dos arts.7º e 11. Usar dados fictícios no protótipo reduz exposição, mas não autoriza afirmar dispensa geral ou conformidade jurídica integral. A equipe confirmou uso exclusivamente acadêmico com dados fictícios e sem produção ou uso planejado de pessoas reais. O prazo de retenção da massa de demonstração ainda não foi definido. Bases legais, responsável pelo tratamento e canal de atendimento exigiriam decisão específica antes de qualquer mudança para dados reais, fora do escopo atual. Não inferir consentimento do mero cadastro nem dispensa jurídica geral. [Decisão e responsabilidades](equipe.md).

Proteção operacional ainda depende de rede, permissões do host, segredos e backups; o código não configura automaticamente TLS, criptografia de volume ou firewall. Não alegar criptografia em repouso por haver BYTEA ou hash. RSA didático de teoria-computacao.md é separado da proteção real.

Os contratos entre módulos estão em [contratos](contratos.md).

## Referências

BRASIL. **Lei nº 13.709, de 14 de agosto de 2018**. Disponível em: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm. Acesso em: 23 nov. 2026. Referência normativa para análise acadêmica, sem declaração de conformidade.


## Validação da arquitetura nesta entrega

Na rodada histórica anterior ao fechamento de 24/11/2026, `verify` passou com 229 testes unitários e 91 de integração, sem falhas/erros/pulos. O E2E Chrome confirmou 12 verificações em duas instâncias team no mesmo Windows: dados gravados em A aparecem em B, upload recuperado na outra instância com integridade igual e CDF gerado/baixado em B. As seis imagens e o JSON estão em [docs/evidencias](evidencias/); casos e critérios em [testes](testes.md). Oito diagramas UML/DER/organograma foram renderizados em `docs/uml/*.png` a partir dos `.puml`.

Backup/restauração local conferiu 19 tabelas (18 de domínio/infra mais flyway_schema_history), 3 arquivos e 19422 bytes em 6,233 s. A restauração em banco novo foi preservada; não houve cópia fora do host. O ensaio comprova o recorte local executado, não alta disponibilidade ou RTO futuro. Naquele ensaio não havia máquina adicional nem aplicação administrativa de firewall; a rede de dois computadores continua sem comprovação; mudança do IP detectado não configura automaticamente acesso dos colegas.

No fechamento de 24/11/2026, `verify spotless:check` passou com 236 unitários + 95 IT e o E2E Chromium passou em 12/12. Evidências atuais: [resumo Java](evidencias/fechamento-20261124/testes-resumo.json) e [ensaio entre instâncias locais](evidencias/fechamento-20261124/resultado.json). Rede entre dois computadores continua pendente.

## Anfitriã planejada e responsabilidades

A transferência do PostgreSQL para a máquina de Felipe foi aprovada, mas não executada. A instância atual em Miguel permanece preservada até backup/restauração e teste de acesso/dados/arquivos em dois computadores. Rafael será operador do banco e Felipe responsável pelas cópias/recuperação. O destino de backup fora da futura anfitriã está pendente; guardar somente na máquina de Felipe não constitui cópia externa. As evidências locais acima são históricas e não comprovam essa transferência. Ver [equipe](equipe.md) e [implantação](implantacao.md).

## Homologação e recuperação de oportunidades

`HomologacaoColetoraController` e `AdminHomologacaoController` usam `HomologacaoService`. A coletora envia os documentos obrigatórios e pede análise; o ADMIN aprova ou reprova com motivo. Serviço e domínio validam estado/documentos; os downloads preservam perfil e propriedade. A aprovação publica `ColetoraHomologadaEvent` dentro da transação. `AlertaAoHomologar` recebe após commit e abre uma transação `REQUIRES_NEW`: confere aptidão, busca solicitações ABERTAS via `SolicitacaoConsulta.listarAbertas()`, filtra o raio e grava alertas ainda inexistentes, publicando `AlertasEnviadosEvent`. Uma coletora aprovada depois da criação do pedido pode assim receber a oportunidade. A ordem entre listeners distintos não é garantida.

As figuras de classes e sequência de homologação em docs/uml representam essa implementação. A presença de suspensão no domínio não comprova um comando de suspensão na tela. Telas de homologação foram acrescentadas em e765791; os registros anteriores de indisponibilidade são históricos.
