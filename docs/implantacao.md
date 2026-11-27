# Configuração, implantação e demonstração

Decisão aprovada: uma instância PostgreSQL 16 compartilhada pela equipe, preservando o monólito Java 21/Spring Boot 3.5.16/Thymeleaf. Cada computador pode executar sua instância do aplicativo; o banco e os arquivos do perfil **team** são comuns. UML: [implantacao.puml](uml/implantacao.puml).

## Transição operacional aprovada em 24/11/2026

A máquina de **Felipe Rohnelt será a futura anfitriã** do PostgreSQL, mas a transferência ainda não ocorreu. A origem em Miguel permanece preservada até backup/restauração, conferência de dados/arquivos/privilégios e teste real entre computadores. **Rafael Camargo será operador do banco; Felipe cuidará das cópias e recuperação.** Não operar cópias divergentes como bancos comuns nem trocar URLs antes de validar o destino.

A sequência e as responsabilidades estão em [operação](operacao-banco.md) e [equipe](equipe.md). Não foram configurados IP/firewall/VPN nem comprovada rede por essa decisão. Como Felipe hospedará o banco, backup somente em seu computador não será externo: **o destino protegido fora dele continua pendente**.

## Perfis e responsabilidades

- **dev:** desenvolvimento isolado, Flyway e seeds de demonstração, arquivos em disco por padrão. Não apontar casualmente para o banco da equipe.
- **team:** credenciais/URL explícitas, Flyway desligado, Hibernate valida esquema. Aplicativo não migra e usa papel SQL com privilégios mínimos. Faltas de configuração devem impedir inicialização, sem fallback para localhost/senha padrão.
- **Operador autorizado:** prepara banco, executa migrações com credencial separada, cria permissões, faz backup/restauração e comunica versão de esquema. Segredos nunca entram no Git ou nos relatórios.

Detalhes operacionais e scripts vigentes: [banco-compartilhado-local.md](banco-compartilhado-local.md). Este documento não escolhe IP definitivo nem abre rede/firewall automaticamente. Endereços de exemplo são placeholders; conexão de colegas e uso em duas máquinas estão **PENDENTES**.

Estado histórico de rede do preparo local: publicação na interface de rede falhou no bind do Docker; usado loopback temporário. Rede identificada como pública, sem alteração administrativa de firewall. Isso permite validação local, não comprova acesso dos colegas. O IP detectado mudou de 10.21.145.125 para 192.168.2.7 durante o trabalho; nenhum deles é endereço definitivo escolhido para a equipe. PostgreSQL em localhost:5432 está saudável; firewall não foi aplicado, não houve privilégio administrativo e naquele ensaio não havia segunda máquina disponível. A escolha posterior de Felipe como futura anfitriã não transforma esse resultado em teste de rede.

## Arquivos compartilhados

Em team, `ArmazenamentoCompartilhado` persiste bytes e metadados em `arquivo_compartilhado` (V200), usando o mesmo PostgreSQL. Isso evita caminhos locais distintos e dependência de SMB para o protótipo; um `pg_dump` consistente inclui os bytes. O modo fora de team preserva `ArmazenamentoEmDisco` e a pasta uploads existente. Arquivos legados só são importados explicitamente pelo operador, sem apagar a origem, após backup e conferência de referências.

Tradeoff: os PDFs/fotos aumentam volume, WAL, tempo de backup e restauração; limite por arquivo deve ser mantido e recuperação medida. Esta decisão simplifica a demonstração acadêmica pequena; não é recomendação de storage para grande escala.

## Configuração proposta e comandos

Conferir os nomes finais das variáveis em `application-team.yml` e no guia operacional antes de usar. Não copiar senhas de exemplo. Na raiz do repositório, com JDK 21 selecionado:

```powershell
java -version
docker version
# Instância isolada para demonstração local, após configurar .env conforme .env.example:
docker compose up -d
.\mvnw.cmd -B verify
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
# Equipe: somente após operador preparar esquema/papel e fornecer configuração privada:
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=team'
```

O `verify`, o perfil team com duas instâncias locais e o fluxo de demonstração foram exercitados; resultados e limites em [testes](testes.md). Configuração da equipe remota e comandos com endereço definitivo permanecem propostos. Não usar `docker compose down -v` nos dados que precisam ser preservados. `.env` e backups não devem ser versionados. Configuração local e instância compartilhada são alternativas, não passos para criar um segundo banco obrigatório.

