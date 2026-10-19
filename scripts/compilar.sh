#!/bin/sh
# Compila o projecto para a pasta out/ (Java 17 ou superior).
cd "$(dirname "$0")/.." || exit 1
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src tests -name '*.java') && echo "Compilado com sucesso."
