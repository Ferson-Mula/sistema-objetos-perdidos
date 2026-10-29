#!/bin/sh
# Compila e corre os testes
cd "$(dirname "$0")/.." || exit 1
sh scripts/compilar.sh || exit 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out TestesSistema
