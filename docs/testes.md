# Testes, critérios de aceitação e evidências

**Fechamento validado em 24/11/2026:** 236 testes unitários + 95 IT, sem falhas/erros/pulos; spotless:check aprovado; E2E Chromium 12/12, Node 6/6 e guard do migrador 11/11. Resumos atuais em evidencias/fechamento-20261124/. O ensaio anterior de backup/restauração permanece histórico, sem nova execução em 24/11. Duas instâncias foram exercitadas no mesmo Windows, não em dois computadores. O ensaio de rede com outro integrante continua pendente. Resultados abaixo se referem a esta execução local, não a CI remoto ou aprovação acadêmica.

## Ambiente e comandos

JDK 21, Maven Wrapper e Docker para Testcontainers; PostgreSQL de teste descartável, separado do banco compartilhado. Não executar testes destrutivos contra os dados da equipe. No PowerShell, a partir da raiz:

```powershell
java -version
docker version
.\mvnw.cmd -B spotless:check
.\mvnw.cmd -B verify
python -c "print(pow(7,3,55), pow(13,27,55), (3*27)%40)"
```

Resultado histórico do `verify` anterior ao fechamento: **BUILD SUCCESS**, 320 testes no total (229 unitários + 91 IT), zero failures/errors/skipped. Totais conferidos pela leitura dos XML: 18 suites Surefire e 17 Failsafe. Spotless da rodada histórica foi aprovado; a rodada de fechamento tem conferência própria ao final deste documento; não inferir porcentagem de cobertura sem relatório específico. Relatórios produzidos: `target/surefire-reports/` e `target/failsafe-reports/`; JaCoCo quando configurado. A configuração do CI está em `.github/workflows/ci.yml`. `spotless:apply` altera código; `spotless:check` é a conferência.

## Matriz de casos

