package model;

import logico.Regras;

/**
 * Tipos de objecto. O que é sensível, se a foto é obrigatória, o prazo de guarda
 * e o destino sugerido vêm das regras (regras/regras.pl), não estão escritos aqui.
 */
public enum Categoria {
    TELEMOVEL("Telemóvel"),
    CARTEIRA("Carteira"),
    DOCUMENTO("Documento de identificação"),
    CARTAO("Cartão (estudante, bancário)"),
    CHAVES("Chaves"),
    MOCHILA("Mochila ou saco"),
    MATERIAL_ESTUDO("Material de estudo"),
    ELECTRONICO("Portátil, carregador, auscultadores"),
    VESTUARIO("Vestuário"),
    OUTRO("Outro");

    private final String nome;

    Categoria(String nome) {
        this.nome = nome;
    }

    /** Nome usado nas regras, por exemplo "telemovel". */
    public String chave() {
        return name().toLowerCase();
    }

    public boolean isSensivel() {
        return Regras.sensivel(chave());
    }

    public boolean isFotoObrigatoria() {
        return Regras.fotoObrigatoria(chave());
    }

    public int getPrazoGuardaDias() {
        return Regras.prazoGuardaDias(chave());
    }

    public String destinoSugerido() {
        return Regras.destinoSugerido(chave());
    }

    @Override
    public String toString() {
        return nome;
    }
}
