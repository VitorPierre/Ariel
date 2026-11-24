# Ariel · Conto de Fadas (Java Swing, MVC)

Sistema desktop de locação de decoração (Engenharia de Software II · UniRios). Segue a Documentação v0.5.

```
src/
  app/         Main                         ponto de entrada: Login -> Sistema -> Sair -> Login
  model/       Cliente, Catalogo, Locacao, Pagamento, Ocorrencia, Devolucao, Documento,
               ServicoContratado, Usuario, Perfil, Etapa,
               Regras (valores das regras), Mensagens (MSG001–MSG020), RegraNegocioException,
               ExigeAutorizacaoException, ValidacaoException
                                            DADOS (sem Swing): o que existe no sistema
  view/        TelaLogin, TelaPrincipal (Painel), TelaClientes, TelaCatalogo, TelaOrcamentos,
               TelaLocacoes, TelaDevolucoes, TelaRelatorios, TelaConfiguracoes,
               DetalhesReserva, Ui, CampoSenha,
               Tema, BotaoUI, BarraRolagem, Logo   (camada visual: cores, fontes e formas)
                                            TELAS: só desenham e reagem a cliques
  controller/  LoginController, UsuarioController, PainelController, ClienteController,
               CatalogoController, RelatorioController, DocumentoController
                                            REGRAS DE NEGÓCIO: decidem o que pode ou não
  util/        Senha (PBKDF2), Pdf (gerador de PDF sem bibliotecas), Cpf (validação)
  recursos/    logo.png e fontes/ (Inter)   arquivos lidos pelo Tema; precisam estar no classpath
```

Como o MVC funciona aqui: a tela pede algo ao controller ("registrar retirada"); o controller
confere as regras e altera o model; se violar uma regra lança `RegraNegocioException` e a tela
mostra a mensagem; quando algo muda, o controller avisa e as telas se redesenham.

Executar: `compilar_e_executar.bat` (Windows) ou `compilar_e_executar.sh`. Requer JDK 17+ (testado no 21).

Visual: segue o **DESIGN.md** (rosa e branco, aprovado). Toda cor, fonte e forma vem de `view/Tema.java`;
nenhuma tela declara cor própria. Os scripts executam com `-cp out:src` para o Java achar a logo e as
fontes em `src/recursos`. Na IDE (IntelliJ, Eclipse, NetBeans), marque `src` como pasta de fontes e os
recursos são copiados sozinhos.

Tela cheia: tecla **F11**, botão na barra de título ou duplo clique na barra. **Esc** sai da tela cheia. Fechar o sistema (botão X ou Alt+F4) sempre pede confirmação.

Login de teste (troque antes de usar de verdade):
- administrador: `admin` / `admin123`
- funcionário: `funcionario` / `func123`

## Regras principais implementadas (Documentação Ariel 0.5 — versão oficial em Word)
- Orçamento vale 30 dias (fixo) e NÃO reserva estoque; só o sinal registrado reserva.
- Sinal de 10%, 20% ou 50% do total: o sistema valida o mínimo de 10% (UC05, MSG014).
- Só o ADMINISTRADOR registra pagamentos e gera contrato/recibo (UC07, UC13). O funcionário monta e salva
  o orçamento; a confirmação fica pendente até o Administrador registrar o sinal (UC05 FA06).
- Reserva confirmada (antes da retirada) pode ser ALTERADA: revalida o estoque sem contar ela mesma; com
  conflito mantém a anterior (MSG010); sem conflito grava e gera nova versão do contrato (UC05 FA02).
- Saldo quitado por inteiro ANTES da retirada; retirada só pelo titular ou terceiro autorizado.
- Devolução pelo titular ou qualquer pessoa identificada; não exige pagamento.
- Devolução PARCIAL: itens "Ainda não voltou" continuam pendentes (não viram perda); o que voltou já
  fica livre. A locação só vai para Devolvido quando tudo for conferido (UC16 FA02).
- Excluir (clientes e catálogo): SEM vínculos exclui (MSG005); COM vínculos DESATIVA e preserva o
  histórico (MSG006). Desativados podem ser reativados (UC03, UC06, UC08, UC09, UC10, UC12 — FA02/FA03).
- "Excluir" no painel não apaga: orçamento vira "Cancelado" (sinal retido, se houver) e devolução
  quitada só sai do painel (continua em Locações, histórico e relatórios) (UC05 FA04).
- Contrato e recibo em PDF, com versões (nova versão só se os dados mudaram). Pasta `documentos/`
  (ou `-Dariel.docs=caminho`).
- Relatórios (só administrador) com exportação CSV e PDF: faturamento, locações, produtos mais locados,
  ocorrências, demanda por tema, agenda, inadimplência e fluxo de caixa (UC18).

### Decisões da equipe diferentes da documentação
- Inadimplência: o UC05 bloqueia a confirmação; a equipe decidiu permitir com AUTORIZAÇÃO registrada
  (quem autorizou fica gravado na locação).
- Multas: o UC11 prevê multa sugerida pelo sistema; a equipe decidiu que multas e indenizações são sempre
  DIGITADAS (o sistema não calcula nem sugere; 0 = sem cobrança).

## Ainda não implementado
Banco de dados (dados ficam em memória), backup (RNF009), estorno/reembolso, fotos.
