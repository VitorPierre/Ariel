# Projeto Ariel — Documento de Design

**Sistema:** Ariel · Conto de Fadas Festas e Eventos
**Versão do design:** 1.2 (rosa e branco) · 16/11/2026
**Status:** **OFICIAL.** Aprovado pela equipe e aplicado ao código Java em todas as
telas e diálogos (16/11/2026). É assim que o sistema se apresenta.

Este é o documento oficial de design do projeto. Ele define a identidade visual,
as cores, a tipografia, as formas, os componentes e o layout de cada tela.
Toda alteração visual no sistema deve seguir o que está aqui; quando a regra
mudar, muda primeiro neste documento e depois no código.

**Como ler:** as seções 1 a 9 são a regra (o que o design pede). A seção 10 diz
como a regra está escrita no código e onde o Swing obrigou a usar um valor
diferente do protótipo. Quando houver diferença, **vale o que está em 10.5**,
porque é o que o sistema realmente mostra.

**Arquivos de referência:**

| O quê | Onde |
|---|---|
| Protótipos navegáveis das telas | Quadro de design "Ariel · Protótipos de telas (rosa e branco)" |
| Protótipos em PDF (4 telas) | `design/Ariel_prototipos_telas.pdf` |
| Este documento em PDF | `design/Ariel_DESIGN.pdf` |
| Logo oficial em rosa (PNG, fundo transparente) | `design/ariel-logo-rosa.png` |
| Logo usada pelo sistema | `src/recursos/logo.png` (cópia da oficial) |
| Fontes usadas pelo sistema | `src/recursos/fontes/` (Inter e Outfit) |
| Camada visual no código | `src/view/Tema.java`, `BotaoUI.java`, `BarraRolagem.java`, `Logo.java`, `Ui.java` |

---

## 1. Princípios

Cinco decisões que explicam o resto do documento. Em caso de dúvida, decida por elas.

1. **Branco manda, rosa aponta.** O fundo é claro e neutro. O rosa forte aparece
   só onde o olho precisa ir: a ação principal, o item de menu ativo, o alerta.
   Uma tela com rosa em todo lugar perde a hierarquia.
2. **A cor nunca é a única pista.** Quem não distingue bem as cores deve
   conseguir usar o sistema. Situação, status e disponibilidade são sempre
   acompanhados de **texto**.
3. **Formas arredondadas e bordas finas.** Nada de borda grossa preta. A
   separação entre áreas vem de um tom de rosa muito claro, não de contraste alto.
4. **Densidade de trabalho, não de vitrine.** A proprietária usa o sistema o dia
   todo. Preferir informação legível e clicável a espaço decorativo.
5. **Nada que o Swing não entregue.** O protótipo é o alvo, mas a implementação é
   Java Swing. Ver a seção 10 antes de propor efeito novo.

---

## 2. Identidade

### 2.1 Logo

O arquivo oficial é `design/ariel-logo-rosa.png`: a letra "A" formada pela sereia,
com a palavra ARIEL abaixo. Fundo transparente.

**Regras de uso:**

- **Largura mínima:** 96 px. Abaixo disso a palavra ARIEL fica ilegível; use só
  a parte de cima (a sereia/A) recortada.
- **Proporção:** sempre proporcional (altura automática). Nunca esticar ou achatar.
- **Espaço livre em volta:** no mínimo 16 px sem nenhum outro elemento.
- **Fundos permitidos:** branco (`#FFFFFF`), fundo geral (`#FFF7FA`) e rosa claro
  (`#FFF0F5`). O corpo da sereia é vazado, então o fundo aparece por dentro do
  desenho: sobre fundo escuro ou colorido a logo perde a forma. Não usar.
- **Proibido:** recolorir, aplicar sombra, contorno, girar, ou colocar dentro de
  um quadrado colorido.

**Tamanhos em uso:**

| Onde | Largura |
|---|---|
| Menu lateral | 96 px |
| Tela de login | 240 px |
| Ícone da janela / barra de tarefas | 32 px (só a sereia) |

### 2.2 Nome

