# Ferramentas de demonstração

Node.js 22 ou superior e npm. As dependências de teste do navegador pertencem a este diretório, não à aplicação Java. Não dependem de uma instalação do Codex, junction em target ou Chrome em caminho fixo.

## Bootstrap do navegador

Na raiz do repositório:

```powershell
npm.cmd ci --prefix tools
npm.cmd run browser:install --prefix tools
npm.cmd test --prefix tools
```

`npm ci` usa o lockfile versionado. `browser:install` baixa o Chromium compatível; requer rede na primeira preparação. Linux pode exigir bibliotecas de sistema adicionais; não se presume que estejam instaladas. Em máquina que já possua Chrome/Edge, alternativa explícita: `PI_BROWSER_CHANNEL=chrome` ou `msedge`. Outra opção é `PI_BROWSER_EXECUTABLE` apontando para executável escolhido pelo operador. Não usar as duas opções juntas; a configuração padrão é Chromium gerenciado.

## Ensaio que grava dados fictícios

Pré-condições: duas aplicações team nas portas locais escolhidas, conectadas a **um banco novo de demonstração isolado**, com migrations e seeds aplicados pelo operador. Nunca usar o banco comum da equipe como massa descartável. O script não cria banco, não executa Flyway e não corrige preparação automaticamente. A rede remota não é aceita pelo ensaio.

```powershell
$env:PI_DEMO_CONFIRM = 'FICTICIO'
$env:PI_APP_A = 'http://127.0.0.1:18080'
$env:PI_APP_B = 'http://127.0.0.1:18081'
npm.cmd run demo --prefix tools
```

O banco deve possuir o ponto fictício Escola Circular compatível com Informática e incompatível com Bateria e a coletora fictícia apta dos seeds. A execução gera conta fictícia com domínio `.invalid`, solicitação e documentos, sem apagar registros anteriores. A confirmação explícita não inspeciona a conexão interna das aplicações: **o operador deve conferir que ambas apontam ao banco isolado**.

Agendamento padrão é calculado 48 horas à frente em `America/Sao_Paulo`, sem depender do fuso do Windows. Override opcional: `$env:PI_AGENDAMENTO='2027-02-01T14:00'`; a data precisa ser real e futura no momento da execução. Ajustar a data do exemplo; ela não é uma configuração permanente.

Cada rodada usa `target/evidencias/<run-id>/`, com JSON, PNG e PDF fictício. `PI_RUN_ID` pode escolher identificador simples único; se a pasta já existir, o script falha sem sobrescrever. Não mover novos resultados sobre `docs/evidencias/e2e-duas-instancias.json` ou outros históricos; copiar a rodada validada para uma pasta nova ao publicar evidências. O JSON registra versão Node/navegador, URLs locais, fuso/agendamento, resultado e horários UTC, sem senhas.

`npm test` verifica geração de datas, virada de ano, overrides inválidos/passados e restrição das URLs; não equivale ao E2E nem aos testes Java. Resultados executados ficam em [docs/testes.md](../docs/testes.md).

## PDFs acadêmicos

`gerar-relatorios.py` requer Python, reportlab e Pillow; conferir o ambiente antes de exportar. Para regenerar somente relatórios afetados:

```powershell
python -B -X utf8 tools/gerar-relatorios.py --only banco-dados design-software
```

O seletor mescla hashes antigos no manifesto e atualiza fontes Markdown/diagramas dos relatórios selecionados. Os outros PDFs e entradas permanecem preservados. Renderizar os PlantUML alterados antes da exportação; depois conferir visualmente os PDFs, tipos SQL/atributos e tabelas. Identificação de equipe/cargos nas capas continua pendente, sem atribuição inventada.
