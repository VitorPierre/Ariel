# Ariel · Conto de Fadas (Java Swing, MVC)

Sistema desktop de locação de decoração (Engenharia de Software II · UniRios). Segue a Documentação v0.5.

```
src/
  app/         Main                         ponto de entrada: Login -> Sistema -> Sair -> Login
  model/       Cliente, Catalogo, Locacao, Pagamento, Ocorrencia, Devolucao, Documento,
               ServicoContratado, Usuario, Perfil, Etapa,
               Regras (valores das regras), Mensagens (MSG001–MSG020), RegraNegocioException,
               ExigeAutorizacaoException    DADOS (sem Swing): o que existe no sistema
  view/        TelaLogin, TelaPrincipal (Painel), TelaClientes, TelaCatalogo, TelaOrcamentos,
               TelaLocacoes, TelaDevolucoes, TelaRelatorios, TelaConfiguracoes,
               DetalhesReserva, Ui, CampoSenha
                                            TELAS: só desenham e reagem a cliques
  controller/  LoginController, UsuarioController, PainelController, CatalogoController,
               RelatorioController, DocumentoController
                                            REGRAS DE NEGÓCIO: decidem o que pode ou não
  util/        Senha (PBKDF2), Pdf (gerador de PDF sem bibliotecas)
```

Como o MVC funciona aqui: a tela pede algo ao controller ("registrar retirada"); o controller
confere as regras e altera o model; se violar uma regra lança `RegraNegocioException` e a tela
mostra a mensagem; quando algo muda, o controller avisa e as telas se redesenham.

Executar: `compilar_e_executar.bat` (Windows) ou `compilar_e_executar.sh`. Requer JDK 17+ (testado no 21).

Tela cheia: tecla **F11**, botão na barra de título ou duplo clique na barra. **Esc** sai da tela cheia (e, na janela normal, fecha o sistema).

Login de teste (troque antes de usar de verdade):
- administrador: `admin` / `admin123`
- funcionário: `funcionario` / `func123`

## Regras principais implementadas (doc. v0.5)
- Orçamento vale 30 dias (fixo) e NÃO reserva estoque; só o sinal registrado reserva.
- Sinal variável por orçamento (atalhos 10/20/50% ou valor livre, sem mínimo fixo).
- Saldo quitado por inteiro ANTES da retirada; retirada só pelo titular ou terceiro autorizado.
- Devolução por qualquer pessoa identificada (nome); não exige pagamento.
- Multas e indenizações nunca são calculadas nem sugeridas: são digitadas (0 = sem cobrança).
- Inadimplência é manual: alerta + autorização do responsável, que fica registrada.
- Estoque por quantidade (total + indisponível). Nada é excluído, só inativado.
- Contrato e recibo em PDF, com versões (nova versão só se os dados mudaram). Pasta `documentos/`
  (ou `-Dariel.docs=caminho`).
- Relatórios (só administrador) com exportação CSV e PDF.

## Ainda não implementado
Banco de dados (dados ficam em memória), backup, estorno/reembolso, alteração de itens/datas de reserva
já confirmada, devolução parcial, inativação de clientes/usuários, fotos.