| Caso | Critério e preparação fictícia | Ação / resultado esperado | Fonte/evidência e resultado |
| --- | --- | --- | --- |
| T01 | CA01, usuário de teste com e-mail `.invalid` e empresa fictícia | Cadastrar, autenticar; senha armazenada BCrypt e não exibida novamente | APROVADO no verify e cadastro fictício pelo navegador (E2E check 2). |
| T02 | CA01, repetir e-mail/CNPJ de teste | Rejeitar e não criar empresa parcial | APROVADO no verify; cenários CadastroIT. |
| T03 | CA02/03, material “Notebook de demonstração”, categoria Informática, quantidade 2 | Persistir e recuperar identificação/categoria/quantidade; quantidade zero rejeitada | APROVADO no verify; FluxoPiIT e persistência E2E. |
| T04 | CA04, ponto fictício ativo, cidade fictícia, aceita categoria válida | Banco→AVL→filtro→lista; nenhum compatível produz mensagem; árvore preserva ordem/balanceamento | APROVADO: ArvoreAvlTest/FluxoPiIT e E2E check 3. |
| T05 | CA05/06, geradora e ponto compatível | Pela consulta escolher ponto/categoria, criar solicitação, consultar detalhe/histórico | APROVADO E2E: solicitação 6 persistida em A e consultada em B, checks 5–6. |
| T06 | CA05, ponto inativo ou categoria incompatível | POST direto rejeitado; sem solicitação parcial | APROVADO: verify e POST incompatível rejeitado no E2E check 4. |
| T07 | CA07, solicitação ABERTA da própria empresa | Editar descrição/identificação; preservar ponto, categoria/quantidade/local | APROVADO: verify e edição ABERTA E2E check 7. |
| T08 | CA07/08, outra empresa ou estado ACEITA | Edição/acesso negado e persistência inalterada | APROVADO: verify (propriedade/estado) e HTTP 409 após aceite E2E check 9. |
| T09 | CA10, coleta com mídias previstas | Concluir sem coleta/destinação/todas mídias rejeita; completar requisitos permite concluir | APROVADO no verify; E2E sem mídias concluiu após destinação e emitiu dois PDFs. |
| T10 | CA08, duas geradoras | Download de documento alheio negado; hash alterado detectado | APROVADO no verify (outra geradora/hash); CDF baixado pela mesma geradora em B no E2E. |
| T11 | RNF02, transação com erro; aceite concorrente | Rollback e somente uma coletora vencedora | APROVADO no verify: rollback, concorrência e transições. |
| T12 | BD, instância isolada | Executar View/Procedure/Function/Trigger, permissões e backup/restauração | APROVADO verify RecursosSqlIT e restauração isolada local em 6,233 s; cópia fora do host PENDENTE. |
| T13 | TC, inteiros didáticos sem dados pessoais | RSA imprime `13 7 1` e tabelas-verdade conferidas | APROVADO: comando RSA executado retornou 13 7 1; duas tabelas conferidas no relatório. |
| T14 | CA09, navegador desktop e tela estreita | Percorrer por teclado, foco, labels, mensagens e alternativa textual | APROVADO nos critérios auditados após o reteste de 27/11/2026 (ver [Auditoria de acessibilidade](#auditoria-de-acessibilidade-t14)): borda dos campos 3,69:1 e anel de foco 3,77:1; ponto marcado por teclado no mapa (Enter/Espaço no centro) ou pelas coordenadas manuais do #25; erros da nova solicitação associados aos campos. A auditoria de 26/11 resultou PARCIAL. Leitor de tela real, zoom de 200% e Lighthouse/axe PENDENTES. Recorte E2E anterior: 390 px e foco via Tab, check 12. |
| T15 | RNF05, duas máquinas autorizadas com mesmo banco team | Documento gravado numa instância é recuperado na outra; conexão/permissões corretas | PARCIAL: duas JVMs 18080/18081 no mesmo Windows compartilharam dados e upload; duas máquinas PENDENTE. |

Testes Java referenciados são relativos a `src/test/java/br/com/ewaste/`. Usar dados integralmente fictícios; revisar massas legadas antes de demonstração, especialmente CNPJ que pode coincidir com organização real. Não coletar dados pessoais reais de colegas para comprovar os casos.

## Registro de execução

| Data / responsável confirmado | Comando / ambiente | Resultado obtido | Relatório verificável |
| --- | --- | --- | --- |
| 24/11/2026 / execução coordenada pelo líder | `.\mvnw.cmd -B verify`, JDK 21/Docker/PostgreSQL descartável | BUILD SUCCESS, 229 unitários + 91 IT, zero falhas/erros/pulos | target/surefire-reports/ e target/failsafe-reports/ |
| 24/11/2026 / execução coordenada pelo líder | Chrome headless, duas instâncias team 18080/18081 no mesmo Windows | 12/12 checks, sucesso e rejeição, dados/arquivos compartilhados | [e2e-duas-instancias.json](evidencias/e2e-duas-instancias.json) e seis PNG |
| 24/11/2026 / execução coordenada pelo líder | Backup-Team.ps1 + Test-Restore.ps1, banco novo isolado preservado | 19 tabelas incluindo flyway_schema_history; 3 arquivos / 19422 bytes; 6,233 s | [restauracao-resumo.json](evidencias/restauracao-resumo.json) |
| 24/11/2026 / execução coordenada pelo líder | Python pow para RSA didático | 13 7 1 | Cálculos reproduzíveis em teoria-computacao.md |
| 26/11/2026 / Davi Fraga | Auditoria T14: perfil dev, PostgreSQL descartável, Chromium automatizado 1366 px e 390 px | PARCIAL: 3 critérios por tela; 2 falhas de contraste de componente e 1 limite de teclado no mapa | [Auditoria de acessibilidade](#auditoria-de-acessibilidade-t14) e PNG em evidencias/acessibilidade-20261126/ |
| 27/11/2026 / Davi Fraga | Reteste T14 após correções: `.\mvnw.cmd -B verify spotless:check` e o mesmo ambiente da auditoria | BUILD SUCCESS, 250 unitários + 97 IT, zero falhas; contraste, mapa por teclado e erros associados aprovados | [Reteste](#reteste-após-correções-27112026) e PNG em evidencias/acessibilidade-20261127/ |

## Roteiro individual

Cada integrante deve selecionar uma história, localizar UML e implementação, explicar uma decisão e apresentar um teste associado com resultado obtido. As áreas principais e revisões cruzadas foram definidas pela equipe em [equipe.md](equipe.md). Cada integrante estuda o fluxo completo e demonstra seu foco: Miguel — Design/identidade/segurança; Davi — requisitos/solicitação/AVL; Guilherme — rastreio/documentos/Teoria; Felipe — qualidade/armazenamento/recuperação; Rafael — SQL/BDII/Administração/custos. Essas atribuições futuras não comprovam contribuições já realizadas; o registro individual e o ensaio ainda precisam acontecer.

## Evidências do navegador e limites

Registro legível: [e2e-duas-instancias.json](evidencias/e2e-duas-instancias.json), com 12 checks e `passed=true`. Imagens: [home](evidencias/01-home.png), [pontos](evidencias/02-pontos.png), [rejeição](evidencias/03-rejeicao.png), [consulta em outra instância](evidencias/04-outra-instancia.png), [documentos](evidencias/05-documentos.png), [móvel](evidencias/06-mobile.png).

A execução criou geradora fictícia, consultou pontos pela AVL, rejeitou categoria inválida no servidor, criou solicitação 6, consultou e editou por outra instância, aceitou/agendou com coletora de seed, rejeitou edição posterior com HTTP 409, recuperou upload PNG em B após envio em A com SHA igual, registrou destinação pela Procedure, concluiu gerando dois PDFs e baixou CDF em B. SHA-256 do PDF consta no JSON. Os testes com mídias e negativa a outra empresa são casos IT separados; a alternância entre instâncias no E2E não equivale a acesso entre empresas distintas.

A restauração foi feita em banco novo com nome isolado, preservado para conferência; os 6,233 s medem aquele ensaio local, não comprovam RTO futuro, failover, retenção automática ou backup fora do anfitrião. Naquele ensaio, a rede entre dois computadores não foi validada: não havia segunda máquina disponível, o firewall não foi aplicado por ausência de privilégios administrativos e o IP do anfitrião mudou durante o trabalho. Loopback saudável não libera acesso remoto.

Conferências históricas anteriores ao fechamento: spotless:check aprovado; importação de legado com arquivo válido e execução repetida passou no IT adicional (total final 91 IT). O script do operador executou migração sem dev depois de uma inicialização com seeds, encerrando com código 0. A tentativa de importar referências sem arquivos dos seeds falhou de forma controlada; não foi tratada como recuperação bem-sucedida.

## Rodada de fechamento da auditoria

Baseline histórico preservado em [testes-resumo.json](evidencias/testes-resumo.json): **320 = 229 unitários + 91 IT**, sem falhas/erros/pulos. A menção anterior a 319 junto de 229+91 era soma incorreta, corrigida sem alterar o JSON histórico. Arquivos E2E/restauração antigos mantêm data e escopo originais.

**Rodada aprovada: 331 testes Java (236 unitários + 95 IT), zero falhas, erros ou ignorados; spotless:check aprovado. E2E: 12/12.** Evidências novas estão publicadas em `docs/evidencias/fechamento-20261124/`, sem sobrescrever os JSON/PNG antigos. Os testes Node de configuração do E2E passaram (6/6): fuso São Paulo/virada de ano, data futura, overrides inválidos/passados e URLs locais distintas. Bootstrap executado com `npm ci --prefix tools` e `npm run browser:install --prefix tools`, Playwright 1.65.1 pinado; Chromium gerenciado instalado. Isso valida preparação/configuração, não substitui o E2E nem altera o total Java.

E2E de fechamento: [resultado.json](evidencias/fechamento-20261124/resultado.json), run `fechamento-20261124-final`, de 24/11/2026. Node v22.21.1, Playwright 1.65.1 e Chromium gerenciado 159.0.8154.6, sem dependências fora do repositório. Duas JVMs locais 18080/18081 apontaram ao banco isolado de auditoria na porta 15432; criaram solicitação **5**. Data de retirada foi calculada dinamicamente para 26/11/2026 09h27 em São Paulo. Há seis PNG e CDF fictício na mesma pasta; o JSON registra hash do PDF. A solicitação 6 e os horários anteriores pertencem à evidência histórica, não foram sobrescritos.

O guard do migrador passou em 11 cenários: [migrador-cenarios.json](evidencias/fechamento-20261124/migrador-cenarios.json). Os casos permitidos simulam Java; não são prova de migração do banco comum. DER lógico corrigido expõe atributos VARCHAR/NUMERIC e PK caminho; UML corrige empresa opcional de ADMIN, assinatura criar(Solicitacao) e listeners separados AFTER_COMMIT/REQUIRES_NEW. PNGs foram regenerados antes dos PDFs afetados.

Resultado Java consolidado: [testes-resumo.json](evidencias/fechamento-20261124/testes-resumo.json), obtido dos XML Surefire/Failsafe após `.\mvnw.cmd -B verify spotless:check` (BUILD SUCCESS). Antes da rodada final, a regressão encontrou expectativas antigas de links indisponíveis em HomeControllerTest e AcessoIT; elas foram corrigidas para a navegação atual, preservando autenticação e logout. Os específicos `.\mvnw.cmd -B -Dtest=ItemFormTest,SolicitacaoControllerTest -Dit.test=FluxoPiIT,SolicitacaoIT,RegrasR1R2IT verify` também passaram (17 IT); são subconjunto, não devem ser somados aos 331.

Comandos adicionais executados: `npm.cmd test --prefix tools` (6/6), `pwsh -NoProfile -File infra/tests/Test-Migrate-Team.ps1` (11/11, Java/Maven simulados), `npm.cmd run demo --prefix tools` com PI_DEMO_CONFIRM=FICTICIO (12/12, Chromium real). O ensaio usou banco isolado, volume próprio e duas JVMs; não alterou o banco compartilhado da equipe.

## Auditoria de acessibilidade (T14)

Execução de 26/11/2026 por Davi Fraga, referente a RF09/CA09 e RNF03. Escopo: telas de cadastro (geradora e coletora), nova solicitação e oportunidades (lista e detalhe). Critérios: contraste, rótulos dos campos e navegação somente por teclado. Referência: WCAG 2.1, nível AA, critérios 1.4.3 (texto, 4,5:1 ou 3:1 para texto grande), 1.4.11 (componentes e estados, 3:1), 1.3.1/3.3.2 (rótulos), 2.1.1/2.1.2 (teclado, sem armadilha) e 2.4.7 (foco visível). **O resultado não é declaração de conformidade WCAG.**

Ambiente: branch `feat/felipe/cep-mapa` (telas com CEP/mapa), perfil dev com seeds fictícios, PostgreSQL 16 descartável em container próprio (porta 55432), sem uso do banco compartilhado. Contas `geradora@teste.com` e `coletora@teste.com` do seed. Chromium automatizado (Playwright) em 1366×900 e 390×844. Contraste calculado pela fórmula de luminância relativa da WCAG sobre as cores computadas; teclado percorrido com Tab, registrando elemento focado, indicador de foco e repetição de foco.

| Tela | Contraste | Rótulos | Teclado |
| --- | --- | --- | --- |
| Cadastro geradora | Texto: 37 elementos, nenhuma falha. **Borda dos campos `#9bad9e` sobre `#ffffff`: 2,37:1 (falha 1.4.11).** | 10/10 campos com `<label for>`; CEP e senha com `aria-describedby` existente. | 21 paradas, ordem visual, foco visível em todas, sem armadilha; o mapa ocupa 5 paradas e libera o foco. |
| Cadastro coletora | Texto: 41 elementos, nenhuma falha. Mesma falha de borda (2,37:1). | 11/11 campos com `<label for>`, incluindo raio de atendimento. | 22 paradas, mesmo resultado. CEP digitado por teclado preencheu endereço, cidade, UF e coordenadas e moveu o foco para Endereço. |
| Nova solicitação | Texto: 57 elementos, nenhuma falha; mensagens de erro 8,16:1. **Borda dos campos sobre `#f5f7f3`: 2,20:1; anel de foco `#c28b20`: 2,78:1 (falhas 1.4.11).** | 20/20 campos com `<label>`, inclusive os itens dinâmicos (`itens0…itens2`). | 33 paradas, ciclo completo, foco visível. Envio sem localização é bloqueado com mensagem em `role="status"` e foco no CEP. |
| Oportunidades (lista e detalhe) | Texto: 21 elementos, nenhuma falha. | Sem campos de formulário; links e botões com texto descritivo ("Ver e aceitar", "Aceitar esta coleta"). | 8 paradas na lista e 8 no detalhe; botão de aceite alcançável e acionável por teclado. |

Em 390 px nenhuma das telas apresentou rolagem horizontal. Botões de estilo suave ("Localizar pelo CEP", "Sair") têm fundo próximo ao da página, mas o texto identifica o componente e tem contraste suficiente; registrados como observação, não como falha.

**Limite de teclado no mapa.** O mapa recebe foco e permite deslocamento, mas a marcação do ponto exato ocorre somente por clique. A alternativa sem mouse é a busca pelo CEP, que depende de serviços externos (ViaCEP, Nominatim e Leaflet via unpkg). Se o CEP não for encontrado ou esses serviços estiverem indisponíveis, latitude e longitude ficam vazias, os campos estão ocultos e o servidor as exige: usuário apenas de teclado não conclui o cadastro nem a solicitação nesse caso.

**Mensagens de erro.** Os textos são claros e ficam junto do campo. Na nova solicitação, porém, o resumo "Revise os campos destacados abaixo" não usa `role="alert"`, os campos inválidos não recebem `aria-invalid` nem `aria-describedby` apontando para a mensagem, e o foco volta ao início da página após o envio. O cadastro usa resumo com `role="alert"`.

Recomendações da auditoria de 26/11 (implementadas em 27/11, ver [reteste](#reteste-após-correções-27112026)):

1. Escurecer a borda dos campos e a cor do anel de foco até pelo menos 3:1 contra `#ffffff` e `#f5f7f3`.
2. Oferecer alternativa por teclado à marcação no mapa, por exemplo campos de latitude/longitude visíveis quando o CEP falhar ou marcação do centro do mapa por tecla.
3. Na nova solicitação, aplicar `role="alert"` ao resumo, `aria-invalid` e `aria-describedby` aos campos com erro e mover o foco para o resumo após o envio.

Não verificados: leitor de tela real (NVDA/VoiceOver), ampliação de 200%/reflow além de 390 px, demais telas do sistema e ferramentas automáticas como Lighthouse/axe. Evidências: [cadastro geradora](evidencias/acessibilidade-20261126/cadastro-geradora-desktop.png) ([390 px](evidencias/acessibilidade-20261126/cadastro-geradora-390px.png)), [cadastro coletora](evidencias/acessibilidade-20261126/cadastro-coletora-desktop.png) ([390 px](evidencias/acessibilidade-20261126/cadastro-coletora-390px.png), [CEP por teclado](evidencias/acessibilidade-20261126/cadastro-coletora-cep-teclado.png)), [nova solicitação](evidencias/acessibilidade-20261126/nova-solicitacao-desktop.png) ([390 px](evidencias/acessibilidade-20261126/nova-solicitacao-390px.png), [sem localização](evidencias/acessibilidade-20261126/nova-solicitacao-erro-sem-mapa.png), [erros do servidor](evidencias/acessibilidade-20261126/nova-solicitacao-erros.png)), [oportunidades](evidencias/acessibilidade-20261126/oportunidades-desktop.png) ([390 px](evidencias/acessibilidade-20261126/oportunidades-390px.png)) e [detalhe](evidencias/acessibilidade-20261126/oportunidade-detalhe-desktop.png).

### Reteste após correções (27/11/2026)

Correções no branch `fix/davi/acessibilidade-t14`, sobre o mesmo ambiente da auditoria (perfil dev, PostgreSQL descartável, Chromium automatizado a 1366 px):

| Achado de 26/11 | Correção | Resultado do reteste |
| --- | --- | --- |
| Borda dos campos 2,37:1 / 2,20:1 | `app.css`: borda de `input`, `select` e `.map` de `#9bad9e` para `#6f8574` | 3,97:1 sobre `#ffffff` e 3,69:1 sobre `#f5f7f3`: **aprovado** (1.4.11) |
| Anel de foco 2,78:1 | `app.css`: `:focus-visible` de `#c28b20` para `#a87410` | 4,06:1 sobre branco, 3,77:1 sobre a página e 3,42:1 sobre `#e4eee5`: **aprovado** (1.4.11/2.4.7) |
| Mapa sem alternativa por teclado | O #25 (Miguel) passou a oferecer latitude/longitude manuais em `<details>`, abertos quando CEP, geocodificação ou mapa falham. Complemento deste reteste em `seletor-localizacao.js`: com o mapa em foco, Enter ou Espaço marca o centro; `app.css` mostra uma mira no centro enquanto o mapa tem foco; ajuda das duas telas cita o atalho | Nova solicitação e cadastro de coletora: Tab até o mapa, setas e `+` para posicionar, Enter preencheu latitude/longitude, exibiu o marcador e anunciou o status; Espaço também marcou sem rolar a página; o raio da coletora foi desenhado. Não depende mais do CEP: **aprovado** (2.1.1) |
| Erros da nova solicitação sem associação | `nova.html`: resumo com `role="alert"` e `tabindex="-1"`, campos com erro recebem `aria-invalid="true"` e `aria-describedby` para a mensagem; `validar-itens.js` move o foco ao resumo | Envio com peso 0 e quantidade 0: foco no resumo, `aria-invalid` e `aria-describedby` somente em `pesoEstimadoKg` e `itens0.quantidade`, mensagens localizadas pelo id; localização marcada mantida. Latitude/longitude manuais sem valor recebem `aria-describedby` com a ajuda e o erro: **aprovado** (3.3.1/1.3.1) |

Testes automatizados novos: `SolicitacaoControllerTest.errosFicamAssociadosAosCamposParaLeitorDeTela` (escrito antes da correção: falhou e depois passou) e `coordenadasAusentesFicamAssociadasAAjudaEAoErro`, criado após a integração com o #25. Rodada completa `.\mvnw.cmd -B verify spotless:check`: BUILD SUCCESS, 250 unitários + 97 IT, zero falhas, erros ou ignorados.

Limites: o atalho do mapa depende do Leaflet carregado de unpkg; se ele falhar, as coordenadas manuais do #25 continuam disponíveis para mouse e teclado. Continuam não verificados: leitor de tela real, zoom de 200% e Lighthouse/axe. Evidências: [mapa por teclado na nova solicitação](evidencias/acessibilidade-20261127/mapa-teclado-enter.png), [mapa por teclado no cadastro de coletora](evidencias/acessibilidade-20261127/cadastro-coletora-mapa-teclado.png) e [erros associados](evidencias/acessibilidade-20261127/nova-solicitacao-erros-aria.png).

## Próximo ensaio entre computadores

A segunda máquina será a de Felipe, escolhida também como futura anfitriã do PostgreSQL; Rafael operará o banco e Felipe as cópias/recuperação. A transferência ainda não ocorreu. T15 continua pendente até teste real autorizado de registro em uma máquina/consulta em outra e envio/recuperação de arquivo, incluindo origem não autorizada bloqueada. Os resultados anteriores de duas JVMs no mesmo Windows permanecem históricos e não serão reapresentados como prova desse ensaio. Todas as novas massas devem ser fictícias; não há produção ou dados pessoais reais planejados.

## Rodada de coordenação e Design — 26/11/2026

`verify spotless:check`: BUILD SUCCESS, **248 unitários + 97 IT = 345**, zero falhas/erros/ignorados, JDK 21/Docker/Testcontainers descartável. [Resumo atual](evidencias/design-homologacao-20261126/testes-java.json). As contagens de 24/11 são históricas.

O navegador real concluiu **12/12** no [fluxo integrado](evidencias/design-homologacao-20261126/fluxo-integrado/resultado.json) e **10/10** no [fluxo de homologação](evidencias/design-homologacao-20261126/homologacao/resultado.json): coletora fictícia, solicitação anterior à aprovação, rejeição sem documentos, upload, análise, reprovação/motivo, correção/reenvio, aprovação, alerta retroativo e acesso ao documento próprio/alheio. No ensaio de homologação, HTTPS externo foi bloqueado para exercitar o cadastro por coordenadas manuais sem CDN/CEP. Dois aplicativos locais usaram banco isolado na porta 15432; isso não comprova VPS nem dois computadores.

A [reprodução anterior](evidencias/design-homologacao-20261126/reproducao-download-500.json) passou em nove casos e encontrou HTTP 500 no download fora da coletora dona. Após correção, os dois controllers devolvem404 vazio; dois testes específicos e o E2E final confirmam a negativa sem arquivo. Esse resultado inicial falho permanece histórico, sem ser apresentado como aprovação.

Treze testes Node cobrem seis casos de configuração do ensaio e sete do seletor de localização: concorrência de CEP/geocoder, invalidação das coordenadas e alternativa manual em falha externa. Não equivalem a treze novos testes Java nem devem ser somados aos 345.
