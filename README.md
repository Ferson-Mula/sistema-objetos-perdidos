# Achados e Perdidos — UJC (Fase 2)

Projecto de Paradigmas de Programação — Grupo 4, UJC, Engenharia em Tecnologias e Sistemas de Informação.

Aplica **orientado a objectos**, **funcional** e **lógico**. Concorrência só no mínimo (exclusão mútua) e eventos
avaliados e não aplicados — ver `docs/analise-paradigmas.md`.

## Requisitos
Java 17 ou superior (`java -version`). Não é preciso Maven nem bibliotecas.

## Como executar (a partir da pasta do projecto)

| O que | Linux / macOS | Windows |
|---|---|---|
| Menu interactivo | `sh scripts/executar.sh` | `scripts\executar.bat` |
| Demonstração automática | `sh scripts/executar.sh demo` | `scripts\executar.bat demo` |
| Testes | `sh scripts/testes.sh` | `scripts\testes.bat` |

Sem scripts:
```
mkdir out
javac -encoding UTF-8 -d out $(find src tests -name '*.java')
java -cp out app.Main          # menu
java -cp out app.Main demo     # demonstração
java -cp out TestesSistema     # testes
```
O programa tem de ser executado a partir da pasta do projecto, porque lê `regras/regras.pl`.

## Contas de teste (palavra-passe `1234`)
| Número | Perfil |
|---|---|
| 20230001, 20230002, 20230003 | Estudantes (Ana, Bruno, Carla) |
| D001 | Docente |
| F001 | Funcionário da Secretaria |
| F002 | Funcionário da Biblioteca |
| G001 | Gestor |

Os dados ficam guardados em `data/base.dat`. Para recomeçar, apague esse ficheiro.

## Estrutura
```
regras/regras.pl      regras de negócio (lógico) — editável sem recompilar
src/model/            classes do domínio (OO)
src/funcional/        funções puras: pesquisa, factos, estatísticas
src/logico/           motor lógico e ponte para o resto do código
src/repository/       BaseDados (listas + ficheiro + avisos)
src/service/          RegistoService (P01, P02), ReclamacaoService (P03), PrazoService (P04)
src/app/              Main, Menus, Demonstracao
tests/                TestesSistema (72 verificações)
docs/                 análise de paradigmas, relatório, diagramas BPMN
```

## Fluxo para experimentar no menu
1. Entre como `20230001` (Ana): registe um objecto encontrado (ex. telemóvel, entregar na Biblioteca). Anote o código.
2. Entre como `F002` (Biblioteca): "Confirmar entrada".
3. Entre como `20230002` (Bruno): "Reclamar um objecto" com o código e um detalhe que o objecto tenha escondido.
4. Entre como `F002`: "Rever reclamações" → aprovar → "Entregar objecto aprovado".
5. Entre como `G001`: "Avançar o relógio" 100 dias → "Executar rotina de prazos" → "Relatório".
