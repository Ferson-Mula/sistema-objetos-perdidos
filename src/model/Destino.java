package model;

/** Destino final de um objecto não reclamado (P04). */
public enum Destino {
    DOACAO("Doação a instituição social"),
    RECICLAGEM("Reciclagem"),
    DESTRUICAO_SEGURA("Destruição segura"),
    ENTREGA_ENTIDADE_EMISSORA("Entrega à entidade emissora");

    private final String nome;

    Destino(String nome) {
        this.nome = nome;
    }

    /** Destino sugerido conforme o tipo do objecto. */
    public static Destino sugerir(Categoria c) {
        if (c == Categoria.DOCUMENTO || c == Categoria.CARTAO) {
            return ENTREGA_ENTIDADE_EMISSORA;
        }
        if (c == Categoria.TELEMOVEL || c == Categoria.CARTEIRA) {
            return DESTRUICAO_SEGURA;
        }
        if (c == Categoria.ELECTRONICO) {
            return RECICLAGEM;
        }
        return DOACAO;
    }

    @Override
    public String toString() {
        return nome;
    }
}
