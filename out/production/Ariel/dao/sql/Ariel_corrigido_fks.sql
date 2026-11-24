-- ============================================================================
-- Ariel_corrigido_fks.sql
-- Versão MÍNIMA de correção do Ariel.sql original: mantém o desenho de
-- chaves primárias compostas por Usuario (igual ao arquivo que o time já tem),
-- só corrige as 7 foreign keys que não batiam com a PK real da tabela referenciada.
--
-- Erros corrigidos:
--   1) Orçamento_has_Serviço -> Serviço: faltava a coluna Serviço_Usuario_idUsuario.
--   2) Ocorrencia -> Devolução: referenciava só idDevolução; Devolução tem PK de 4 colunas.
--   3) Pacote_has_Produto -> Produto: faltavam Categoria_idCategoria / Categoria_Usuario_idUsuario.
--   4) Produto_has_Orçamento -> Produto: mesmo problema do item 3.
--   5) Devolução_has_Produto -> Devolução: mesmo problema do item 2.
--   6) Devolução_has_Produto -> Produto: mesmo problema do item 3.
--   7) Produto_has_Ocorrencia -> Produto: mesmo problema do item 3.
--      (e, em cadeia, Produto_has_Ocorrencia -> Ocorrencia precisou crescer de 3
--      para 6 colunas, porque a PK de Ocorrencia também cresceu no item 2.)
--
-- AVISO: não há um servidor MySQL disponível neste ambiente para testar a
-- execução real deste script. A verificação foi feita manualmente, coluna por
-- coluna, comparando cada FOREIGN KEY com a PRIMARY KEY da tabela referenciada.
-- Recomendo rodar este script num banco de teste antes de usar em produção.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `ariel` DEFAULT CHARACTER SET utf8mb4;
USE `ariel`;

