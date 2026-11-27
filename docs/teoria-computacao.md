# Relatório técnico — Lógica e Teoria dos Números

## 1 Introdução

O aplicativo E-Waste registra materiais e acompanha descarte. Este relatório relaciona requisitos concretos à lógica matemática e demonstra criptografia didática. Os requisitos acadêmicos estão no documento oficial (Faculdade SENAI FATESG, 2026, p. 4-5). A identificação da implementação e dos resultados está em [rastreabilidade](rastreabilidade.md).

## 2 Parte I — Lógica aplicada a requisitos

Domínio: usuários u, solicitações s, pontos p e documentos d; Auth(u) significa sessão válida, Dona(u,s) liga empresa do usuário à geradora, Vencedora(u,s) liga empresa do usuário à coletora que aceitou s e Aberta(s) é status ABERTA. Os conectivos seguem a lógica proposicional e de predicados (Rosen, 2019). As fórmulas especificam quando uma operação é permitida; não tornam uma chamada inválida segura por si só.

| Regra / requisito | Formalização | Interpretação e código relacionado |
| --- | --- | --- |
| RN01 / RF01 | Cadastrar(u) ⇒ EmailLivre(u) ∧ CnpjLivre(u) ∧ DadosValidos(u) | CadastroService e restrições de unicidade; uma verificação prévia não elimina corrida, o banco decide unicidade. |
| RN02 / RF06 | VerHistorico(u,s) ⇒ Auth(u) ∧ (Dona(u,s) ∨ Vencedora(u,s)) | SolicitacaoController mostra a linha do tempo à geradora dona e ColetaController à coletora vencedora; outra empresa do mesmo perfil recebe 404 e o perfil errado recebe 403. |
| RN03 / RF05 | CriarComPonto(s,p) ⇒ Ativo(p) ∧ Aceita(p,Categoria(s)) | Ponto selecionado precisa estar válido; legado sem ponto é uma exceção de compatibilidade, não teste de ponto ativo. |
| RN05 / RF07 | Editar(u,s) ⇔ Auth(u) ∧ PerfilGeradora(u) ∧ Dona(u,s) ∧ Aberta(s) | Edição restrita a descrição/identificação; lock e rechecagem impedem transição concorrente silenciosa. |
| RN06 / RF10 | Concluir(s) ⇒ Coletada(s) ∧ Destinada(s) ∧ (TemMidias(s) ⇒ TodasRegistradas(s)) | RastreioService.concluir e teste regraR3ExigeColetaDestinacaoETodasAsMidias; ausência de mídias torna a implicação interna verdadeira, sem dispensar coleta/destinação. |
| RN07 / RF08 | Baixar(u,d) ⇒ Auth(u) ∧ GeradoraDona(u,d) | DocumentoService e rota restrita. Conhecer ID não é autorização. |

### 2.1 Tabela-verdade de edição

Fixando sessão e perfil válidos, D = Dona(u,s), A = Aberta(s), P = D ∧ A:

| D | A | P |
| --- | --- | --- |
| F | F | F |
| F | V | F |
| V | F | F |
| V | V | V |

O último caso é o único permitido. A ausência de sessão/perfil válido torna a condição completa falsa mesmo se D e A forem verdadeiros.

### 2.2 Tabela-verdade de mídias para conclusão

Fixando coleta e destinação já registradas, M = TemMidias(s), T = TodasRegistradas(s), P = ¬M ∨ T, equivalente a M ⇒ T:

| M | T | ¬M ∨ T |
| --- | --- | --- |
| F | F | V |
| F | V | V |
| V | F | F |
| V | V | V |

Na semântica do aplicativo, T é a correspondência entre a quantidade declarada e a registrada. Sem mídias declaradas não há o que registrar, e a fórmula é verdadeira por vacuidade: as linhas com M falso mostram a implicação, não um caso de uso diferente. O teste real também verifica coleta e destinação, pois a tabela fixa essas premissas.

