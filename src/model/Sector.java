package model;

/** Sectores que guardam objectos. */
public enum Sector {
    SECRETARIA("Secretaria"),
    BIBLIOTECA("Biblioteca");

    private final String nome;

    Sector(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
