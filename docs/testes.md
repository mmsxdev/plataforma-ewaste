# Testes, critérios de aceitação e evidências

**Validação executada em 24/11/2026:** build completo aprovado, 229 testes unitários e 91 de integração, sem falhas, erros ou pulos; E2E em Chrome headless com 12 verificações aprovadas; backup/restauração isolada aprovados. Duas instâncias foram exercitadas no mesmo Windows, não em dois computadores. O ensaio de rede com outro integrante continua pendente. Resultados abaixo se referem a esta execução local, não a CI remoto ou aprovação acadêmica.

## Ambiente e comandos

JDK 21, Maven Wrapper e Docker para Testcontainers; PostgreSQL de teste descartável, separado do banco compartilhado. Não executar testes destrutivos contra os dados da equipe. No PowerShell, a partir da raiz:

```powershell
java -version
docker version
.\mvnw.cmd -B spotless:check
.\mvnw.cmd -B verify
python -c "print(pow(7,3,55), pow(13,27,55), (3*27)%40)"
```

Resultado do `verify`: **BUILD SUCCESS**, 319 testes no total (229 unitários + 91 IT), zero failures/errors/skipped. Totais conferidos pela leitura dos XML: 18 suites Surefire e 17 Failsafe. Spotless final será conferido pelo líder após a revisão documental; não inferir porcentagem de cobertura sem relatório específico. Relatórios produzidos: `target/surefire-reports/` e `target/failsafe-reports/`; JaCoCo quando configurado. A configuração do CI está em `.github/workflows/ci.yml`. `spotless:apply` altera código; `spotless:check` é a conferência.

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
| T14 | CA09, navegador desktop e tela estreita | Percorrer por teclado, foco, labels, mensagens e alternativa textual | APROVADO no recorte E2E: viewport 390 px e foco via Tab, check 12; auditoria completa de acessibilidade NÃO realizada. |
| T15 | RNF05, duas máquinas autorizadas com mesmo banco team | Documento gravado numa instância é recuperado na outra; conexão/permissões corretas | PARCIAL: duas JVMs 18080/18081 no mesmo Windows compartilharam dados e upload; duas máquinas PENDENTE. |

Testes Java referenciados são relativos a `src/test/java/br/com/ewaste/`. Usar dados integralmente fictícios; revisar massas legadas antes de demonstração, especialmente CNPJ que pode coincidir com organização real. Não coletar dados pessoais reais de colegas para comprovar os casos.

## Registro de execução

| Data / responsável confirmado | Comando / ambiente | Resultado obtido | Relatório verificável |
| --- | --- | --- | --- |
| 24/11/2026 / execução coordenada pelo líder | `.\mvnw.cmd -B verify`, JDK 21/Docker/PostgreSQL descartável | BUILD SUCCESS, 229 unitários + 91 IT, zero falhas/erros/pulos | target/surefire-reports/ e target/failsafe-reports/ |
| 24/11/2026 / execução coordenada pelo líder | Chrome headless, duas instâncias team 18080/18081 no mesmo Windows | 12/12 checks, sucesso e rejeição, dados/arquivos compartilhados | [e2e-duas-instancias.json](evidencias/e2e-duas-instancias.json) e seis PNG |
| 24/11/2026 / execução coordenada pelo líder | Backup-Team.ps1 + Test-Restore.ps1, banco novo isolado preservado | 19 tabelas incluindo flyway_schema_history; 3 arquivos / 19422 bytes; 6,233 s | [restauracao-resumo.json](evidencias/restauracao-resumo.json) |
| 24/11/2026 / execução coordenada pelo líder | Python pow para RSA didático | 13 7 1 | Cálculos reproduzíveis em teoria-computacao.md |

## Roteiro individual

Cada integrante deve selecionar uma história, localizar UML e implementação, explicar uma decisão e apresentar um teste associado com resultado obtido. Atribuições e contribuições individuais: PENDENTES de registro pela equipe; nomes de donos de módulos não comprovam contribuição realizada.

## Evidências do navegador e limites

Registro legível: [e2e-duas-instancias.json](evidencias/e2e-duas-instancias.json), com 12 checks e `passed=true`. Imagens: [home](evidencias/01-home.png), [pontos](evidencias/02-pontos.png), [rejeição](evidencias/03-rejeicao.png), [consulta em outra instância](evidencias/04-outra-instancia.png), [documentos](evidencias/05-documentos.png), [móvel](evidencias/06-mobile.png).

A execução criou geradora fictícia, consultou pontos pela AVL, rejeitou categoria inválida no servidor, criou solicitação 6, consultou e editou por outra instância, aceitou/agendou com coletora de seed, rejeitou edição posterior com HTTP 409, recuperou upload PNG em B após envio em A com SHA igual, registrou destinação pela Procedure, concluiu gerando dois PDFs e baixou CDF em B. SHA-256 do PDF consta no JSON. Os testes com mídias e negativa a outra empresa são casos IT separados; a alternância entre instâncias no E2E não equivale a acesso entre empresas distintas.

A restauração foi feita em banco novo com nome isolado, preservado para conferência; os 6,233 s medem aquele ensaio local, não comprovam RTO futuro, failover, retenção automática ou backup fora do anfitrião. Rede entre dois computadores não foi validada: não há segunda máquina disponível, o firewall não foi aplicado por ausência de privilégios administrativos e o IP do anfitrião mudou durante o trabalho. Loopback saudável não libera acesso remoto.
