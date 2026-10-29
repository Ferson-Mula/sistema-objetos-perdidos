#!/bin/sh
# Abre o programa (menu). Com o argumento "demo" mostra a demonstração automática.
cd "$(dirname "$0")/.." || exit 1
[ -d out ] || sh scripts/compilar.sh || exit 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out app.Main "$@"