Abaixo da logo no menu lateral: **CONTO DE FADAS** — 11 px, maiúsculas,
espaçamento entre letras 1,5 px, cor `#7A5C68`.

No login: **CONTO DE FADAS · FESTAS E EVENTOS** — 14 px, espaçamento 3 px.

Não repetir a palavra "Ariel" em texto ao lado da logo: ela já está no desenho.

### 2.3 Observação sobre o arquivo

A versão rosa foi gerada a partir da arte azul original, por conversão de cores.
Serve para a tela e para o sistema. **Se o time tiver o arquivo vetorial
(.ai, .svg, .pdf) da logo, recolorir a partir dele e substituir o PNG** — o vetor
dá qualidade melhor em tamanhos grandes e na impressão do contrato.

---

## 3. Cores

Toda cor usada no sistema está aqui. **Não introduzir cor fora desta tabela.**

### 3.1 Marca

| Nome | Hex | Uso |
|---|---|---|
| Rosa principal | `#C2255C` | Botão de ação principal, item de menu ativo, selo urgente, foco de campo |
| Rosa escuro | `#A61E4D` | Texto de destaque, números grandes, texto de botão secundário, links |
| Rosa pressionado | `#7A1238` | Link em hover; botão principal pressionado |

### 3.2 Superfícies

| Nome | Hex | Uso |
|---|---|---|
| Branco | `#FFFFFF` | Cartões, seções, campos, menu lateral, tabelas |
| Fundo geral | `#FFF7FA` | Fundo da área de trabalho (atrás dos cartões) |
| Rosa claro | `#FFF0F5` | Caixa "Hoje", botões `+ Produto / + Pacote / + Serviço`, painel do login, linha selecionada, selo neutro |

### 3.3 Linhas e bordas

| Nome | Hex | Uso |
|---|---|---|
| Borda de seção | `#F6DCE5` | Contorno de cartões e seções; divisória do menu; linha do cabeçalho de tabela |
| Borda de cartão | `#F1D3DD` | Contorno dos cartões de reserva no painel |
| Borda de campo | `#E8BFCD` | Contorno de campos, selects e botões secundários |
| Borda de botão claro | `#EFC4D3` | Contorno do botão "Sair" e do "Excluir" nos cartões |
| Linha de tabela | `#FBE9EF` | Divisória entre linhas de tabela e de lista |
| Borda de selo neutro | `#D9B8C4` | Contorno do selo de situação sem urgência |

### 3.4 Texto

| Nome | Hex | Uso | Contraste sobre branco |
|---|---|---|---|
| Texto | `#3B1F2B` | Títulos e texto principal | 14,8:1 |
| Texto médio | `#5E4350` | Parágrafos de apoio | 9,0:1 |
| Texto suave | `#7A5C68` | Rótulos, legendas, texto secundário | 5,9:1 |

### 3.5 Status

| Nome | Fundo | Texto | Uso |
|---|---|---|---|
| Normal | `#FFF0F5` | `#A61E4D` | Situação comum ("Vence em 2 dias", "Aguardando entrada") |
| Neutro | `#FFFFFF` + borda `#D9B8C4` | `#3B1F2B` | Situação resolvida ("Quitado", "Em dia", "Finalizada") |
| Urgente | `#C2255C` | `#FFFFFF` | Exige ação hoje ("Devolução hoje", "Cobrança pendente", "Inadimplente", "Sem estoque") |
| Erro | `#FDECEC` | `#9B1C1C` | Aviso de bloqueio ou indisponibilidade |

### 3.6 Etapas do painel (faixa colorida no topo da coluna)

| Etapa | Hex | Leitura |
|---|---|---|
| Orçamento | `#F8BBD0` | Rosa claro — nada reservado ainda |
| Locado | `#E64980` | Rosa médio — itens com o cliente |
| Devolvido | `#8E1F4D` | Vinho — ciclo encerrado |

As três também se distinguem pela **intensidade**, não só pelo tom: funciona em
tela preto e branco e para quem não distingue cores.

### 3.7 Sombra

Uma só sombra em todo o sistema, nos cartões de reserva:
`0 2px 6px rgba(166, 30, 77, 0.06)`.