## Demonstração funcional

1. Conferir `/actuator/health`, versão do esquema e perfil sem expor credenciais.
2. Usar contas e empresas fictícias; consultar `/mapa` (lista textual de pontos), filtrar cidade/categoria.
3. Iniciar solicitação pelo ponto, identificar material/categoria/quantidade; consultar detalhe e histórico.
4. Editar informação enquanto ABERTA; tentar categoria incompatível ou edição após aceite para demonstrar rejeição.
5. No percurso estendido existente, coletora apta aceita/agenda, registra execução e conclui; geradora consulta documentos simulados.
6. Demonstrar teste da AVL e SQL acadêmico em instância isolada. Conferir acesso indevido negado.
7. Em team, validar banco/documento em uma segunda máquina autorizada; até esse ensaio o requisito operacional permanece não verificado.

## Backup e recuperação

Proposta inicial para o protótipo: backup lógico diário e antes de migração/importação; cópia fora do host; retenção 7 diários/4 semanais, sujeita a recursos e validação da equipe. **Metas propostas** RPO 24h e RTO 2h; não são garantias nem medições. O dump inclui dados e arquivos binários do team; guardar também procedimento de roles/configuração privada em destino protegido. Disco legado requer cópia separada enquanto existir.

Restaurar em banco novo isolado, conferir contagens/FKs/bytes/hash e recuperar um documento pelo aplicativo antes de aceitar a cópia. Medir início/fim e perda simulada de dados, registrar em testes.md. Não restaurar por cima do banco compartilhado durante ensaio. Falha do host comum afeta todos; disponibilidade alta não faz parte desta entrega.

## Resultado verificado em 24/11/2026

Na rodada histórica anterior ao fechamento, o build completo passou: 229 testes unitários e 91 IT, zero falhas/erros/pulos. No Chrome headless, duas JVMs nas portas 18080 e 18081 conectadas ao mesmo PostgreSQL percorreram 12 verificações aprovadas: cadastro fictício, consulta de pontos/AVL, POST de categoria incompatível rejeitado, solicitação 6, consulta em B, edição ABERTA, aceite/agendamento, edição posterior HTTP409, upload PNG compartilhado, destinação pela Procedure, conclusão com dois PDFs e download do CDF em B. Móvel a 390px e foco por Tab foram exercitados, sem auditoria completa de acessibilidade. [Evidência E2E](evidencias/e2e-duas-instancias.json).

Backup e restauração por scripts foram executados em banco novo isolado preservado: 19 tabelas incluindo o histórico Flyway e 3 arquivos somando 19422 bytes; tempo observado 6,233 s. [Resumo de restauração](evidencias/restauracao-resumo.json). Não foi feita cópia fora do anfitrião. O tempo do ensaio local não comprova RTO futuro, retenção nem disponibilidade remota. Duas JVMs no mesmo Windows não substituem teste com dois computadores.

No fechamento de 24/11/2026, `verify spotless:check` passou com 236 unitários + 95 IT e o E2E Chromium passou em 12/12. Evidências atuais: [resumo Java](evidencias/fechamento-20261124/testes-resumo.json) e [ensaio entre instâncias locais](evidencias/fechamento-20261124/resultado.json). Rede entre dois computadores continua pendente.

Roteiro de transferência autorizado, ainda não executado: [transferencia-felipe.md](transferencia-felipe.md). A restauração de ensaio não restaura privilégios; o operador precisa conferi-los antes de promover o banco.

## Destino revisto para VPS — decisão posterior

Em 25/11 a equipe substituiu o plano de anfitriã no PC do Felipe por uma VPS administrada por ele. A restauração local e a aplicação UP no computador de Felipe foram concluídas por ele; a evidência formal ainda deve ser anexada. A VPS ainda aguarda configuração/acesso e comprovação de dados/arquivos/rede/backup. O acesso dos integrantes será limitado ao projeto; administração geral da VPS permanece com Felipe. Não apresentar a VPS como implantada. O teste local de 26/11 usa somente a instância isolada na porta 15432.
