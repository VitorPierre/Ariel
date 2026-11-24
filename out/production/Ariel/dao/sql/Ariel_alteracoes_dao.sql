-- ============================================================================
-- Ariel_alteracoes_dao.sql
-- Rode isto DEPOIS do Ariel_corrigido_fks.sql (mesmo banco `ariel`).
--
-- Ao escrever o DAO, cruzei cada tabela com a classe Java correspondente e vi que
-- várias colunas que o model já usa hoje simplesmente não existem no schema corrigido
-- (ele corrige as FKs, mas não foi desenhado com todos os campos que o Java já tem).
-- Sem essas colunas, salvar um cliente ou uma locação no MySQL perderia dado
-- silenciosamente (endereço, por exemplo, que é OBRIGATÓRIO pra locar). Em vez de
-- fazer isso, acrescentei as colunas que faltam — nada é removido ou renomeado,
-- então quem já rodou o Ariel_corrigido_fks.sql pode rodar isto em cima sem medo.
--
-- Resumo do que foi acrescentado e por quê:
--   Cliente    -> rg, instagram, whatsapp, endereco, observacoes, ativo
--                 (o Cliente.java tem esses 6 campos; a tabela só tinha Nome/CPF/Inadiplente)
--   Categoria  -> descricao, ativo      (Catalogo.Categoria tem os dois; faltavam)
--   Tema       -> descricao, ativo      (Catalogo.Tema tem os dois; faltavam)
--   Serviço    -> descricao, ativo      (Catalogo.Servico tem os dois; faltavam.
--                 OBS: a coluna `tipoServiço` já existente passou a guardar o "nome" do
--                 serviço — não criei uma coluna nova redundante. `distanciaServiço`
--                 continua existindo mas o Java não usa; fica null por enquanto)
--   Produto    -> ativo, indisponivel   (Catalogo.Item tem os dois pra produto; faltavam.
--                 a coluna `quantidade` já existente passou a guardar o Item.estoque)
--   Locação    -> etapa, validade, retirada, devolucao, devolvidoEm, arquivada,
--                 ocultaNoPainel, retiradoPor, devolvidoPor, emissao, modalidade,
--                 motivoArquivo, autorizadoInadimplentePor, contratoPendente
--                 (a Locacao.java tem todos esses campos; a tabela só tinha valorFinal/
--                 observações/dataEvento/pessoaAutorizada — ainda ficaram de fora, por
--                 não terem tabela equivalente: itens da locação, documentos, ocorrências,
--                 autorizados a retirar e o controle de devolução parcial — ver DAO)
--   Pacote_has_Produto -> quantidade
--                 (Catalogo.Item.componentes guarda um Map<Item, Integer>: cada produto do
--                 pacote tem uma QUANTIDADE, ex. "2x cadeira". A tabela de junção original
--                 só dizia QUE o produto está no pacote, sem quantidade — sem essa coluna
--                 não dava pra reconstruir o pacote de volta corretamente)
-- ============================================================================

USE `ariel`;

ALTER TABLE `Cliente`
  ADD COLUMN `rg` VARCHAR(45) NOT NULL DEFAULT '',
  ADD COLUMN `instagram` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `whatsapp` VARCHAR(45) NOT NULL DEFAULT '',
  ADD COLUMN `endereco` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `observacoes` VARCHAR(500) NOT NULL DEFAULT '',
  ADD COLUMN `ativo` TINYINT NOT NULL DEFAULT 1;

ALTER TABLE `Categoria`
  ADD COLUMN `descricao` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `ativo` TINYINT NOT NULL DEFAULT 1;

ALTER TABLE `Tema`
  ADD COLUMN `descricao` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `ativo` TINYINT NOT NULL DEFAULT 1;

ALTER TABLE `Serviço`
  ADD COLUMN `descricao` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `ativo` TINYINT NOT NULL DEFAULT 1;

ALTER TABLE `Produto`
  ADD COLUMN `ativo` TINYINT NOT NULL DEFAULT 1,
  ADD COLUMN `indisponivel` INT NOT NULL DEFAULT 0;

ALTER TABLE `Locação`
  ADD COLUMN `etapa` VARCHAR(20) NOT NULL DEFAULT 'ORCAMENTO',
  ADD COLUMN `validade` DATE NULL,
  ADD COLUMN `retirada` DATE NULL,
  ADD COLUMN `devolucao` DATE NULL,
  ADD COLUMN `devolvidoEm` DATE NULL,
  ADD COLUMN `arquivada` TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN `ocultaNoPainel` TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN `retiradoPor` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `devolvidoPor` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `emissao` DATE NULL,
  ADD COLUMN `modalidade` VARCHAR(30) NOT NULL DEFAULT 'Retira',
  ADD COLUMN `motivoArquivo` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `autorizadoInadimplentePor` VARCHAR(255) NOT NULL DEFAULT '',
  ADD COLUMN `contratoPendente` TINYINT NOT NULL DEFAULT 0;

ALTER TABLE `Pacote_has_Produto`
  ADD COLUMN `quantidade` INT NOT NULL DEFAULT 1;

-- Pagamento (model/Pagamento.java é um record: data, valor, forma [Pix/Dinheiro/Cartão], tipo).
-- A tabela só tinha `tipo` e `valorPagamento`: faltavam a data e a forma de pagamento.
ALTER TABLE `Pagamento`
  ADD COLUMN `dataPagamento` DATE NULL,
  ADD COLUMN `forma` VARCHAR(30) NOT NULL DEFAULT 'PIX';
