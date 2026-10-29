# Análise do problema e do código — Fase 2

Sistema Universitário de Gestão e Correspondência de Objectos Perdidos e Encontrados (UJC) — Grupo 4

Este documento responde a quatro perguntas:
1. Qual é o problema, em poucas linhas?
2. O que estava mal ou a mais no código da fase 1?
3. Onde se aplicou cada paradigma (orientado a objectos, funcional, lógico) e porquê?
4. É preciso o paradigma concorrente? E o orientado a eventos?

---

## 1. O problema em poucas linhas

Quem encontra um objecto regista-o e entrega-o num sector (Secretaria ou Biblioteca). Quem perde um objecto regista a perda.
O sistema tem de: (P01) registar e guardar o objecto, (P02) ligar perdas a achados, (P03) verificar se quem reclama é mesmo o dono e entregar,
(P04) controlar prazos e dar destino ao que ninguém reclama.

O que torna o problema difícil não é o volume de dados, é o conjunto de **regras**:

| Tipo de dificuldade | Exemplos (da análise AS-IS) |
|---|---|
| Muitas regras que mudam com o tempo | O que é "sensível", prazos de 30/90 dias, destino por categoria, quando duas coisas "correspondem" |
| Ciclo de vida com estados | Um objecto não pode saltar de PRE_REGISTADO para DEVOLVIDO |
| Decisões sobre prova | Quando a resposta do reclamante chega para reservar o objecto |
| Duas pessoas ao mesmo tempo | Dois reclamantes do mesmo objecto (RN12) |
| Coisas que acontecem com o passar do tempo | Pré-registo expira às 48 h, procura aos 60 dias, aviso aos 15 dias |

Esta lista é a base de todas as decisões abaixo.

## 2. O que havia de errado no código da fase 1

| Problema | Onde | Consequência |
|---|---|---|
| Regras de negócio escondidas em `if`, `switch` e pesos numéricos | `Categoria`, `Destino`, `CorrespondenciaService` (pesos .25/.20/...), `ReclamacaoService.pontuarVerificacao` | Para mudar uma regra é preciso mexer em código e ninguém não programador a consegue ler |
| Classes que não fazem nada de diferente | `Estudante`, `Docente`, `Gestor`, `FuncionarioSector` (subclasses de `Utilizador` com 15 linhas) | Herança sem comportamento próprio: um campo `perfil` chega |
| Classes só para guardar um valor ou envolver outra | `Notificacao`, `Movimento`, `Correspondencia`, `Destino`, `EstadoProcura`, `Persistencia`, `Sistema`, `Consola`, `DadosIniciais`, `Datas` | Mais ficheiros para ler sem mais funcionalidade |
| Serviços muito pequenos | `NotificacaoService`, `AutenticacaoService`, `CatalogoService`, `RelatorioService`, `CorrespondenciaService` | Código espalhado; o júnior tem de abrir 8 ficheiros para seguir um processo |
| Ciclos `for (int i = 0; ...)` repetidos para filtrar listas | Todos os serviços | Muito código para dizer "os objectos DISPONIVEL"; fácil enganar-se |
| Duas verificações separadas no mesmo sítio | `submeter` verificava e reservava sem protecção | Em duas threads, o mesmo objecto podia ser reservado duas vezes |

**Resultado:** 39 ficheiros e 3 177 linhas (sem testes).

## 3. Decisões por paradigma

### 3.1 Orientado a objectos — aplicado (é a base)

**Porquê:** o domínio tem "coisas" com dados e regras próprias (Objecto, Procura, Reclamacao, Utilizador). É o encaixe natural.

**Como ficou:**
- Cada classe do pacote `model` protege os seus dados (campos `private`) e só muda por métodos com nome claro.
- `Objecto.mudarEstado(...)` é o **único** sítio onde o estado muda. Confere a mudança com `EstadoObjecto.podePassarPara(...)` e escreve o histórico. Uma mudança inválida lança erro: o objecto nunca fica num estado impossível.
- A máquina de estados está no `enum EstadoObjecto`; quem lê vê logo o ciclo de vida completo (30 linhas).
- `Utilizador` guarda só o *hash* da palavra-passe (SHA-256) e tem `ehFuncionarioDe(sector)`.
- **Herança retirada.** Um utilizador é um `Utilizador` com um `Perfil` (enum). Menos 4 classes.

