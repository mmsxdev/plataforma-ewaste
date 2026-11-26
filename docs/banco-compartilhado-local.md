# PostgreSQL e arquivos compartilhados da equipe

## Decisão e justificativa acadêmica

Uma única máquina anfitriã executa PostgreSQL 16 pelo Compose. Cada integrante roda o Spring Boot na própria máquina com o perfil `team` e a URL do MESMO servidor. Docker fixa a versão principal, torna a configuração reproduzível e mantém os dados no volume persistente `ewaste-team-pg16`. O compartilhamento decorre da conexão à mesma instância; Docker/Git não sincronizam cinco bancos.

No perfil `team`, anexos e PDFs de até 10 MB são persistidos como BYTEA em `arquivo_compartilhado`, pela implementação `ArmazenamentoCompartilhado`. As referências continuam nas tabelas de negócio e os controllers continuam verificando o proprietário antes do download. Todas as aplicações recuperam os mesmos bytes, sem SMB nem cinco pastas `uploads` diferentes. Um backup lógico inclui esses arquivos e suas referências no mesmo snapshot. A opção é adequada ao protótipo pequeno; aumenta volume/WAL e tempo de recuperação e não é uma proposta de storage para grande escala.

Fora de `team`, `ArmazenamentoEmDisco` preserva `uploads`. Nada é importado ou apagado automaticamente. DBeaver/pgAdmin são clientes opcionais; nenhum deles cria sincronização entre servidores.

## Transição operacional aprovada em 24/11/2026

A máquina de **Felipe Rohnelt será a futura anfitriã** do PostgreSQL, mas a transferência ainda não ocorreu. A origem em Miguel permanece preservada até backup/restauração, conferência de dados/arquivos/privilégios e teste real entre computadores. **Rafael Camargo será operador do banco; Felipe cuidará das cópias e recuperação.** Não operar cópias divergentes como bancos comuns nem trocar URLs antes de validar o destino.

A sequência e as responsabilidades estão em [operação](operacao-banco.md) e [equipe](equipe.md). Não foram configurados IP/firewall/VPN nem comprovada rede por essa decisão. Como Felipe hospedará o banco, backup somente em seu computador não será externo: **o destino protegido fora dele continua pendente**.

## Preparação da anfitriã

1. Inspecionar `docker context show`, `docker ps -a`, `docker volume ls` e os rótulos dos volumes antes de iniciar. Não apagar/reutilizar volume desconhecido e não usar `down -v`. Volume anônimo preservado não comprova que dados antigos não existam.
2. Somente se não houver `.env`, copiar `.env.example` e preencher senhas fortes distintas de operador e aplicação. `.env`, backups e uploads estão ignorados no Git. Não publicar `docker compose config` completo: ele expande senhas.
3. O Compose tem projeto `ewaste-team` e volume explícito `ewaste-team-pg16`. Se esse nome já pertencer a outro projeto, interromper. `POSTGRES_*` e o init de roles só inicializam volume vazio; não redefinem senhas de volume existente.
4. Inicialmente manter `DB_BIND_IP=127.0.0.1`, `DB_URL=jdbc:postgresql://127.0.0.1:5432/ewaste`. Executar `docker compose config --quiet` e `docker compose up -d db`. Conferir saúde. Essa etapa não libera acesso a colegas.
5. Aplicar somente a revisão combinada e testada pelo operador: `.\infra\Migrate-Team.ps1 -ApprovedRevision '<SHA_COMBINADO_40_HEX>'`. O perfil usa credencial `DB_USER/DB_PASSWORD`, aplica Flyway e encerra. Para uma instância NOVA de demonstração, o operador pode usar `migrate,dev` após revisar os seeds fictícios. Não usar `dev` no banco comum como rotina de cada integrante.
6. A role `DB_APP_USER` é criada sem SUPERUSER, CREATEDB ou CREATEROLE, com operações de dados e sem CREATE no schema. O operador retira acesso ao histórico Flyway depois de migrar. Os quatro recursos SQL são invoker; a conta técnica não equivale a uma identidade humana. Auditoria acadêmica não é trilha imutável contra quem tem CRUD.

## Publicação somente para os colegas

Reservar IP privado/DHCP para a anfitriã. `localhost` na máquina do colega aponta para ela mesma. Antes de alterar o bind, no **PowerShell Administrador**, definir apenas IPs privados individuais autorizados:

