package model;

/** Categorias de objectos. Cada categoria define sensibilidade, foto e prazo de guarda (RN02, RN06, RN17). */
public enum Categoria {
    DOCUMENTO("Documento de identificação", true, false, 30),
    CARTAO("Cartão (estudante, bancário)", true, false, 30),
    TELEMOVEL("Telemóvel", true, true, 90),
    CARTEIRA("Carteira", true, true, 90),
    CHAVES("Chaves", false, true, 90),
    MOCHILA("Mochila ou saco", false, true, 90),
    MATERIAL_ESTUDO("Material de estudo", false, true, 90),
    ELECTRONICO("Portátil, carregador, auscultadores", false, true, 90),
    VESTUARIO("Vestuário", false, true, 90),
    OUTRO("Outro", false, true, 90);

    private final String nome;
    private final boolean sensivel;
    private final boolean fotoObrigatoria;
    private final int prazoGuardaDias;

    Categoria(String nome, boolean sensivel, boolean fotoObrigatoria, int prazoGuardaDias) {
        this.nome = nome;
        this.sensivel = sensivel;
        this.fotoObrigatoria = fotoObrigatoria;
        this.prazoGuardaDias = prazoGuardaDias;
    }

    public String getNome() {
        return nome;
    }

    /** Objectos sensíveis nunca mostram foto nem descrição no catálogo público (RN06). */
    public boolean isSensivel() {
        return sensivel;
    }

    /** A foto é obrigatória, excepto para documentos e cartões (RN02). */
    public boolean isFotoObrigatoria() {
        return fotoObrigatoria;
    }

    public int getPrazoGuardaDias() {
        return prazoGuardaDias;
    }

    @Override
    public String toString() {
        return nome;
    }
}
