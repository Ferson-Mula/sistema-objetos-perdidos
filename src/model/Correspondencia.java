package model;

/** Par procura/objecto com a pontuação calculada (não é guardado). */
public class Correspondencia {

    private final Procura procura;
    private final Objecto objecto;
    private final double pontuacao;

    public Correspondencia(Procura procura, Objecto objecto, double pontuacao) {
        this.procura = procura;
        this.objecto = objecto;
        this.pontuacao = pontuacao;
    }

    public Procura getProcura() {
        return procura;
    }

    public Objecto getObjecto() {
        return objecto;
    }

    public double getPontuacao() {
        return pontuacao;
    }
}