CREATE TABLE IF NOT EXISTS `Usuario` (
  `idUsuario` INT NOT NULL AUTO_INCREMENT,
  `Nome` VARCHAR(255) NOT NULL,
  `Login` VARCHAR(255) NOT NULL,
  `senha` VARCHAR(255) NOT NULL,
  `perfilAcesso` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Cliente` (
  `idCliente` INT NOT NULL AUTO_INCREMENT,
  `Nome` VARCHAR(255) NOT NULL,
  `CPF` VARCHAR(45) NOT NULL,
  `Inadiplente` TINYINT,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idCliente`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Cliente_Usuario`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Serviço` (
  `idServiço` INT NOT NULL AUTO_INCREMENT,
  `tipoServiço` VARCHAR(45) NOT NULL,
  `valor` DOUBLE NOT NULL,
  `distanciaServiço` VARCHAR(45) NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idServiço`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Serviço_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Orçamento` (
    `idOrçamento` INT NOT NULL AUTO_INCREMENT,
    `entrada` DOUBLE NOT NULL,
    `dataEmissão` DATETIME NOT NULL,
    `dataValidadeOrçamento` DATETIME NOT NULL,
    `valorFinalOrçamento` DOUBLE NOT NULL,
    `desconto` DOUBLE NOT NULL,
    `Cliente_idCliente` INT NOT NULL,
    `Cliente_Usuario_idUsuario` INT NOT NULL,
    -- Usuário que cadastrou o orçamento
    `Usuario_idUsuario` INT NOT NULL,
    PRIMARY KEY (
        `idOrçamento`,
        `Cliente_idCliente`,
        `Cliente_Usuario_idUsuario`
    ),
    CONSTRAINT `fk_Orçamento_Cliente1`
        FOREIGN KEY (
            `Cliente_idCliente`,
            `Cliente_Usuario_idUsuario`
        )
        REFERENCES `Cliente` (
            `idCliente`,
            `Usuario_idUsuario`
        ),
    CONSTRAINT `fk_Orçamento_Usuario1`
	FOREIGN KEY (`Usuario_idUsuario`)
	REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Tema` (
	`idTema` INT NOT NULL AUTO_INCREMENT,
	`nome` VARCHAR(255),
	`Usuario_idUsuario` INT NOT NULL,
	PRIMARY KEY (`idTema`, `Usuario_idUsuario`),
	CONSTRAINT `fk_Tema_Usuario1`
	FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Categoria` (
  `idCategoria` INT NOT NULL AUTO_INCREMENT,
  `Nome` VARCHAR(255) NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idCategoria`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Categoria_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Produto` (
  `idProduto` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(255),
  `descrição` VARCHAR(255),
  `cor` VARCHAR(255),
  `valor` DOUBLE,
  `quantidade` INT NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  `Categoria_idCategoria` INT NOT NULL,
  `Categoria_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idProduto`, `Usuario_idUsuario`, `Categoria_idCategoria`, `Categoria_Usuario_idUsuario`),
  CONSTRAINT `fk_Produto_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`),
  CONSTRAINT `fk_Produto_Categoria1`
    FOREIGN KEY (`Categoria_idCategoria`, `Categoria_Usuario_idUsuario`)
    REFERENCES `Categoria` (`idCategoria`, `Usuario_idUsuario`)
);

-- CORRIGIDO (1): acrescentada `Serviço_Usuario_idUsuario` para fechar a FK com a PK composta de `Serviço`.
CREATE TABLE IF NOT EXISTS `Orçamento_has_Serviço` (
  `Orçamento_idOrçamento` INT NOT NULL,
  `Orçamento_Cliente_idCliente` INT NOT NULL,
  `Orçamento_Cliente_Usuario_idUsuario` INT NOT NULL,
  `Serviço_idServiço` INT NOT NULL,
  `Serviço_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`, `Serviço_idServiço`, `Serviço_Usuario_idUsuario`),
  CONSTRAINT `fk_Orçamento_has_Serviço_Orçamento1`
    FOREIGN KEY (`Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`)
    REFERENCES `Orçamento` (`idOrçamento`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Orçamento_has_Serviço_Serviço1`
    FOREIGN KEY (`Serviço_idServiço`, `Serviço_Usuario_idUsuario`)
    REFERENCES `Serviço` (`idServiço`, `Usuario_idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Locação` (
    `idLocação` INT NOT NULL AUTO_INCREMENT,
    `valorFinal` DOUBLE NOT NULL,
    `observações` VARCHAR(255) NOT NULL,
    `dataEvento` DATETIME NOT NULL,
    `pessoaAutorizada` VARCHAR(45) NOT NULL,
    `Cliente_idCliente` INT NOT NULL,
    `Cliente_Usuario_idUsuario` INT NOT NULL,
    -- Orçamento aprovado que originou a locação
    `Orçamento_idOrçamento` INT NOT NULL,
    -- Usuário que registrou/fechou a locação
    `Usuario_idUsuario` INT NOT NULL,
    PRIMARY KEY (
        `idLocação`,
        `Cliente_idCliente`,
        `Cliente_Usuario_idUsuario`
    ),
    CONSTRAINT `fk_Locação_Cliente1`
        FOREIGN KEY (
            `Cliente_idCliente`,
            `Cliente_Usuario_idUsuario`
        )
        REFERENCES `Cliente` (
            `idCliente`,
            `Usuario_idUsuario`
        ),
    CONSTRAINT `fk_Locação_Orçamento1`
        FOREIGN KEY (
            `Orçamento_idOrçamento`,
            `Cliente_idCliente`,
            `Cliente_Usuario_idUsuario`
        )
        REFERENCES `Orçamento` (
            `idOrçamento`,
            `Cliente_idCliente`,
            `Cliente_Usuario_idUsuario`
        ),
    CONSTRAINT `fk_Locação_Usuario1`
        FOREIGN KEY (`Usuario_idUsuario`)
        REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Documento` (
  `idDocumento` INT NOT NULL AUTO_INCREMENT,
  `tipo` VARCHAR(255),
  `arquivo` BLOB,
  `Locação_idLocação` INT NOT NULL,
  `Locação_Cliente_idCliente` INT NOT NULL,
  `Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idDocumento`, `Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Documento_Locação1`
    FOREIGN KEY (`Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`)
    REFERENCES `Locação` (`idLocação`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Devolução` (
  `idDevolução` INT NOT NULL AUTO_INCREMENT,
  `dataDevolução` DATETIME NOT NULL,
  `Locação_idLocação` INT NOT NULL,
  `Locação_Cliente_idCliente` INT NOT NULL,
  `Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idDevolução`, `Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Devolução_Locação1`
    FOREIGN KEY (`Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`)
    REFERENCES `Locação` (`idLocação`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
);

-- CORRIGIDO (2): acrescentadas as 3 colunas que faltavam para fechar a FK com a PK de 4 colunas de `Devolução`.
CREATE TABLE IF NOT EXISTS `Ocorrencia` (
  `idOcorrencia` INT NOT NULL AUTO_INCREMENT,
  `tipo` VARCHAR(255) NOT NULL,
  `valor` DOUBLE NOT NULL,
  `Devolução_idDevolução` INT NOT NULL,
  `Devolução_Locação_idLocação` INT NOT NULL,
  `Devolução_Locação_Cliente_idCliente` INT NOT NULL,
  `Devolução_Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idOcorrencia`, `Devolução_idDevolução`, `Devolução_Locação_idLocação`, `Devolução_Locação_Cliente_idCliente`, `Devolução_Locação_Cliente_Usuario_idUsuario`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Ocorrencia_Devolução1`
    FOREIGN KEY (`Devolução_idDevolução`, `Devolução_Locação_idLocação`, `Devolução_Locação_Cliente_idCliente`, `Devolução_Locação_Cliente_Usuario_idUsuario`)
    REFERENCES `Devolução` (`idDevolução`, `Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Ocorrencia_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Pagamento` (
  `idPagamento` INT NOT NULL AUTO_INCREMENT,
  `tipo` VARCHAR(255) NOT NULL,
  `valorPagamento` DOUBLE NOT NULL,
  `Locação_idLocação` INT NOT NULL,
  `Locação_Cliente_idCliente` INT NOT NULL,
  `Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idPagamento`, `Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Pagamento_Locação1`
    FOREIGN KEY (`Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`)
    REFERENCES `Locação` (`idLocação`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Pacote` (
  `idPacote` INT NOT NULL AUTO_INCREMENT,
  `nome` VARCHAR(255) NOT NULL,
  `descrição` VARCHAR(255),
  `preço` DOUBLE NOT NULL,
  `disponivel` TINYINT NOT NULL,
  PRIMARY KEY (`idPacote`)
);

-- CORRIGIDO (3): acrescentadas `Produto_Categoria_idCategoria` / `Produto_Categoria_Usuario_idUsuario`
-- para fechar a FK com a PK de 4 colunas de `Produto`.
CREATE TABLE IF NOT EXISTS `Pacote_has_Produto` (
  `Pacote_idPacote` INT NOT NULL,
  `Produto_idProduto` INT NOT NULL,
  `Produto_Usuario_idUsuario` INT NOT NULL,
  `Produto_Categoria_idCategoria` INT NOT NULL,
  `Produto_Categoria_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`Pacote_idPacote`, `Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`),
  CONSTRAINT `fk_Pacote_has_Produto_Pacote1`
    FOREIGN KEY (`Pacote_idPacote`)
    REFERENCES `Pacote` (`idPacote`),
  CONSTRAINT `fk_Pacote_has_Produto_Produto1`
    FOREIGN KEY (`Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`)
    REFERENCES `Produto` (`idProduto`, `Usuario_idUsuario`, `Categoria_idCategoria`, `Categoria_Usuario_idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Pacote_has_Orçamento` (
  `Pacote_idPacote` INT NOT NULL,
  `Orçamento_idOrçamento` INT NOT NULL,
  `Orçamento_Cliente_idCliente` INT NOT NULL,
  `Orçamento_Cliente_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`Pacote_idPacote`, `Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Pacote_has_Orçamento_Pacote1`
    FOREIGN KEY (`Pacote_idPacote`)
    REFERENCES `Pacote` (`idPacote`),
  CONSTRAINT `fk_Pacote_has_Orçamento_Orçamento1`
    FOREIGN KEY (`Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`)
    REFERENCES `Orçamento` (`idOrçamento`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
);

-- CORRIGIDO (4): mesma correção do item (3), agora em `Produto_has_Orçamento`.
CREATE TABLE IF NOT EXISTS `Produto_has_Orçamento` (
  `Produto_idProduto` INT NOT NULL,
  `Produto_Usuario_idUsuario` INT NOT NULL,
  `Produto_Categoria_idCategoria` INT NOT NULL,
  `Produto_Categoria_Usuario_idUsuario` INT NOT NULL,
  `Orçamento_idOrçamento` INT NOT NULL,
  `Orçamento_Cliente_idCliente` INT NOT NULL,
  `Orçamento_Cliente_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`, `Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Produto_has_Orçamento_Produto1`
    FOREIGN KEY (`Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`)
    REFERENCES `Produto` (`idProduto`, `Usuario_idUsuario`, `Categoria_idCategoria`, `Categoria_Usuario_idUsuario`),
  CONSTRAINT `fk_Produto_has_Orçamento_Orçamento1`
    FOREIGN KEY (`Orçamento_idOrçamento`, `Orçamento_Cliente_idCliente`, `Orçamento_Cliente_Usuario_idUsuario`)
    REFERENCES `Orçamento` (`idOrçamento`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
);

-- CORRIGIDO (5) e (6): acrescentadas as colunas que faltavam para `Devolução` (3 colunas) e `Produto` (2 colunas).
CREATE TABLE IF NOT EXISTS `Devolução_has_Produto` (
  `Devolução_idDevolução` INT NOT NULL,
  `Devolução_Locação_idLocação` INT NOT NULL,
  `Devolução_Locação_Cliente_idCliente` INT NOT NULL,
  `Devolução_Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  `Produto_idProduto` INT NOT NULL,
  `Produto_Usuario_idUsuario` INT NOT NULL,
  `Produto_Categoria_idCategoria` INT NOT NULL,
  `Produto_Categoria_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`Devolução_idDevolução`, `Devolução_Locação_idLocação`, `Devolução_Locação_Cliente_idCliente`, `Devolução_Locação_Cliente_Usuario_idUsuario`, `Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`),
  CONSTRAINT `fk_Devolução_has_Produto_Devolução1`
    FOREIGN KEY (`Devolução_idDevolução`, `Devolução_Locação_idLocação`, `Devolução_Locação_Cliente_idCliente`, `Devolução_Locação_Cliente_Usuario_idUsuario`)
    REFERENCES `Devolução` (`idDevolução`, `Locação_idLocação`, `Locação_Cliente_idCliente`, `Locação_Cliente_Usuario_idUsuario`),
  CONSTRAINT `fk_Devolução_has_Produto_Produto1`
    FOREIGN KEY (`Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`)
    REFERENCES `Produto` (`idProduto`, `Usuario_idUsuario`, `Categoria_idCategoria`, `Categoria_Usuario_idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Endereço` (
  `idEndereço` INT NOT NULL AUTO_INCREMENT,
  `Bairro` VARCHAR(255) NOT NULL,
  `Logradouro` VARCHAR(255) NOT NULL,
  `Complemento` VARCHAR(255),
  `Numero` INT,
  `Cliente_idCliente` INT NOT NULL,
  `Cliente_Usuario_idUsuario` INT NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idEndereço`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Endereço_Cliente1`
    FOREIGN KEY (`Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
    REFERENCES `Cliente` (`idCliente`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Endereço_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

CREATE TABLE IF NOT EXISTS `Contato` (
  `idContato` INT NOT NULL AUTO_INCREMENT,
  `tipoContato` VARCHAR(45) NOT NULL,
  `valorContato` VARCHAR(45) NOT NULL,
  `Cliente_idCliente` INT NOT NULL,
  `Cliente_Usuario_idUsuario` INT NOT NULL,
  `Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (`idContato`, `Cliente_idCliente`, `Cliente_Usuario_idUsuario`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Contato_Cliente1`
    FOREIGN KEY (`Cliente_idCliente`, `Cliente_Usuario_idUsuario`)
    REFERENCES `Cliente` (`idCliente`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Contato_Usuario1`
    FOREIGN KEY (`Usuario_idUsuario`)
    REFERENCES `Usuario` (`idUsuario`)
);

-- CORRIGIDO (7): acrescentadas colunas para fechar a FK com `Produto` (2 colunas a mais) E com
-- `Ocorrencia` (que também cresceu de 3 para 6 colunas na correção (2) acima).
CREATE TABLE IF NOT EXISTS `Produto_has_Ocorrencia` (
  `Produto_idProduto` INT NOT NULL,
  `Produto_Usuario_idUsuario` INT NOT NULL,
  `Produto_Categoria_idCategoria` INT NOT NULL,
  `Produto_Categoria_Usuario_idUsuario` INT NOT NULL,
  `Ocorrencia_idOcorrencia` INT NOT NULL,
  `Ocorrencia_Devolução_idDevolução` INT NOT NULL,
  `Ocorrencia_Devolução_Locação_idLocação` INT NOT NULL,
  `Ocorrencia_Devolução_Locação_Cliente_idCliente` INT NOT NULL,
  `Ocorrencia_Devolução_Locação_Cliente_Usuario_idUsuario` INT NOT NULL,
  `Ocorrencia_Usuario_idUsuario` INT NOT NULL,
  PRIMARY KEY (
    `Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`,
    `Ocorrencia_idOcorrencia`, `Ocorrencia_Devolução_idDevolução`, `Ocorrencia_Devolução_Locação_idLocação`,
    `Ocorrencia_Devolução_Locação_Cliente_idCliente`, `Ocorrencia_Devolução_Locação_Cliente_Usuario_idUsuario`, `Ocorrencia_Usuario_idUsuario`
  ),
  CONSTRAINT `fk_Produto_has_Ocorrencia_Produto1`
    FOREIGN KEY (`Produto_idProduto`, `Produto_Usuario_idUsuario`, `Produto_Categoria_idCategoria`, `Produto_Categoria_Usuario_idUsuario`)
    REFERENCES `Produto` (`idProduto`, `Usuario_idUsuario`, `Categoria_idCategoria`, `Categoria_Usuario_idUsuario`),
  CONSTRAINT `fk_Produto_has_Ocorrencia_Ocorrencia1`
    FOREIGN KEY (
      `Ocorrencia_idOcorrencia`, `Ocorrencia_Devolução_idDevolução`, `Ocorrencia_Devolução_Locação_idLocação`,
      `Ocorrencia_Devolução_Locação_Cliente_idCliente`, `Ocorrencia_Devolução_Locação_Cliente_Usuario_idUsuario`, `Ocorrencia_Usuario_idUsuario`
    )
    REFERENCES `Ocorrencia` (
      `idOcorrencia`, `Devolução_idDevolução`, `Devolução_Locação_idLocação`,
      `Devolução_Locação_Cliente_idCliente`, `Devolução_Locação_Cliente_Usuario_idUsuario`, `Usuario_idUsuario`
    )
);

CREATE TABLE IF NOT EXISTS `Tema_has_Pacote` (
  `Tema_idTema` INT NOT NULL,
  `Tema_Usuario_idUsuario` INT NOT NULL,
  `Pacote_idPacote` INT NOT NULL,
  PRIMARY KEY (`Tema_idTema`, `Tema_Usuario_idUsuario`, `Pacote_idPacote`),
  CONSTRAINT `fk_Tema_has_Pacote_Tema1`
    FOREIGN KEY (`Tema_idTema`, `Tema_Usuario_idUsuario`)
    REFERENCES `Tema` (`idTema`, `Usuario_idUsuario`),
  CONSTRAINT `fk_Tema_has_Pacote_Pacote1`
    FOREIGN KEY (`Pacote_idPacote`)
    REFERENCES `Pacote` (`idPacote`)
);