Não usar sombra em seções, botões ou campos.

---

## 4. Tipografia

| Papel | Fonte | Peso |
|---|---|---|
| Títulos e números grandes | **Outfit** | 600 / 700 |
| Texto, rótulos, tabelas, botões | **Inter** | 400 / 500 / 600 / 700 |

Alternativas quando a fonte não estiver instalada: Outfit → Poppins → a sans do
sistema; Inter → a sans do sistema.

### 4.1 Escala

| Uso | Tamanho | Peso | Cor |
|---|---|---|---|
| Título da tela | 30 px | 700 Outfit | `#3B1F2B` |
| Total em destaque | 26 px | 700 Outfit | `#A61E4D` |
| Número de estatística | 32 px | 700 Outfit | `#A61E4D` |
| Título de coluna do painel | 16 px | 600 Outfit, espaçamento 1 px | `#3B1F2B` |
| Rótulo de seção (ex.: "RESUMO") | 12 px | 700 Inter, maiúsculas, espaçamento 1 px | `#7A5C68` |
| Texto / tabela / botão | 14 px | 400–600 Inter | `#3B1F2B` |
| Texto secundário | 13 px | 400 Inter | `#7A5C68` |
| Rótulo de campo, legenda | 12 px | 400 Inter | `#7A5C68` |
| Micro-rótulo ("HOJE", "CONTO DE FADAS") | 11 px | 500 Inter, espaçamento 1–1,5 px | `#7A5C68` |

**Regra:** nunca usar texto abaixo de 11 px, e 11 px só em maiúsculas com
espaçamento, nunca em frase corrida.

---

## 5. Formas e espaçamento

### 5.1 Raio dos cantos

| Elemento | Raio |
|---|---|
| Seção, coluna do painel | 18 px |
| Caixa de estatística | 16 px |
| Cartão de reserva, caixa "Hoje" | 14 px |
| Botão, campo, select, item de menu | 12 px |
| Botão pequeno ("Excluir" no cartão) | 8 px |
| Selo / contador | 999 px (pílula) |

### 5.2 Espaçamento

| Situação | Valor |
|---|---|
| Margem interna da área de trabalho | 28 px em cima/baixo, 32 px nas laterais |
| Margem interna de seção | 20 px |
| Entre seções | 20 px |
| Entre campos de um formulário | 12 px |
| Entre caixas de estatística / colunas | 16 px |
| Entre cartões de uma coluna | 10 px |
| Entre itens de menu | 4 px |

### 5.3 Alturas

| Elemento | Altura |
|---|---|
| Botão, campo, select, item de menu | **44 px mínimo** |
| Botão principal do login e do orçamento | 48 px |
| Botão pequeno dentro do cartão | 28 px |
| Linha de lista de clientes | 52 px |
| Faixa colorida da coluna | 6 px |

**44 px é piso, não sugestão:** é o tamanho mínimo para clicar com conforto.

---

## 6. Componentes

### 6.1 Botões

| Tipo | Fundo | Texto | Borda | Quando usar |
|---|---|---|---|---|
| **Principal** | `#C2255C` | `#FFFFFF` | nenhuma | A ação que conclui a tela. **Um por tela.** |
| **Secundário** | `#FFFFFF` | `#A61E4D` | `#E8BFCD` | Ações de apoio ("Voltar à lista", "Salvar rascunho") |
| **Suave** | `#FFF0F5` | `#A61E4D` | nenhuma | Ações repetidas dentro de uma seção (`+ Produto`, `+ Pacote`, `+ Serviço`) |
| **Neutro** | `#FFFFFF` | `#5E4350` | `#E8BFCD` | Ação destrutiva leve ou de menor peso ("Remover", "Descartar") |

Todos: 12 px de raio, 44 px de altura, texto 14 px peso 600, margem lateral
interna 16–20 px.

### 6.2 Campos

- Fundo `#FFFFFF`, borda 1 px `#E8BFCD`, raio 12 px, altura 44 px (48 px no login).
- Margem interna lateral 14–16 px, texto 14 px `#3B1F2B`.
- O **rótulo fica acima** do campo: 12 px, `#7A5C68`, 6 px de distância.
- Placeholder em `#7A5C68`.
- **Todo campo tem rótulo visível.** Quando o layout não permitir (busca do
  painel), o rótulo existe para o leitor de tela e o placeholder explica o campo.

