#!/bin/sh
# Compila todo o código para a pasta out/
cd "$(dirname "$0")/.." || exit 1
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src tests -name '*.java') && echo "Compilado."