### 3.2 Funcional — aplicado (nas consultas, estatísticas e extracção de factos)

**Porquê:** muitas tarefas são "pegar numa lista, filtrar, transformar, contar". Funções puras (sem alterar nada) são fáceis de ler e de testar.

**Como ficou** (`funcional/Consultas.java` e `funcional/Texto.java`):
- `catalogo`, `pesquisar`, `candidatos`, `procurasCompativeis`: um `stream().filter(...).collect(...)` em vez de um ciclo com índice.
- `contarPorEstado`, `contarPorCategoria`: `groupingBy` + `counting`.
- `Texto.cobertura`, `Texto.sobreposicao`: funções sem efeitos secundários.
- `caracteristicas(procura, objecto)` e `caracteristicasProva(reclamacao)`: transformam objectos numa lista de **factos** (`mesma_cor`, `local_proximo`, `detalhe_correcto`, ...). Não decidem nada.

**Onde NÃO se usou:** as alterações de estado (`mudarEstado`, `submeter`, ...) são código normal com efeitos. Forçar estilo funcional aí só complicava.

### 3.3 Lógico — aplicado (nas regras que mudam)

**Porquê:** as regras de negócio são afirmações do tipo "isto é verdade se...". Escritas como regras, ficam num ficheiro que se lê como o texto do regulamento.

**Como ficou:**
- `regras/regras.pl` contém factos e regras em estilo Prolog (sensível, foto obrigatória, prazo de guarda, destino sugerido, correspondência, verificação suficiente).
- `logico/MotorLogico.java` (cerca de 230 linhas com comentários) é um motor mínimo: factos, regras, variáveis, *backtracking* e negação (`\+`). Não precisa de bibliotecas nem de instalar Prolog.
- `logico/Regras.java` esconde o motor: o resto do código chama `Regras.sensivel("telemovel")` ou `Regras.correspondencia(factos)`.

**A divisão de trabalho é esta:**

```
Funcional  : "estes factos são verdadeiros"  ->  [mesma_categoria, mesma_cor, local_proximo]
Lógico     : "com estes factos, há correspondência?"  ->  sim / não   (regras.pl)
OO         : "então o objecto muda de estado"  ->  objecto.mudarEstado(...)
```

**Vantagem prática:** mudar o prazo de documentos de 30 para 45 dias, ou aceitar uma nova forma de correspondência, é editar uma linha em `regras.pl` — sem compilar de novo.

**Substituição de pesos por regras:** na fase 1 a correspondência era uma soma de pontos (cor .25 + marca .20 + ...) com um limiar de 0,50. Agora são 5 regras explícitas (ver `regras.pl`). Cada decisão pode ser explicada ("coincidiu a categoria, a cor e o local"), o que a soma não permitia.

**Limites assumidos:** o motor não é um Prolog completo (não há listas, nem aritmética, nem termos dentro de termos). Chega para este problema; se as regras crescerem muito, a evolução natural é usar o SWI-Prolog.

### 3.4 Concorrente — avaliado: só é necessário o mínimo

**Pergunta a fazer:** existe algum ponto em que duas execuções ao mesmo tempo estragam os dados, ou algum trabalho tão lento que obrigue a fazer duas coisas ao mesmo tempo?

| Candidato | Há problema real? | Decisão |
|---|---|---|
| Dois reclamantes do mesmo objecto (RN12) | **Sim.** `submeter` lê o estado e depois reserva; entre as duas operações outro pode reservar | **Aplicado o mínimo:** `synchronized (base)` à volta das operações que alteram dados |
| Rotina de prazos (P04) | Corre em segundos; só precisa de não coincidir com outras alterações | Fica coberta pelo mesmo `synchronized` |
| Pesquisar candidatos, estatísticas | Lista pequena (centenas de itens), milissegundos | **Não** paralelizar: não há ganho |
| Várias threads/`ExecutorService` | Não há tarefa lenta nem pedidos em paralelo (aplicação de consola, um utilizador de cada vez) | **Não** usar |

