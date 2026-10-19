package model;

/** Zonas do campus. Zonas do mesmo grupo são consideradas próximas. */
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

    /** 1.0 se for a mesma zona, 0.6 se for de zona vizinha, 0 caso contrário. */
    public double proximidade(Zona outra) {
        if (this == outra) {
            return 1.0;
        }
        if (this.grupo == outra.grupo && this.grupo != 9) {
            return 0.6;
        }
        return 0;
    }

    @Override
    public String toString() {
        return nome;
    }
}
