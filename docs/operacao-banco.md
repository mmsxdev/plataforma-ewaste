# Operação, permissões, backup e recuperação

## Responsabilidades e preservação

O Compose usa PostgreSQL 16, projeto `ewaste-team` e volume `ewaste-team-pg16`. Somente o operador usa `DB_USER/DB_PASSWORD` e perfil `migrate`. Cada aplicação usa `DB_APP_USER/DB_APP_PASSWORD` e perfil `team`, sem Flyway nem CREATE no schema. A credencial do operador não é distribuída. O init de roles só roda em volume vazio; volume existente exige inspeção e concessões explícitas, nunca reinicialização destrutiva.

Antes de criar o banco foram listados containers, volumes e rótulos: nenhum identificado como E-Waste. Volumes anônimos e recursos de outros projetos foram preservados; sua existência não permite afirmar ausência de dados antigos. O clone também não recupera `.env`, volumes ou `uploads` antigos.

## Transição operacional aprovada em 24/11/2026

A futura anfitriã do PostgreSQL compartilhado será **a máquina de Felipe Rohnelt**, por decisão da equipe. **Rafael Camargo é o operador do banco/migrador; Felipe responde pelas cópias e ensaios de recuperação.** A instância atual em Miguel permanece preservada até a transferência ser executada e validada. Esta decisão documental não indica que o banco foi movido, que IP/firewall/VPN estejam configurados ou que o acesso entre computadores já tenha passado.

Antes da troca: inventariar o ambiente de Felipe sem reutilizar volumes desconhecidos; fixar a revisão do esquema; produzir backup consistente do banco e arquivos; restaurar em destino isolado, conferir contagens/bytes/hash/privilégios e uma operação/download; preparar acesso apenas às máquinas autorizadas e executar o teste cruzado real. Só depois promover a instância de Felipe e trocar as URLs dos clientes de forma coordenada. Durante a preparação, a cópia de destino não é outro banco comum para escrita independente. Não apagar a origem nem substituir volumes/dados para concluir o ensaio.

Com Felipe como anfitrião, uma cópia guardada somente em seu computador não protege contra a perda da anfitriã. **O destino protegido fora da máquina de Felipe continua pendente**, assim como o teste de transferência e o ensaio de rede entre as máquinas. Metas de frequência/retenção/RPO/RTO permanecem propostas, sem agendamento ou garantia novos. Ver [equipe e responsabilidades](equipe.md).

## Política proposta para o protótipo

- Backup diário e imediatamente antes de migração/importação. Meta de RPO: 24 horas; reduzir o intervalo se o grupo não aceitar perder um dia de trabalho.
- Meta inicial de RTO: 2 horas, incluindo provisionamento e conferência do aplicativo. Não é garantia de disponibilidade.
- Retenção proposta: 7 diários e 4 semanais. Não foi criado agendamento nem excluída cópia automaticamente.
- Cópia protegida fora da máquina anfitriã continua necessária. Backup no mesmo disco não protege contra sua perda.
- Configuração privada/credenciais de recuperação devem ser guardadas por canal protegido, fora do Git. O dump lógico não guarda senhas de roles globais.

## Exportação consistente

PowerShell 7, na raiz do projeto:

```powershell
.\infra\Backup-Team.ps1
```

O script mantém um snapshot PostgreSQL aberto para inventário e `pg_dump --snapshot`, usa formato customizado e compara SHA-256 do arquivo antes/depois da cópia. O manifesto guarda commit Git, horários UTC, contagens por tabela, assinaturas dos registros e total de bytes BYTEA. O SHA do Git registra HEAD; alterações locais ainda não commitadas não são automaticamente representadas por ele. Para uma versão de entrega, executar depois de fixar a revisão.

Em `team`, documentos e fotos estão em `arquivo_compartilhado`, portanto entram no mesmo snapshot que suas referências. A assinatura MD5 das linhas serve somente para comparação técnica de restauração; não é mecanismo de proteção criptográfica. A integridade de documentos no aplicativo continua usando SHA-256.

Se houver `uploads` legado, o script copia a pasta e registra SHA-256 por arquivo. Essa cópia de filesystem exige suspender escritas durante a cópia e não participa do snapshot SQL. Originais não são apagados.

## Recuperação de ensaio

```powershell
.\infra\Test-Restore.ps1 -BackupDirectory 'backups\PASTA_DO_BACKUP'
```

O script confere o SHA-256 antes de criar um banco, gera exclusivamente `ewaste_restore_<GUID>`, restaura em transação e compara tabelas, contagens, assinaturas e bytes. Nunca aponta para o banco comum, não usa `--clean` e não executa DROP. O banco de ensaio e os dumps temporários são preservados para revisão. O manifesto JSON do ensaio registra duração e resultado.

Esse ensaio não constitui plano automático de troca do banco da equipe. Em recuperação real, o operador deve recriar as roles, conferir privilégios, validar a revisão do esquema, apontar uma aplicação ao banco restaurado e conferir uma operação e download antes de promover a conexão. Não trocar URLs de todos os integrantes sem esse procedimento.

## ACID e limites

Cadastro e solicitação usam transações; aceite é condicional no banco, edição usa lock e rastreio serializa mudanças. O teste de rollback, a procedure e a trigger demonstram atomicidade e consistência; testes concorrentes demonstram conflitos. Durabilidade depende também de volume, WAL, disco e recuperação, e não só de `@Transactional`.

No perfil `team`, os bytes participam da transação JDBC do chamador: falha na emissão pode reverter bytes e metadados. No armazenamento local, gravações em disco não se revertem junto ao PostgreSQL. Eventos `AFTER_COMMIT` podem abrir outra transação; não alegamos uma única transação para todo o fluxo distribuído por eventos.

## Evidências desta entrega

Ver [testes e resultados](testes.md) e `docs/evidencias/`. Foi restaurada uma cópia fictícia, comparando inclusive anexos e PDFs. O tempo medido se refere ao volume pequeno desta demonstração; não comprova RTO futuro em outra máquina ou volume maior. RPO proposto requer cumprimento operacional da frequência de cópias.

## Referências

POSTGRESQL GLOBAL DEVELOPMENT GROUP. **PostgreSQL 16 Documentation: Backup and Restore; Database Roles; Transaction Isolation**. Disponível em: https://www.postgresql.org/docs/16/backup.html; https://www.postgresql.org/docs/16/user-manag.html; https://www.postgresql.org/docs/16/transaction-iso.html. Acesso em: 24 nov. 2026.

## Conferência do operador nesta execução

O script Migrate-Team.ps1 foi executado na revisão commitada com sucesso, sem recarregar seeds. Uma tentativa explícita de importação encontrou cinco referências fictícias do seed de homologação sem arquivos locais e interrompeu corretamente o lote; não foram inventados documentos nem removidas referências. Os três arquivos reais do ensaio team continuam disponíveis no banco. A importação com arquivo local válido, preservação da origem e idempotência foi aprovada em ArmazenamentoCompartilhadoIT.

No Windows, encerrar instâncias do aplicativo que estejam executando o mesmo target/*.jar antes de empacotar: o arquivo aberto pode impedir o repackage. A falha de empacotamento interrompe o script antes de tocar no banco. Não encerrar processos de outros projetos.

Roteiro de transferência autorizado, ainda não executado: [transferencia-felipe.md](transferencia-felipe.md). A restauração de ensaio não restaura privilégios; o operador precisa conferi-los antes de promover o banco.