**Prova de que o mínimo é necessário.** O teste `doisReclamantesAoMesmoTempo` lança 2 threads 50 vezes sobre o mesmo objecto. Com o `synchronized`, passa sempre (só um reserva). Retirando o `synchronized` de `submeter`, **falha** — foi verificado.

**O que NÃO se fez:** locks por objecto, filas, `ExecutorService`, `ConcurrentHashMap`. Um único bloqueio grosseiro é suficiente e é fácil de perceber. Se o sistema passar a ter muitos utilizadores simultâneos (portal web), então estuda-se bloqueio por objecto ou uma base de dados com transacções.

### 3.5 Orientado a eventos — avaliado: não é necessário agora

**Pergunta a fazer:** há partes do sistema que têm de reagir a algo que acontece, sem que ninguém peça, ou vários consumidores para o mesmo acontecimento?

| Candidato | Situação actual | Decisão |
|---|---|---|
| Avisar o dono quando aparece objecto compatível | Chamada directa na mesma operação (2 linhas) | Não precisa de eventos |
| Notificações (aviso ao achador, funcionário, gestor) | `BaseDados.avisar(...)` guarda a mensagem | Não precisa |
| Prazos (48 h, 15 dias, 60 dias) | Rotina que o Gestor lança; é um *polling* simples | Não precisa; um temporizador diário chega |

Acrescentar `EventBus`, `Listener`s e eventos teria sido "aplicar o paradigma só por aplicar" — era mais código e mais classes sem resolver nenhum problema.

**Quando passa a fazer sentido** (e onde se mexeria):
1. Enviar email/SMS/WhatsApp reais: deixa de ser uma linha; pode demorar e falhar. Aí cria-se o evento `ObjectoDisponivel` e um *listener* que envia, sem atrasar `confirmarEntrada`.
2. Mais de um consumidor para o mesmo acontecimento (notificação + auditoria + painel do gestor).
3. Prazos em tempo real (um temporizador por objecto) em vez da rotina diária.

O ponto de entrada seria `RegistoService.confirmarEntrada` e `ReclamacaoService.avaliar`, onde hoje estão as chamadas a `base.avisar(...)`. Está documentado em `PrazoService` (cabeçalho).

## 4. Resumo: o que está onde

| Paradigma | Ficheiros | Aplicado? |
|---|---|---|
| Orientado a objectos | `model/*`, `repository/BaseDados`, `service/*` | Sim |
| Funcional | `funcional/Consultas`, `funcional/Texto` | Sim |
| Lógico | `regras/regras.pl`, `logico/MotorLogico`, `logico/Regras` | Sim |
| Concorrente | `synchronized (base)` em `service/*`, teste das 2 threads | Só o mínimo |
| Orientado a eventos | — | Não (documentado quando seria) |

## 5. Antes e depois

| | Fase 1 | Fase 2 |
|---|---|---|
| Ficheiros `.java` (sem testes) | 39 | 23 |
| Linhas (sem testes) | 3 177 | 2 417 |
| Subclasses de `Utilizador` | 4 | 0 (campo `perfil`) |
| Serviços | 8 | 3 (`RegistoService`, `ReclamacaoService`, `PrazoService`) |
| Regras de negócio escritas em | `if`/`switch`/pesos em Java | ficheiro `regras/regras.pl` |
| Dois reclamantes ao mesmo tempo | podia falhar | protegido e testado |
| Verificações automáticas | 35 | 72 |

## 6. Como ler o código (ordem recomendada para um júnior)

1. `model/EstadoObjecto.java` — o ciclo de vida do objecto.
2. `model/Objecto.java` — um objecto e o método `mudarEstado`.
3. `regras/regras.pl` — as regras em texto.
4. `funcional/Consultas.java` — como se escolhem factos e candidatos.
5. `service/RegistoService.java` → `ReclamacaoService.java` → `PrazoService.java` — os processos P01 a P04.
6. `app/Demonstracao.java` — o fluxo todo a correr.
7. `logico/MotorLogico.java` — por último (é a parte mais técnica; não é preciso entendê-la para usar as regras).