### 6.3 Selos (pílulas de situação)

Raio 999 px, texto 12 px peso 600, margem interna 4 px × 12 px.
As combinações de cor estão em 3.5. **O texto do selo diz a situação por
extenso** — nunca um selo só com cor.

### 6.4 Cartão de reserva (painel)

```
┌──────────────────────────────────────┐
│ #001 · Maria Souza        [Excluir]  │  título 14 px peso 700
│ Evento: 01/12 · Total: R$ 1.260,00   │  13 px #7A5C68
│ (Vence em 2 dias)                    │  selo
└──────────────────────────────────────┘
```
Fundo branco, borda `#F1D3DD`, raio 14 px, sombra de 3.7, margem interna 12 px × 14 px.

### 6.5 Coluna do painel

Faixa colorida de 6 px no topo (cor da etapa) + cabeçalho com o nome da etapa e o
contador em pílula (`#FFF0F5` / `#A61E4D`) + lista de cartões.
Fundo branco, borda `#F6DCE5`, raio 18 px.

### 6.6 Tabela

- Cabeçalho: 12 px, `#7A5C68`, peso 600, alinhado à esquerda, com linha
  inferior `#F6DCE5`.
- Linhas: 14 px, divisória `#FBE9EF`, margem interna 12 px.
- Valores monetários **alinhados à direita**; texto à esquerda.
- Linha com problema: fundo `#FDECEC` e o texto da coluna de situação em
  `#9B1C1C` peso 600 — **com a palavra escrita** ("Indisponível (falta 1)").
- Tabela larga demais para o espaço: rolagem horizontal própria, nunca encolher
  a coluna até quebrar o número.

### 6.7 Menu lateral

Largura 220 px. Fundo branco, divisória direita `#F6DCE5`.
Logo centralizada no topo, itens empilhados, rodapé com a caixa "Hoje", o nome do
usuário e o botão "Sair".

| Estado do item | Fundo | Texto | Peso |
|---|---|---|---|
| Normal | transparente | `#3B1F2B` | 500 |
| Ativo | `#C2255C` | `#FFFFFF` | 600 |
| Hover | `#FFF0F5` | `#3B1F2B` | 500 |

---

## 7. Estados

| Estado | Como aparece |
|---|---|
| **Hover** em botão principal | Fundo escurece para `#A61E4D` |
| **Hover** em botão claro/secundário | Fundo `#FFF0F5` |
| **Hover** em cartão ou linha | Borda muda para `#E8BFCD` |
| **Foco** (teclado) em campo | Borda 2 px `#C2255C` |
| **Foco** (teclado) em botão | Contorno 2 px `#C2255C`, afastado 2 px da borda |
| **Selecionado** (linha de lista) | Fundo `#FFF0F5`, borda `#E8BFCD` |
| **Desabilitado** | Fundo `#FFF0F5`, texto `#7A5C68`, cursor normal |
| **Erro** | Mensagem em caixa `#FDECEC` com texto `#9B1C1C`, abaixo do elemento |

**O foco de teclado nunca é removido.** Quem usa o sistema o dia todo navega por
Tab; sem o anel de foco a pessoa se perde.

---

## 8. Layout das telas

Todas as telas, menos o login, seguem a mesma moldura:

```
┌────────────┬──────────────────────────────────────────────┐
│            │  Título da tela            [ações da tela]   │
│   menu     │                                              │
│  lateral   │  conteúdo                                    │
│  220 px    │                                              │
│            │                                              │
└────────────┴──────────────────────────────────────────────┘
```

Tamanho de referência: **1280 × 800**. Mínimo suportado: 1024 × 700.

### 8.1 Login

Duas colunas. Esquerda: painel `#FFF0F5` com a logo grande (260 px), o nome
completo e uma linha sobre o sistema. Direita: formulário centralizado, máximo
380 px de largura, com usuário, senha, o botão "Entrar" e um aviso sobre
redefinição de senha.

