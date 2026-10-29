package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Reclamação de um objecto por quem diz ser o dono. */
public class Reclamacao implements Serializable {

    private final int id;
    private final Objecto objecto;
    private final Procura procura;          // a procura do reclamante (pode ser null)
    private final Utilizador reclamante;

    private String resposta;                // resposta à pergunta sobre o detalhe oculto
    private String provas;
    private EstadoReclamacao estado;
    private LocalDateTime prazoResposta;    // até quando pode juntar provas
    private LocalDate prazoLevantamento;    // até quando pode levantar o objecto
    private String motivo;                  // motivo da última decisão
    private boolean recursoUsado;
    private int tentativas = 1;

    public Reclamacao(int id, Objecto objecto, Procura procura, Utilizador reclamante, String resposta,
                      String provas) {
        this.id = id;
        this.objecto = objecto;
        this.procura = procura;
        this.reclamante = reclamante;
        this.resposta = resposta;
        this.provas = provas;
    }

    /** Uma reclamação está "activa" enquanto ainda pode terminar com a entrega do objecto. */
    public boolean estaActiva() {
        return estado == EstadoReclamacao.PROVA_ADICIONAL || estado == EstadoReclamacao.EM_VERIFICACAO
                || estado == EstadoReclamacao.APROVADA || estado == EstadoReclamacao.EM_RECURSO;
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

    public String getResposta() {
        return resposta;
    }

    public void setResposta(String resposta) {
        this.resposta = resposta;
    }

    public String getProvas() {
        return provas;
    }

    public void setProvas(String provas) {
        this.provas = provas;
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

    public void setPrazoResposta(LocalDateTime prazoResposta) {
        this.prazoResposta = prazoResposta;
    }

    public LocalDate getPrazoLevantamento() {
        return prazoLevantamento;
    }

    public void setPrazoLevantamento(LocalDate prazoLevantamento) {
        this.prazoLevantamento = prazoLevantamento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isRecursoUsado() {
        return recursoUsado;
    }

    public void setRecursoUsado(boolean recursoUsado) {
        this.recursoUsado = recursoUsado;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void maisUmaTentativa() {
        tentativas++;
    }

    @Override
    public String toString() {
        return "Reclamação #" + id + " | " + objecto.getCodigo() + " | " + reclamante.getNome() + " | " + estado;
    }
}
