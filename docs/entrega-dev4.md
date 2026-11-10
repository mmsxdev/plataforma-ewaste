# Dev 4 — Rastreio e documentos

## Como explicar o fluxo

1. A solicitação guarda o estado da coleta. O módulo `rastreio` guarda o histórico de cada ação.
2. A coletora vencedora registra peso real, fotos, mídias destruídas e destinação. Só ela pode alterar essa coleta.
3. A conclusão exige retirada e destinação. Se houver mídias declaradas, todas precisam estar registradas. A mudança de estado usa `TransicaoSolicitacao`, do módulo do Davi.
4. Ao concluir, o módulo `documento` gera os PDFs simulados, salva os arquivos fora da pasta pública e registra o hash SHA-256 e um código de verificação.
5. A geradora baixa apenas os próprios documentos. A página pública de verificação compara o arquivo guardado com o hash original, sem oferecer download.

O CDF e o laudo são emitidos em toda coleta concluída. O certificado de destruição é emitido quando a solicitação contém mídias.

## Roteiro manual

1. No perfil `dev`, entre como `coletora@teste.com` e abra a solicitação de 250 kg com 5 HDs em **Oportunidades**. Aceite e agende a retirada.
2. Abra **Meu painel → Minhas coletas**, selecione a coleta, informe o peso real e, se quiser, anexe uma foto JPG ou PNG.
3. Registre os cinco números de série, o tipo, o método e o nível de destruição. Registre a destinação final e conclua.
4. Entre como `geradora@teste.com`, abra **Meus documentos** e baixe o CDF, o laudo e o certificado. O certificado contém os cinco números de série.
5. Abra o link **Verificar** de um documento. O resultado deve ser **Documento confirmado**.

## Testes

`./mvnw verify` roda o teste `FluxoRastreioIT` com PostgreSQL real. Ele cobre a regra R3, o conteúdo do PDF, o download negado à outra geradora, o hash alterado e a linha do tempo automática.
