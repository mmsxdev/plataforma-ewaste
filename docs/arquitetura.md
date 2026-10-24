# Arquitetura

Monólito modular em Spring Boot: um único deploy, com o código dividido em módulos de dono único (pacotes em `br.com.ewaste`).

- Telas renderizadas no servidor com Thymeleaf; JavaScript só no mapa (Leaflet) e no contador de notificações.
- Cada módulo expõe para os outros apenas o pacote `api` (interfaces + records). O teste `ArquiteturaTest` garante isso.
- Efeitos colaterais (alertas, notificações, cobrança) viajam por eventos de domínio em `shared/events`. Eles são publicados com `ApplicationEventPublisher` e ouvidos com `@TransactionalEventListener`.
- Segurança por prefixo de rota: `/geradora/**`, `/coletora/**` e `/admin/**`, cada um restrito ao perfil correspondente.
- Banco PostgreSQL com esquema versionado por Flyway. O Hibernate só valida (`ddl-auto: validate`).

Fluxo principal: homologação libera a coletora → a geradora cria a solicitação → o roteamento alerta as coletoras aptas no raio → a primeira aceita e agenda → a coleta e as mídias são registradas → os documentos (CDF, laudo, certificado) são emitidos → a geradora baixa os documentos no painel. O mapa público é independente desse fluxo.

Os contratos entre módulos estão em [contratos.md](contratos.md).
