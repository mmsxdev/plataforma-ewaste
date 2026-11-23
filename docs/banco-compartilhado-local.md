# Banco e arquivos compartilhados na rede local

## Decisão para o Projeto Integrador

Uma máquina da equipe será a **anfitriã**: nela, Docker Desktop executa uma única instância do PostgreSQL 16. Cada integrante pode executar sua própria cópia do Spring Boot e conectar ao mesmo endereço IP do PostgreSQL. O Git compartilha o código; o PostgreSQL compartilha os registros. Não há necessidade de contratar hospedagem para esta entrega educacional.

| Na máquina anfitriã | Nas máquinas dos integrantes |
| --- | --- |
| Docker Desktop, serviço `db`, volume `pgdata` e cópia de segurança | Código do mesmo repositório, Java 21 e `.env` local com `DB_URL` da anfitriã |
| IP privado estável ou reserva DHCP e porta 5432 liberada só para os IPs dos colegas | DBeaver ou pgAdmin opcional para inspeção SQL; aplicação Spring rodando localmente |
| Pasta de arquivos compartilhada para anexos e PDFs, se todas as instâncias executarem funções com arquivos | Acesso à mesma pasta de arquivos e permissões de leitura/gravação quando necessário |

Exemplo: anfitriã `192.168.1.50` e banco `ewaste`. Na máquina de cada integrante: `DB_URL=jdbc:postgresql://192.168.1.50:5432/ewaste`. O endereço é ilustrativo: descubra o IPv4 real no dia do ensaio. `localhost` sempre significa a própria máquina de quem está executando o comando. Logo, `localhost` em cinco computadores criaria cinco acessos distintos, não um banco compartilhado.

**Justificativa de Docker:** o Compose já fixa PostgreSQL 16, nome do banco, credenciais configuráveis, porta e volume persistente. `docker compose up -d db` cria container, banco inicial e volume quando faltarem, sem exigir instalação individual do servidor PostgreSQL. Flyway cria e atualiza o esquema ao iniciar a aplicação. Docker não sincroniza bancos em computadores diferentes: o compartilhamento vem da conexão de todos à **mesma instância**. DBeaver e pgAdmin são clientes administrativos dessa instância, não servidores nem meios de sincronização. Também seria possível instalar PostgreSQL diretamente na anfitriã, mas isso exigiria configuração e manutenção manual do serviço.

## Preparação da anfitriã (PowerShell, na raiz do repositório)

1. Confirme que Docker Desktop está ativo e examine `docker compose ps -a` e `docker volume ls` antes de criar ou reutilizar qualquer volume. A ausência do banco na tela do Docker Desktop não comprova a ausência de um volume antigo. Não execute `docker compose down -v`.
2. Copie `.env.example` para `.env` se ainda não houver `.env`. Defina `DB_PASSWORD` como uma senha forte, ajuste `DB_BIND_IP` para o IPv4 privado da anfitriã, por exemplo `192.168.1.50`, e ajuste também `DB_URL=jdbc:postgresql://192.168.1.50:5432/ewaste` nessa máquina (substitua o IP ilustrativo pelo endereço real). A publicação restrita ao IPv4 escolhido não atende conexões em `localhost`. Preserve qualquer `.env` existente até conferir seus valores; não o envie ao Git.
3. Execute `docker compose up -d db` e confira `docker compose ps`. A primeira inicialização do volume cria o banco `ewaste`; as migrations Flyway rodam quando a aplicação inicia. Se o volume já contiver um banco, suas variáveis `POSTGRES_*` não redefinem usuários, senhas ou dados existentes.
4. Permita conexões de entrada TCP 5432 **somente** dos IPs dos integrantes na rede privada do firewall do Windows. Não encaminhe a porta no roteador e não exponha o banco à internet. A anfitriã deve continuar ligada enquanto os demais estiverem trabalhando.
5. Inicie uma versão combinada da aplicação para aplicar as migrations previstas; não execute cada branch de desenvolvimento com migrations novas sobre o banco compartilhado.

A configuração `DB_BIND_IP` publica a porta apenas no endereço escolhido; por padrão, ela permanece em `127.0.0.1` para desenvolvimento individual. Se o IPv4 mudar, atualize a variável e recrie o serviço com `docker compose up -d db` depois de confirmar a situação do volume.

## Preparação dos integrantes

1. Atualize o código até a revisão combinada pela equipe. Copie `.env.example` para `.env`, preencha `DB_USER` e `DB_PASSWORD` conforme a anfitriã e altere **apenas a URL da conexão** para `jdbc:postgresql://IP_DA_ANFITRIA:5432/ewaste`. O arquivo fica local e fora do Git.
2. Confirme a conexão com DBeaver ou pgAdmin usando host = IP da anfitriã, porta = 5432, database = ewaste e as credenciais acordadas.
3. Rode `.\mvnw.cmd spring-boot:run` localmente. Não precisa executar `docker compose up` na própria máquina para usar o banco compartilhado. Acesse a sua aplicação em `http://localhost:8080`.
4. Crie um registro fictício na aplicação de um integrante e consulte o mesmo registro a partir da aplicação de outro. É o teste de aceite para os dados relacionais compartilhados.

Conexões por VPN privada podem cumprir o mesmo papel quando os integrantes não estão na mesma rede, desde que a anfitriã e a regra de acesso sejam configuradas para essa rede. Sem LAN ou VPN e sem hospedar a aplicação/banco, não há sincronização simultânea entre máquinas.

## Migrations, testes e arquivos

- O esquema compartilhado deve acompanhar uma revisão única do código. Branches com migrations incompatíveis não devem apontar ao mesmo banco. Para alterações de esquema em elaboração e testes destrutivos, cada integrante pode usar um banco local descartável e voltar ao banco comum após a integração.
- O perfil `dev` inclui seeds e `out-of-order`; use-o no banco comum somente se a equipe decidir carregar dados fictícios e tiver revisado essas seeds. O comando de execução acima não ativa esse perfil.
- Os testes `*IT.java` usam Testcontainers: executá-los não é a forma de popular nem validar o banco comum.
- Os anexos e PDFs são gravados em `STORAGE_DIR` no disco e o banco guarda a referência. Cada aplicação em outra máquina apontando a `./uploads` diferente verá os mesmos registros, **mas não os mesmos arquivos**. Para testar uploads e downloads em todas as instâncias, disponibilize uma pasta de rede com permissões para a equipe, mapeie-a em todas as máquinas no mesmo caminho, por exemplo `Z:/ewaste-uploads`, e configure `STORAGE_DIR` com esse caminho. Teste escrita em uma máquina e leitura em outra. Alternativamente, centralize a execução dos fluxos de arquivos em uma única instância da aplicação até montar a pasta compartilhada.
- Faça backup periódico do banco com `pg_dump` e da pasta de uploads; volume persistente não substitui backup. Antes da apresentação, ensaie a restauração com dados fictícios e registre tempo e resultado.

## Limites e aceite

A solução depende da anfitriã ligada, conectividade entre as máquinas e endereço conhecido. O aceite exige: conexão de dois integrantes ao mesmo banco, criação/consulta cruzada, aplicação das migrations da revisão combinada, acesso cruzado a um arquivo quando esse fluxo for demonstrado, e cópia restaurável do banco e dos arquivos.