```powershell
.\infra\Set-TeamAccess.ps1 -HostAddress '<IP_PRIVADO_ANFITRIA>' -TeamAddresses @('<IP_PRIVADO_COLEGA>')
```

O script não muda a categoria da rede. Cria permissão para os IPs informados e bloqueios IPv4 complementares para a porta 5432, incluindo quando há regras anteriores permissivas. Não abre roteador, internet ou todos os endereços da LAN. Se regras E-Waste já existirem, o script interrompe para conferência; não apaga regras de terceiros. Docker Desktop/WSL pode exigir conferência adicional de firewall; a prova é testar origem autorizada e outra não autorizada.

Só depois da proteção, ajustar no `.env` da anfitriã `DB_BIND_IP=<IP_PRIVADO_ANFITRIA>` e o host de `DB_URL` para esse mesmo IP; executar `docker compose up -d db`. Confirmar o IP ATUAL com `Get-NetIPAddress`; ele mudou durante esta execução, portanto não copiar um IP antigo de um log. Se Docker não puder vincular o endereço, conferir a interface e voltar ao bind local sem apagar o volume. Esta sessão não tinha privilégio de administrador e não executou a liberação da rede.

## Configuração de cada integrante

Copiar `.env.team.example` para `.env` sem sobrescrever configuração existente, receber por canal privado somente `DB_APP_USER/DB_APP_PASSWORD` e configurar `DB_URL=jdbc:postgresql://IP_PRIVADO_ANFITRIA:5432/ewaste`. Nunca distribuir a senha do operador. Rodar:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=team'
```

Não executar Compose nas máquinas clientes para o trabalho em equipe. Não combinar `team` com `dev` ou `migrate`. `team` desliga Flyway e usa Hibernate apenas para validar o esquema. Branches com migrations novas/incompatíveis usam Testcontainers ou banco descartável isolado; somente a versão combinada é promovida pelo operador. Não executar testes destrutivos sobre o banco comum.

DBeaver/pgAdmin: host privado da anfitriã, porta 5432, banco `ewaste`, usuário limitado e senha local. Em redes diferentes, uma VPN privada com roteamento e regras para os IPs da equipe é necessária antes de assumir acesso. Sem LAN/VPN não há conexão simultânea. A anfitriã precisa permanecer ligada.

## Importação explícita de uploads legados

Primeiro executar backup e conferir qual pasta contém os arquivos antigos. Não há `uploads` prévio no clone; Git não recupera arquivos ignorados nem volumes Docker antigos. Se existirem referências antigas não importadas, o modo team recusa o download, sem buscar silenciosamente uma pasta diferente por máquina.

Com `STORAGE_DIR` apontando à pasta original e a credencial do operador:

```powershell
.\infra\Migrate-Team.ps1 -ApprovedRevision '<SHA_COMBINADO_40_HEX>' -ImportLegacyUploads
```

A rotina lê referências existentes de documentos/evidências, verifica caminho dentro da raiz e assinatura, preserva o identificador, grava os bytes sem substituir registros existentes e mantém os arquivos originais. Referência sem arquivo seguro interrompe a importação; o lote é transacional. Não atribui dados novos a arquivos perdidos.

## Backup, recuperação e aceite

Ver [operação do banco](operacao-banco.md). `infra/Backup-Team.ps1` exporta snapshot e manifesto SHA-256; `infra/Test-Restore.ps1` restaura somente em banco novo GUID, compara tabelas/assinaturas/bytes e preserva a cópia. Dump inclui BYTEA; uploads legados precisam cópia separada. Copiar o backup para destino protegido fora do host é uma ação operacional ainda necessária, não um resultado garantido pelo volume Docker.

O ensaio automatizado `tools/validar-fluxo.mjs` usa dois processos Spring locais: criação em A/consulta em B e upload em A/download em B. Isso não substitui teste em dois computadores. Quando houver outro computador: liberar seu IP, verificar conexão/autenticação, criar registro em uma máquina, consultar na outra, enviar imagem e recuperar o PDF com a conta autorizada. Registrar hora, máquinas, resultado e hash; testar também uma conta não autorizada. Não declarar a rede validada antes desse ensaio.
O script do operador confere HEAD e fontes limpas, empacota e executa o JAR sem DevTools. Isso evita interpretar o exit code do Maven como prova de inicialização: no modo de desenvolvimento uma falha em restartedMain pode coexistir com BUILD SUCCESS. Seeds repetíveis ausentes no perfil de operação são ignorados na resolução, sem ignorar migrations versionadas ausentes nem executar repair.