### 2.3 Tabela-verdade de consulta ao histórico

A = Auth(u), D = Dona(u,s), C = Vencedora(u,s) e P = A ∧ (D ∨ C):

| A | D | C | P |
| --- | --- | --- | --- |
| F | F | F | F |
| F | F | V | F |
| F | V | F | F |
| F | V | V | F |
| V | F | F | F |
| V | F | V | V |
| V | V | F | V |
| V | V | V | V |

Sem sessão, nenhuma combinação permite a consulta. Com sessão, basta ser a geradora dona ou a coletora vencedora; a disjunção é inclusiva. As linhas com D e C verdadeiros não ocorrem no aplicativo, porque uma empresa é GERADORA ou COLETORA (CHECK em `empresa.tipo`); elas completam a tabela lógica.

### 2.4 Ambiguidades e consistência

Os requisitos selecionados apresentam os seguintes pontos de análise:

a) ambiguidade: “usuário” não especifica pessoa física ou representante de empresa; o projeto preserva B2B, aceito pelos professores conforme equipe.md;

b) ambiguidade: “entrega ou coleta” não determina se ambos os modos são obrigatórios;

c) ambiguidade: “atualização de dados” não diz em quais estados os campos podem mudar; RN05 explicita o limite, com edição só de descrição e identificação e só em ABERTA;

d) ambiguidade: ponto selecionado não significa coletora vencedora; o roteamento B2B permanece separado do local informado;

e) contradição aparente: lida isoladamente, a funcionalidade 7 do enunciado (consulta, atualização e organização dos dados) permitiria gerenciar qualquer registro, o que conflita com a funcionalidade 8 (controle de acesso e proteção); RN02, RN05 e RN07 restringem cada operação à dona ou à vencedora, e as duas exigências passam a valer juntas;

f) inconsistência potencial: voltar uma solicitação CONCLUIDA a um estado anterior comprometeria o histórico e os documentos emitidos; RN04 só permite avançar, e `TransicoesSolicitacao` recusa regressões;

g) inconsistência encontrada nesta revisão: a versão anterior formalizava VerHistorico só para a geradora dona, mas a coletora vencedora também consulta a linha do tempo da coleta; como a fórmula é condição necessária, o código a violava, e RN02 passou a usar Dona ∨ Vencedora, como mostra a tabela-verdade de consulta ao histórico.

As inconsistências de datas e grupos do PDF estão em requisitos.md e não foram corrigidas silenciosamente. A confirmação geral do usuário sobre as interpretações e as datas adotadas está registrada em equipe.md; as contradições do documento original permanecem identificadas.

As regras revisadas são satisfatíveis em conjunto. Por exemplo, a geradora dona edita a solicitação enquanto ela está ABERTA (D e A verdadeiros na tabela-verdade de edição) e, depois da conclusão, ela e a coletora vencedora consultam o histórico. Não se constatou contradição formal entre as seis fórmulas.

## 3 Parte II — Teoria dos Números e criptografia

Nome/e-mail do responsável, telefone/endereço, senha, localização e documentos/evidências precisam proteção conforme finalidade e acesso (Stallings, 2017). No aplicativo, senha é protegida por **hash BCrypt**, que não se descriptografa; SHA-256 verifica integridade de documentos, não cifra seu conteúdo. HTTPS/SMB e proteção dos hosts dependem do ambiente. O exemplo a seguir **não está integrado à autenticação e não deve proteger dados reais**.

### 3.1 Conceitos

Divisibilidade: a divide b se existe inteiro k com b = ak. Primos possuem exatamente os divisores positivos 1 e eles próprios. O MDC identifica coprimalidade, necessária para o inverso modular usado aqui. Congruência a ≡ b (mod n) significa n divide a−b; aritmética modular opera com classes de restos. A dificuldade de fatorar produtos grandes é parte da motivação do RSA (Rivest; Shamir; Adleman, 1978), mas os números minúsculos deste exemplo são deliberadamente inseguros. Os conceitos seguem Rosen (2019).

