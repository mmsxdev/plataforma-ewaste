# Guia de uso e demonstração do E-Waste

Este guia apresenta o que a interface de demonstração disponibiliza. Configuração técnica: [guia-tecnico.md](guia-tecnico.md). Critérios e resultados: [testes.md](testes.md). Use somente empresas, pessoas e materiais fictícios no ensaio acadêmico.

## 1 Entrar e escolher o perfil

Na página inicial, consulte pontos sem autenticação ou cadastre uma empresa geradora/coletora. O cadastro solicita dados da empresa e do responsável; o sistema valida CNPJ, e-mail, senha e localização. Escolher coletora não libera automaticamente sua atuação: a aptidão depende da homologação existente. ADMIN não é cadastrado publicamente.

Faça login com a conta criada. Os usuários dos seeds existem somente em ambiente preparado para demonstração; consulte o operador. Nunca reutilize senha pessoal. A navegação de GERADORA oferece solicitações e documentos; COLETORA oferece oportunidades e coletas. Envio/análise administrativa de homologação, financeiro e notificações não possuem interface completa neste protótipo e não devem ser apresentados como ações disponíveis.

## 2 Consultar pontos e solicitar descarte

1. Abra **Pontos de coleta** (`/mapa`). Informe cidade e categoria; consulte local, horário e materiais aceitos. A consulta é textual e não exige mapa cartográfico.
2. No ponto compatível, escolha **Registrar solicitação para este ponto**. Faça login como geradora se necessário; se a navegação não retomar a escolha, volte à consulta após autenticar.
3. Confira ponto/categoria selecionados. Informe descrição, localização, peso estimado e os itens com identificação (ex.: “Notebook fictício”), categoria, quantidade e peso. Registre mídias apenas quando fizerem parte da massa fictícia preparada.
4. Envie. Um ponto indisponível ou incompatível deve provocar mensagem de rejeição, sem pedido parcial. Corrija o material ou volte à consulta.
5. Na solicitação criada, confira situação e histórico. A escolha do ponto informa o destino; não significa que uma coletora já assumiu. O modelo B2B mantém roteamento para coletoras aptas no raio.

Coleta no endereço sem ponto continua disponível por compatibilidade com o fluxo existente. Não apresentar essa alternativa como prova de que entrega presencial foi implementada; a suficiência do recorte de coleta foi informada pelo usuário como confirmada pelos professores, sem alegar que existe entrega presencial.

## 3 Corrigir e acompanhar

A geradora dona pode corrigir descrição e identificação dos itens enquanto a solicitação está **ABERTA**. Essa edição não muda categoria, quantidade, local e ponto já usados no roteamento. Depois do aceite, a tentativa é recusada para preservar consistência. Não use outra empresa para acessar o pedido: a autorização é verificada no servidor, não apenas na visibilidade de botões.

O histórico deve permanecer consultável quando um ponto deixa de receber novos pedidos. A apresentação de solicitações legadas sem ponto/identificação não autoriza inventar valores para preencher o passado. Confira os resultados do teste dessa compatibilidade na rodada vigente.

## 4 Executar coleta e consultar documentos

Uma coletora apta entra em Oportunidades, aceita a solicitação e agenda uma data futura no horário de São Paulo. Somente a vencedora registra a operação. Em Minhas coletas, registra peso real e foto JPG/PNG opcional, destinação e, se declaradas, todas as mídias com os dados de destruição.

A conclusão exige coleta, destinação e todas as mídias previstas. Sem esses requisitos, deve ser rejeitada. Ao concluir, são emitidos CDF e laudo; certificado de destruição somente se houver mídias. São documentos simulados sem validade legal. A geradora baixa apenas os próprios arquivos. A verificação pública confere integridade por hash; não oferece acesso geral aos PDFs.

## 5 Demonstração de sucesso e rejeição

- Sucesso: cadastro fictício → pontos/categoria → material identificado → solicitação → histórico → edição em ABERTA → aceite/agendamento → execução/destinação → conclusão → documentos.
- Rejeição: escolher categoria não aceita e enviar; demonstrar mensagem e ausência de pedido parcial. Outra opção: tentar editar depois do aceite e apresentar recusa por regra de estado.
- Compartilhamento local: abrir duas instâncias team no mesmo computador, criar em A e consultar/baixar em B. Declarar exatamente esse escopo; não equivale a teste em dois computadores.

Evidências históricas estão em [evidencias](evidencias/). A rodada E2E de fechamento está em evidencias/fechamento-20261124/resultado.json e passou12/12; os demais resultados estão em testes.md; imagens antigas não devem ser sobrescritas para parecerem atuais.

## 6 Limites e cuidados

Não demonstrar homologação administrativa/financeiro/notificações como telas prontas. Não usar dados pessoais reais, importar fotos reais ou expor credenciais. Problemas de acesso ao banco/rede são tratados pelo operador, sem reset/limpeza dos dados. As divergências originais e confirmações posteriores de datas/grupo/B2B estão em [requisitos](requisitos.md) e [equipe](equipe.md). A nova anfitriã será a máquina de Felipe; a transferência e o teste entre computadores ainda não aconteceram. Não apontar clientes para cópias divergentes durante a preparação.