### 8.2 Painel principal

1. Título + busca + botão `+ Novo orçamento`.
2. Quatro caixas de estatística: reservas na semana, pagamentos pendentes,
   orçamentos a vencer, devoluções previstas.
3. Linha de instrução ("Arraste o cartão para outra coluna…").
4. Três colunas: Orçamento, Locado, Devolvido.

### 8.3 Novo orçamento

Conteúdo em duas colunas:

- **Esquerda (principal):** seção "Cliente e datas" e seção "Itens da proposta"
  (botões de inclusão, tabela e aviso de estoque).
- **Direita (resumo, 280 px):** totais, escolha do sinal (10% / 20% / 50% ou
  outro valor), o botão de confirmar e o de descartar.

O aviso vermelho de estoque fica **abaixo da tabela**, não em diálogo.

### 8.4 Clientes

Duas colunas: lista com busca e filtro à esquerda, ficha do cliente à direita
(dados, pessoas autorizadas e os botões de situação, histórico e salvar).

### 8.5 Telas construídas sem protótipo

Catálogo, Locações, Devoluções, Relatórios e Configurações não tiveram protótipo
desenhado. Elas foram construídas direto no código, reaproveitando a moldura da
seção 8, a tabela de 6.6, os selos de 6.3 e os botões de 6.1. O layout aplicado,
que passa a ser a referência dessas telas:

| Tela | Layout aplicado |
|---|---|
| **Catálogo** | Título + busca + botão principal (`+ Novo produto` / `pacote` / `serviço`, conforme a aba). Quatro abas em botões de escolha: Produtos, Pacotes, Temas e categorias, Serviços. Em cada aba: seção com a tabela à esquerda e seção "Detalhe" com o formulário (rótulos acima dos campos) e o botão "Salvar" à direita. Temas e categorias ficam lado a lado, cada um com tabela e formulário. |
| **Locações** | Título + busca + filtro. Seção com a tabela das locações à esquerda; seção "Detalhe da locação" (340 px) à direita, com os dados e uma grade de botões de ação. "Excluir" é botão neutro. |
| **Devoluções** | Título + seletor da locação. Seção "Conferência dos itens" (tabela + botões suaves "Registrar dano", "Registrar perda", "Ainda não voltou" e o neutro "Item OK") e seção "Ocorrências e pendências" com a prévia da cobrança e o botão principal "Registrar devolução". |
| **Relatórios** | Linha de filtros (tipo, filtro, período) com rótulo acima de cada um e os botões na mesma linha; abaixo, uma seção com a tabela do relatório. |
| **Configurações** | Seções empilhadas: "Meus dados" (campos em duas colunas, botão principal "Salvar meus dados") e, só para o administrador, "Usuários" (formulário, botão secundário "+ Criar usuário" e tabela de usuários). |

Os diálogos (confirmações, detalhes da reserva, histórico do cliente, registrar
entrada/retirada) usam fundo branco, texto `#3B1F2B` e botões no tipo Secundário,
com o anel de foco da seção 7.

**Não inventar componente novo sem registrar neste documento.** Quando o time
desenhar protótipos dessas cinco telas, o protótipo aprovado substitui esta tabela.

---

## 9. Acessibilidade

Regras obrigatórias, conferidas nos protótipos e mantidas no código:

1. **Contraste mínimo 4,5:1** para todo texto. Os pares em uso:

   | Combinação | Contraste |
   |---|---|
   | `#3B1F2B` sobre branco | 14,8:1 |
   | `#7A5C68` sobre branco | 5,9:1 |
   | `#A61E4D` sobre branco | 7,2:1 |
   | Branco sobre `#C2255C` | 5,7:1 |
   | `#9B1C1C` sobre `#FDECEC` | 7,1:1 |

   **Atenção:** o tom `#D6336C` (uma das alternativas de rosa) dá 4,6:1 com texto
   branco — passa no limite. Não usar rosa mais claro que esse atrás de texto branco.