| Conceito | Onde aparece no exemplo |
| --- | --- |
| Números primos | p = 5 e q = 11 formam n = 55 |
| Divisibilidade e MDC | e = 3 só serve porque MDC(3, 40) = 1, calculado pelo algoritmo de Euclides |
| Congruência | d é o inverso de e: 3·27 ≡ 1 (mod 40) |
| Aritmética modular | cifrar e decifrar são potências reduzidas módulo 55 |

### 3.2 Exemplo RSA calculável à mão

Escolhemos p=5 e q=11 (primos distintos), n=pq=55 e φ(n)=(p−1)(q−1)=40. Escolhemos e=3. Pelo algoritmo de Euclides: 40=13·3+1 e 3=3·1+0; o último resto não nulo é 1, portanto MDC(3,40)=1. Da identidade 1=40−13·3, o inverso de 3 módulo 40 é −13 ≡ 27. Logo d=27 e ed=81=2·40+1 ≡1 (mod 40). Chave pública (e,n)=(3,55); privada (d,n)=(27,55). Em uso real p, q e φ(n) não seriam divulgados: quem fatora n calcula φ(n) e, com ele, d.

Mensagem numérica fictícia m=7, com 0≤m<n e MDC(7,55)=1. Cifragem c=m^e mod n: 7³=343=6·55+13, então **c=13**.

Decifragem por quadrados sucessivos: 13²=169 ≡4; 13⁴≡16; 13⁸≡256≡36; 13¹⁶≡1296≡31 (mod 55). Como 27=16+8+2+1:

13²⁷ ≡31·36·4·13 ≡16·4·13 ≡9·13 ≡117 ≡ **7** (mod 55).

Checagem dos restos: 31·36=1116=20·55+16; 16·4=64=55+9; 9·13=117=2·55+7. Recuperamos m. Pela relação ed=1+2φ(n), o teorema de Euler para m coprimo a n também justifica m^ed ≡m. A prova geral de RSA não se limita a esta escolha coprima; este relatório demonstra este caso específico.

Verificação reproduzível sem dados reais: `python -c "print(pow(7,3,55), pow(13,27,55), (3*27)%40)"` deve imprimir `13 7 1`. Registrar execução em testes.md; não confundir expectativa matemática com teste de segurança do aplicativo. Não há padding, tamanho de chave seguro, gestão de chaves ou codificação de mensagens neste exemplo.

## 4 Conclusão

A formalização expõe premissas de autorização/estado e evita interpretar condições isoladas como autorização completa. A própria revisão mostrou isso: ao conferir a fórmula da consulta ao histórico contra o código, a equipe encontrou o caso da coletora vencedora, que a regra escrita não cobria. O exemplo matemático mostra primos, MDC e congruências participando da construção de chaves e da recuperação da mensagem. A proteção efetiva do aplicativo depende de mecanismos e operação apropriados, não deste RSA didático.

## Referências

FACULDADE SENAI FATESG. **Projeto Integrador 2026-2: Engenharia de Software, 4º período**. Goiânia, 2026. 9 p. Documento fornecido à equipe, arquivo PI2026-2 ENG4-T1.pdf.

RIVEST, Ronald L.; SHAMIR, Adi; ADLEMAN, Leonard. A method for obtaining digital signatures and public-key cryptosystems. **Communications of the ACM**, [S. l.], v. 21, n. 2, p. 120-126, 1978. Disponível em: https://people.csail.mit.edu/rivest/Rsapaper.pdf. Acesso em: 23 nov. 2026.

ROSEN, Kenneth H. **Discrete mathematics and its applications**. 8. ed. New York: McGraw-Hill Education, 2019.

STALLINGS, William. **Cryptography and network security**: principles and practice. 7. ed. Boston: Pearson, 2017.
