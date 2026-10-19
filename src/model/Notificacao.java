package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Mensagem enviada a um utilizador (simulada: fica guardada e é mostrada na consola). */
public class Notificacao implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Utilizador destinatario;
    private final String assunto;
    private final String mensagem;
    private final LocalDateTime criadaEm;
    private boolean lida;

    public Notificacao(int id, Utilizador destinatario, String assunto, String mensagem, LocalDateTime criadaEm) {
        this.id = id;
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.criadaEm = criadaEm;
    }

    public int getId() {
        return id;
    }

    public Utilizador getDestinatario() {
        return destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }

    @Override
    public String toString() {
        return (lida ? "  " : "* ") + criadaEm.toString().replace('T', ' ').substring(0, 16) + " | " + assunto
                + " | " + mensagem;
    }
}