2. **Alvo de clique de 44 px**, conforme 5.3.
3. **Nunca só cor:** todo status tem texto; as etapas também variam em intensidade.
4. **Foco de teclado sempre visível** (seção 7).
5. **Todo campo tem rótulo**, visível ou para leitor de tela.
6. Botão só com ícone precisa de descrição para leitor de tela.

---

## 10. Implementação em Java Swing

O sistema é Java 21 + Swing, padrão MVC. O design mora **inteiro na camada
`view`**: nenhuma classe de `model` ou `controller` conhece cor, fonte ou tela.
Dentro da `view`, cores, fontes e formas ficam **centralizadas em
`view/Tema.java`**, e nenhuma tela declara cor própria.

### 10.1 Arquivos da camada visual

| Arquivo | Responsabilidade |
|---|---|
| `view/Tema.java` | Constantes de cor, raio e altura; registro das fontes; padrões globais do Swing (`UIManager`); as peças `Painel`, `Botao`, `Selo` e `Arredondada` |
| `view/BotaoUI.java` | Aparência de todo botão comum, inclusive os dos diálogos (`JOptionPane`): tipo Secundário, 44 px, anel de foco |
| `view/BarraRolagem.java` | Barra de rolagem fina e rosa, sem setas |
| `view/Logo.java` | Carrega `recursos/logo.png` e entrega no tamanho pedido, sempre proporcional |
| `view/Ui.java` | Montagem de tela: `caixa()` (seção com título), `secao()`, `bloco()` (rótulo acima do campo), `pilha()`, `rolagem()` |
| `view/TelaPrincipal.java` | Fábricas de botão (`botaoPrimario`, `botaoCinza`, `botaoSuave`, `botaoNeutro`), o campo de texto `Campo` e o estilo dos selects |
| `src/recursos/` | `logo.png` e `fontes/` (Inter Regular/Bold, Outfit SemiBold/Bold) |

`src/recursos` precisa estar no classpath. Os scripts `compilar_e_executar`
rodam com `-cp out:src`; na IDE, basta `src` ser pasta de fontes. Sem os
recursos o sistema abre, mas sem logo e com a fonte padrão do computador.

### 10.2 Constantes (`Tema.java`)

```java
// Marca
PRIMARIA        = new Color(0xC2255C);   // rosa principal
PRIMARIA_ESCURA = new Color(0xA61E4D);   // hover / texto de destaque
PRIMARIA_PRESS  = new Color(0x7A1238);   // pressionado

// Superfícies
SUPERFICIE      = Color.WHITE;           // cartões, campos, tabelas, menu
FUNDO           = new Color(0xFFF7FA);   // fundo geral
ROSA_CLARO      = new Color(0xFFF0F5);   // caixas, botões suaves, seleção

// Linhas
BORDA_SECAO     = new Color(0xF6DCE5);
BORDA_CARTAO    = new Color(0xF1D3DD);
BORDA_CAMPO     = new Color(0xE8BFCD);
BORDA_CLARA     = new Color(0xEFC4D3);
LINHA_TABELA    = new Color(0xFBE9EF);
BORDA_SELO      = new Color(0xD9B8C4);

// Texto
TEXTO           = new Color(0x3B1F2B);
TEXTO_MEDIO     = new Color(0x5E4350);
TEXTO_SUAVE     = new Color(0x7A5C68);

// Status
ERRO            = new Color(0x9B1C1C);   ERRO_BG   = new Color(0xFDECEC);

// Etapas  (Tema.corEtapa(etapa) devolve a cor certa)
ORCAMENTO       = new Color(0xF8BBD0);
LOCADO          = new Color(0xE64980);
DEVOLVIDO       = new Color(0x8E1F4D);

// Sombra (só nos cartões de reserva)
SOMBRA          = new Color(166, 30, 77, 15);

// Formas — o Swing recebe o RAIO; o canto desenhado é o dobro (18/16/14/12/8)
RAIO_SECAO = 9, RAIO_CAIXA = 8, RAIO_CARTAO = 7, RAIO_CONTROLE = 6, RAIO_PEQUENO = 4;
ALTURA     = 44;                         // altura mínima de botão, campo e item de menu
```

### 10.3 Fontes

