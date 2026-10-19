package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Reclamação de um objecto por quem diz ser o dono (P03). */
public class Reclamacao implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Objecto objecto;
    private final Procura procura;
    private final Utilizador reclamante;
    private final LocalDateTime criadaEm;

    private String respostaDetalhe;
    private String provas;
    private double pontuacao;
    private EstadoReclamacao estado;
    private LocalDateTime prazoResposta;
    private LocalDate prazoLevantamento;
    private String motivoDecisao;
    private boolean recursoUsado;
    private int tentativas = 1;

    public Reclamacao(int id, Objecto objecto, Procura procura, Utilizador reclamante, String respostaDetalhe,
                      String provas, LocalDateTime criadaEm) {
        this.id = id;
        this.objecto = objecto;
        this.procura = procura;
        this.reclamante = reclamante;
        this.respostaDetalhe = respostaDetalhe;
        this.provas = provas;
        this.criadaEm = criadaEm;
    }

    public int getId() {
        return id;
    }

    public Objecto getObjecto() {
        return objecto;
    }

    public Procura getProcura() {
        return procura;
    }

    public Utilizador getReclamante() {
        return reclamante;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public String getRespostaDetalhe() {
        return respostaDetalhe;
    }

    public void setRespostaDetalhe(String r) {
        this.respostaDetalhe = r;
    }

    public String getProvas() {
        return provas;
    }

    public void setProvas(String p) {
        this.provas = p;
    }

    public double getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(double pontuacao) {
        this.pontuacao = pontuacao;
    }

    public EstadoReclamacao getEstado() {
        return estado;
    }

    public void setEstado(EstadoReclamacao estado) {
        this.estado = estado;
    }

    public LocalDateTime getPrazoResposta() {
        return prazoResposta;
    }

    public void setPrazoResposta(LocalDateTime p) {
        this.prazoResposta = p;
    }

    public LocalDate getPrazoLevantamento() {
        return prazoLevantamento;
    }

    public void setPrazoLevantamento(LocalDate p) {
        this.prazoLevantamento = p;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public void setMotivoDecisao(String m) {
        this.motivoDecisao = m;
    }

    public boolean isRecursoUsado() {
        return recursoUsado;
    }

    public void setRecursoUsado(boolean r) {
        this.recursoUsado = r;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void incrementarTentativas() {
        tentativas++;
    }

    @Override
    public String toString() {
        return "Reclamação #" + id + " | " + objecto.getCodigo() + " | " + reclamante.getNome() + " | " + estado
                + " | pontuação " + String.format("%.2f", pontuacao);
    }
}
