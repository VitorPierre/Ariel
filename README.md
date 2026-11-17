<p align="center">
  <img src="ariel/design/ariel-logo-rosa.png" alt="Logo Ariel · Conto de Fadas" width="220">
</p>

# Ariel · Conto de Fadas

Sistema desktop para gestão de **locação de decoração para festas e eventos**: clientes, catálogo, orçamentos, reservas, pagamentos, retirada, devolução, contratos em PDF e relatórios.

Projeto acadêmico de **Engenharia de Software II (UniRios)**, desenvolvido em **Java Swing** com arquitetura **MVC** e baseado na Documentação Ariel v0.5.

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Interface](https://img.shields.io/badge/interface-Swing-pink)
![Arquitetura](https://img.shields.io/badge/arquitetura-MVC-blue)
![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)

## Funcionalidades

- **Painel** com as reservas organizadas em colunas: Orçamento, Locado e Devolvido
- **Clientes** e **Catálogo** com cadastro, edição, desativação e reativação
- **Orçamentos** com validade de 30 dias e reserva de estoque somente após o sinal
- **Locações**: confirmação, alteração de reserva confirmada, pagamento do saldo e retirada pelo titular ou por terceiro autorizado
- **Devoluções** totais ou parciais, com registro de ocorrências
- **Contrato e recibo em PDF**, com controle de versões (gerador de PDF próprio, sem bibliotecas externas)
- **Relatórios** (somente administrador) com exportação em CSV e PDF: faturamento, locações, produtos mais locados, ocorrências, demanda por tema, agenda, inadimplência e fluxo de caixa
- **Perfis de acesso**: Administrador e Funcionário
- Senhas protegidas com PBKDF2 e validação de CPF
- Tela cheia com **F11** (e **Esc** para sair)

## Tecnologias

- Java 17 ou superior (testado no Java 21)
- Java Swing (somente a biblioteca padrão, sem dependências externas)
- Fontes Inter e Outfit, ambas sob licença SIL OFL

## Como executar

Pré-requisito: [JDK 17+](https://adoptium.net/) instalado.

```bash
git clone https://github.com/VitorPierre/Ariel.git
cd Ariel/ariel
```

**Windows:**

```bat
compilar_e_executar.bat
```

**Linux / macOS:**

```bash
sh compilar_e_executar.sh
```

Os scripts compilam o código para a pasta `out/` e executam `app.Main` com `src` no classpath, para que a logo e as fontes de `src/recursos` sejam encontradas.

**Pela IDE (IntelliJ, Eclipse, NetBeans):** marque `src` como pasta de fontes e execute `app.Main`. Os recursos são copiados automaticamente.

## Acesso de teste

| Perfil | Usuário | Senha |
|---|---|---|
| Administrador | `admin` | `admin123` |
| Funcionário | `funcionario` | `func123` |

> Credenciais apenas para demonstração. Troque-as antes de qualquer uso real.

## Estrutura do projeto

```text
Ariel/
└── ariel/
    ├── src/
    │   ├── app/          # Main: Login → Sistema → Sair → Login
    │   ├── model/        # Dados e regras (sem Swing): Cliente, Catalogo, Locacao, Pagamento...
    │   ├── view/         # Telas Swing e camada visual (Tema, BotaoUI, BarraRolagem...)
    │   ├── controller/   # Regras de negócio: decidem o que pode ou não ser feito
    │   ├── util/         # Senha (PBKDF2), Pdf, Cpf
    │   └── recursos/     # Logo e fontes lidas pelo Tema
    ├── design/           # Protótipos em PDF e logo oficial
    ├── DESIGN.md         # Documento oficial de design
    ├── compilar_e_executar.bat
    └── compilar_e_executar.sh
```

### Como o MVC funciona aqui

A tela pede algo ao controller (por exemplo, "registrar retirada"). O controller confere as regras e altera o model. Se alguma regra for violada, lança `RegraNegocioException` e a tela mostra a mensagem. Quando algo muda, o controller avisa e as telas se redesenham.

## Principais regras de negócio

- O orçamento vale 30 dias e **não reserva estoque**; apenas o sinal registrado reserva.
- Sinal de 10%, 20% ou 50% do total, com mínimo de 10%.
- Somente o **Administrador** registra pagamentos e gera contrato e recibo. O Funcionário monta e salva o orçamento, e a confirmação fica pendente até o sinal ser registrado.
- Reserva confirmada pode ser alterada: o estoque é revalidado e, sem conflito, uma nova versão do contrato é gerada.
- O saldo deve ser quitado por inteiro antes da retirada.
- Na devolução parcial, itens que ainda não voltaram continuam pendentes; a locação só vira "Devolvido" quando tudo for conferido.
- Excluir cliente ou item de catálogo **sem vínculos** apaga o registro. **Com vínculos**, apenas desativa, preservando o histórico.

### Decisões da equipe que diferem da documentação

- **Inadimplência:** a confirmação é permitida mediante autorização registrada (fica gravado quem autorizou).
- **Multas:** sempre digitadas pelo usuário; o sistema não calcula nem sugere valores.

## Design

O visual (rosa e branco) segue o [DESIGN.md](ariel/DESIGN.md). Toda cor, fonte e forma vem de `view/Tema.java`; nenhuma tela declara cor própria. Os protótipos estão em [`ariel/design/`](ariel/design/).

## Limitações atuais

- Os dados ficam **em memória** (não há banco de dados), então são perdidos ao fechar o programa
- Backup, estorno/reembolso e fotos ainda não foram implementados

## Autor

**Vitor Pierre** · [@VitorPierre](https://github.com/VitorPierre)