```java
static Font titulo(int estilo, int tam);   // Outfit → Poppins → Inter
static Font fonte(int estilo, int tam);    // Inter → sans do sistema
static Font fonteSecao();                  // título de seção: 12 px, negrito, espaçado
```

As quatro fontes estão **embutidas** em `src/recursos/fontes/` e são registradas
pelo `Tema` ao iniciar, então o visual é o mesmo em qualquer computador:

| Arquivo | Uso |
|---|---|
| `Inter-Regular.otf`, `Inter-Bold.otf` | Textos, rótulos, tabelas, botões |
| `Outfit-Bold.ttf` | Títulos de tela, números de estatística, total do orçamento |
| `Outfit-SemiBold.ttf` | Reservada (peso 600 da seção 4); hoje os títulos usam o Bold |

Regras: manter os nomes dos arquivos (o `Tema` procura exatamente esses) e usar
sempre os arquivos **estáticos** da Outfit, nunca o "VariableFont", que o Java
carrega só no peso mais fino. Licença das duas: SIL Open Font License 1.1.

### 10.4 Como usar na prática

| Preciso de… | Uso… |
|---|---|
| Botão principal / secundário / suave / neutro | `TelaPrincipal.botaoPrimario`, `botaoCinza`, `botaoSuave`, `botaoNeutro` |
| Seção com título em maiúsculas | `Ui.caixa("TÍTULO", conteudo)` |
| Seção sem título (ex.: em volta de uma tabela) | `Ui.secao(conteudo)` |
| Campo com rótulo acima | `Ui.bloco("Rótulo", campo)` |
| Formulário empilhado, cada item na sua altura | `Ui.pilha(espaco, itens...)` |
| Tabela com rolagem sem borda | `Ui.rolagem(tabela)` |
| Selo de situação | `Tema.Selo.urgente(t)`, `.normal(t)`, `.neutro(t)` |
| Cor de uma etapa | `Tema.corEtapa(etapa)` |
| Caixa de erro / aviso | `Tema.Painel` com fundo `ERRO_BG` e texto `ERRO` |

Peças desenhadas pelo `Tema` (Swing não tem canto arredondado nem sombra nativos):

| Classe | O que faz |
|---|---|
| `Tema.Painel` | `JPanel` com cantos arredondados, borda fina, faixa de etapa e sombra opcionais |
| `Tema.Botao` | `JButton` arredondado, com hover, pressionado, desabilitado e anel de foco |
| `Tema.Selo` | `JLabel` em pílula |
| `Tema.Arredondada` | `Border` arredondada para campos e caixas |

Todas usam `RenderingHints.VALUE_ANTIALIAS_ON` (`Tema.suave(g)`); sem isso o
canto arredondado fica serrilhado.

### 10.5 Valores praticados no Swing

O protótipo foi desenhado para 1280 × 800 em tecnologia web. A janela do sistema
abre em 1120 × 720 e o Swing desenha texto um pouco maior para o mesmo número.
Por isso alguns valores foram ajustados na aplicação. **Estes são os valores
oficiais do sistema:**

| Item | Protótipo (seções 2 a 8) | No sistema |
|---|---|---|
| Janela principal | 1280 × 800, mínimo 1024 × 700 | Abre em 1120 × 720, mínimo 980 × 620; F11 = tela cheia |
| Janela de login | — | 1000 × 640, mínimo 860 × 600 |
| Largura do menu lateral | 220 px | 190 px |
| Logo no menu / no login | 104 px / 260 px | 96 px / 240 px |
| Margem da área de trabalho | 28 px / 32 px | 24 px em cima e embaixo, 28 px nas laterais |
| Texto de tabela | 14 px | 13 px |
| Altura da linha de tabela e de lista | 52 px (lista de clientes) | 36 px |
| Coluna de resumo do orçamento | 280 px | 300 px |
| Ficha do cliente (coluna direita) | — | 470 px |
| Valores monetários em tabela | Alinhados à direita | Alinhados à esquerda (ver Pendências) |

Tudo o mais (cores, raios, alturas de 44/48 px, tipos de botão, selos, estados)
está aplicado como nas seções 3 a 7.

