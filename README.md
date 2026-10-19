# Sistema Universitário de Gestão e Correspondência de Objectos Perdidos e Encontrados (UJC)

Grupo 4 — Paradigmas de Programação, 3.º Ano, Engenharia em Tecnologias e Sistemas de Informação.
Docente: Eng. Delcinio Chirindza.

## Estado do projecto: Fase 1 (primeiro entregável)

Esta versão implementa os quatro processos TO-BE do relatório (P01 a P04) como uma aplicação de consola
em Java, **sem** concorrência, sem programação distribuída e sem programação orientada a eventos.
Tudo corre num só programa, passo a passo, e cada serviço chama directamente os outros.
O objectivo é ter um sistema que funciona e cujos limites sejam visíveis, para a fase seguinte
(ver `docs/fase2-pontos-de-estrangulamento.md`).

Paradigmas usados nesta fase: **Orientado a Objectos** (modelo, herança, estados, encapsulamento) e
código **imperativo/sequencial** (ciclos e condições).

## Como executar

Requisitos: Java 17 ou superior. Não são precisas bibliotecas nem Maven.

| Acção | Linux / macOS | Windows |
|---|---|---|
| Compilar | `sh scripts/compilar.sh` | `scripts\compilar.bat` |
| Aplicação de consola | `sh scripts/executar.sh` | `scripts\executar.bat` |
| Demonstração automática | `sh scripts/demo.sh` | `scripts\demo.bat` |
| Testes (35 verificações) | `sh scripts/testar.sh` | `scripts\testar.bat` |
| Recomeçar do zero | `sh scripts/executar.sh --reiniciar` | `scripts\executar.bat --reiniciar` |

Os dados ficam em `data/achados.dat` (criado na primeira execução). A demonstração não usa este ficheiro.

### Contas de teste (palavra-passe de todas: `1234`)

| Número | Perfil |
|---|---|
| 20230001, 20230002, 20230003 | Estudantes (Ana Macie, Bruno Tembe, Carla Nhaca) |
| D001 | Docente |
| F001 | Funcionário da Secretaria |
| F002 | Funcionário da Biblioteca |
| G001 | Gestor |

## Estrutura

```
src/
  app/          Main, menus de consola, demonstração, dados iniciais, ligação dos serviços (Sistema)
  model/        Entidades e estados: Utilizador (Estudante, Docente, FuncionarioSector, Gestor),
                Objecto, Procura, Reclamacao, Movimento, Notificacao, enums
  service/      Regras de negócio: Registo (P01/P02), Correspondencia, Catalogo, Reclamacao (P03),
                Prazo (P04), Relatorio, Notificacao, Autenticacao
  repository/   BaseDados (listas em memória) e Persistencia (ficheiro único)
  util/         Texto, Datas, Relogio (permite avançar o tempo na demonstração)
tests/          TestesSistema (testes simples, sem framework)
data/           Ficheiro de dados (gerado)
docs/           Relatório, diagramas BPMN, pontos de estrangulamento
scripts/        Compilar, executar, demo, testar
```

## O que cada processo faz

| Processo | Onde | Resumo |
|---|---|---|
| P01 Registo de objecto encontrado | `RegistoService` | Pré-registo com código, entrega no sector, confirmação pelo funcionário, estado DISPONÍVEL |
| P02 Perda e correspondência | `RegistoService`, `CorrespondenciaService` | Procura de 60 dias; pontuação por cor, marca, local, data e texto; aviso ao dono |
| P03 Reclamação e devolução | `ReclamacaoService` | Desafio com detalhe oculto, provas, reserva, revisão, recurso ao Gestor, levantamento |
| P04 Prazos e destino final | `PrazoService` | Rotina de prazos (executada à mão), destino aprovado pelo Gestor |

As regras RN01 a RN20 do relatório estão referidas nos comentários e mensagens de erro. A tabela do estado
de cada regra está em `docs/fase2-pontos-de-estrangulamento.md`.

## Documentação

- `docs/relatorio/` — trabalho revisto (AS-IS e TO-BE).
- `docs/bpmn/` — diagramas BPMN AS-IS e TO-BE.
- `docs/referencias/` — template TO-BE e notas da discussão.
- `docs/fase2-pontos-de-estrangulamento.md` — o que a fase 1 ainda não resolve.
