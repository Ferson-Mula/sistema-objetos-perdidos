package model;

/** Zonas do campus. Zonas do mesmo grupo são vizinhas. */
public enum Zona {
    BIBLIOTECA("Biblioteca", 1),
    SECRETARIA("Secretaria", 1),
    CANTINA("Cantina", 2),
    CASAS_DE_BANHO("Casas de banho", 2),
    BLOCO_A("Bloco A", 3),
    BLOCO_B("Bloco B", 3),
    BLOCO_C("Bloco C", 3),
    ENTRADA_PRINCIPAL("Entrada principal", 4),
    PARQUE_ESTACIONAMENTO("Parque de estacionamento", 4),
    OUTRA("Outra zona", 9);

    private final String nome;
    private final int grupo;

    Zona(String nome, int grupo) {
        this.nome = nome;
        this.grupo = grupo;
    }

    /** Verdadeiro se for a mesma zona ou uma zona vizinha. */
    public boolean estaPerto(Zona outra) {
        return this == outra || (grupo == outra.grupo && grupo != 9);
    }

    @Override
    public String toString() {
        return nome;
    }
}
