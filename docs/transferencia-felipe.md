# Transferência do PostgreSQL para a máquina do Felipe

Plano aprovado pela equipe em 24/11/2026. **Não executado neste registro:** depende de acesso à máquina do Felipe. A instância atual no Miguel e seus volumes permanecem preservados. Rafael é o operador do banco; Felipe responde pelas cópias e pela recuperação. Somente uma instância será a referência de escrita da equipe após a troca.

## Preparação sem alterar a origem

1. No Felipe, inspecionar Docker, containers, volumes e portas antes de iniciar qualquer serviço. Não reutilizar volume desconhecido ou substituir configuração privada existente. Usar PostgreSQL 16 e uma cópia do repositório na revisão combinada, com `.env` privado próprio e credenciais fortes. Não publicar senhas nem distribuir a credencial do operador.
2. Inicializar o destino com volume novo identificado, acesso somente local e sem aplicações da equipe escrevendo nele. A máquina do Felipe precisa estar disponível, com Docker, Java 21 e PowerShell 7. O caminho do repositório e o IP privado serão conferidos nela, não copiados do Windows do Miguel.
3. Combinar a janela de troca: interromper as gravações das aplicações na origem e não aplicar migrations durante a cópia final. Se houver nova gravação depois do backup, repetir a cópia final; não mesclar bancos divergentes automaticamente.

## Backup e restauração de conferência

Na origem, na raiz do repositório, o comando já existente é:

```powershell
pwsh -NoProfile -File infra/Backup-Team.ps1 -BackupRoot '<DIRETORIO_PRIVADO_DE_BACKUP>'
```

Transferir o pacote completo gerado, incluindo `database.dump`, `manifest.json` e uploads legados quando presentes, por canal privado. O perfil team inclui os bytes dos anexos/PDFs no dump; referências a disco legado exigem os arquivos correspondentes. Conferir SHA-256 na chegada.

No Felipe, depois de preparar o Compose e a configuração privada de destino:

```powershell
pwsh -NoProfile -File infra/Test-Restore.ps1 -BackupDirectory '<PACOTE_RECEBIDO>'
```

Esse script cria um banco GUID **novo**, preserva-o e compara tabelas, linhas, assinaturas e bytes. Não restaura por cima de `DB_NAME`. Os comandos existem e tiveram execução local histórica; a sequência entre os dois computadores permanece proposta, ainda não testada.

**O banco restaurado não fica pronto para uso pelos clientes só porque a comparação passou.** `Test-Restore.ps1` usa `--no-owner --no-privileges`. Rafael precisa conferir proprietário/roles e configurar, no banco de destino escolhido, CONNECT e USAGE, DML nas tabelas de aplicação, uso das sequências e privilégios padrão para objetos futuros. Revogar CREATE público e acesso da role app ao histórico Flyway; manter a app sem SUPERUSER/CREATEDB/CREATEROLE. O init do Compose não reaplica esses privilégios automaticamente a um banco restaurado novo. Conferir DML permitido e DDL negado antes da promoção. Não usar usuário de operador para contornar falta de permissões no aplicativo.

## Ensaio e promoção coordenada

1. Escolher explicitamente o banco restaurado validado e registrar o nome, revisão Git e versão Flyway; não apontar por engano para o banco vazio inicial. Testar primeiro acesso local com profile team, sem executar Flyway por cada cliente.
2. Configurar IP privado estável e acesso apenas aos IPs da equipe, usando `infra/Set-TeamAccess.ps1` no Windows com privilégio administrativo. Se estiverem em redes distintas, preparar VPN privada antes de pressupor conectividade. Não abrir a porta no roteador ou na internet.
3. Usar dados fictícios: criar um registro no aplicativo de um computador e consultá-lo no outro; enviar uma imagem e recuperar os mesmos bytes na outra instância; baixar um PDF e conferir integridade. Testar também uma origem não autorizada. Registrar computadores, revisão, horários, comandos, resultados e hashes, sem credenciais.
4. Somente após os testes, alterar as URLs privadas dos clientes de forma coordenada para o Felipe e habilitar as gravações no destino. Manter a origem sem novas gravações e preservada para recuperação. Se a promoção falhar antes de novas gravações no destino, retornar coordenadamente à origem. Se já houver dados novos no destino, parar e planejar recuperação para não perder registros; não reativar duas bases independentes.

## Backup externo e evidência necessária

Com o PostgreSQL no Felipe, uma cópia no mesmo computador não é externa. Falta definir diretório/recurso protegido fora dele, acesso e transporte das cópias. A máquina do Miguel pode ser avaliada como destino externo, mas isso ainda não está configurado nem foi comprovado. Não há agendamento novo autorizado/implementado por este roteiro.

Aceite: backup e restauração no destino aprovados; privilégios conferidos; duas máquinas compartilham registro e arquivo; origem não autorizada recusada; todos os clientes usam a mesma instância; cópia externa e recuperação com resultados registrados. Até lá, registrar **transferência pendente**, preservando as evidências locais anteriores em [testes](testes.md).