### 10.6 O que não trazer do protótipo

| No protótipo | No Swing |
|---|---|
| Degradê | Não usar. Cor sólida. |
| Sombra em vários níveis | Uma sombra só, nos cartões de reserva |
| Transição / animação | Não usar. Troca de cor direta no hover. |
| Símbolos especiais (✓, →, ▾) | Desenhar com `Graphics2D`. A fonte pode não ter o símbolo e ele vira um quadrado vazio — já aconteceu neste projeto. |
| Rolagem suave | Rolagem padrão do Swing, com a barra do `BarraRolagem` |

### 10.7 Regras para quem for mexer nas telas

1. **Nenhum `new Color(...)` fora do `Tema.java`.** Precisa de uma cor nova?
   Registre na seção 3 e crie a constante.
2. **Nenhum `new Font(...)` nas telas.** Use `Tema.fonte` ou `Tema.titulo`.
3. **Botão novo sai de uma das quatro fábricas** (10.4). Um botão principal por tela.
4. **Campo novo entra com `Ui.bloco`**, para o rótulo ficar acima.
5. **Altura de controle é `Tema.ALTURA`**, nunca um número solto.
6. **Status sempre com texto**, nunca só a cor.
7. Depois de alterar uma tela, abrir o sistema e conferir: hover, foco pelo Tab,
   estado desabilitado e a janela no tamanho mínimo.
8. **Banco de dados e visual não vão na mesma entrega.** Ao integrar o MySQL,
   não alterar a camada visual junto: se algo quebrar, fica claro que a causa é
   a persistência.

---

## 11. Decisões registradas

| Decisão | Motivo |
|---|---|
| Rosa e branco, no lugar do azul | Escolha da equipe; combina com o público de festas infantis |
| Logo recolorida, desenho mantido | A arte é oficial; só a cor muda |
| Sem degradê | O Swing não reproduz bem, e o visual chapado envelhece menos |
| Sem fonte serifada | A palavra ARIEL da logo é sem serifa; manter coerência |
| Etapas variam em intensidade, não só em tom | Acessibilidade (seção 9.3) |
| Aviso de estoque na própria tela, não em diálogo | O usuário precisa ver o aviso enquanto corrige a tabela |
| Design aplicado antes da integração com o banco | Decisão da equipe: o sistema já deve se apresentar no visual aprovado |
| Fontes embutidas no projeto | Mac e Windows mostram o mesmo visual, sem depender de fonte instalada |
| Medidas menores que as do protótipo (10.5) | A janela do sistema é menor que a prancha do protótipo; manter as medidas do protótipo cortaria colunas |

---

## 12. Histórico

| Versão | Data | O que mudou |
|---|---|---|
| 1.0 | 16/11/2026 | Primeira versão. Paleta rosa e branco, logo recolorida, 4 telas prototipadas (Login, Painel, Novo orçamento, Clientes). |
| 1.1 | 16/11/2026 | Design aplicado ao código Java em todas as telas e diálogos. As 5 telas sem protótipo seguem as mesmas regras (seções 3 a 7). |
| 1.2 | 16/11/2026 | Design oficializado. Fonte Outfit embutida. Documentados: layout das 5 telas construídas sem protótipo (8.5), arquivos da camada visual, modo de uso, valores praticados no Swing e regras de manutenção (seção 10). |

### Pendências

- [x] Aplicar o design ao código
- [x] Embutir as fontes Inter e Outfit no projeto
- [ ] Conferir o visual no Windows e no Mac antes da apresentação (as telas foram fotografadas em Linux)
- [ ] Protótipos de Catálogo, Locações, Devoluções, Relatórios e Configurações (hoje vale a tabela de 8.5)
- [ ] Alinhar valores monetários à direita nas tabelas (regra de 6.6 ainda não aplicada)
- [ ] Devoluções: três títulos de coluna aparecem cortados na largura padrão da janela
- [ ] Renomear no código os métodos herdados do visual antigo (`botaoCinza`, `botaoPreto`) para os nomes dos tipos de botão
- [ ] Substituir o PNG da logo pela versão feita a partir do vetor original
