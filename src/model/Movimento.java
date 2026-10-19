package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Registo de custódia e auditoria: quem fez o quê e quando (RN15, RN16). */
public class Movimento implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LocalDateTime quando;
    private final String autor;
    private final String accao;
    private final EstadoObjecto de;
    private final EstadoObjecto para;
    private final String nota;

    public Movimento(LocalDateTime quando, String autor, String accao, EstadoObjecto de,
                     EstadoObjecto para, String nota) {
        this.quando = quando;
        this.autor = autor;
        this.accao = accao;
        this.de = de;
        this.para = para;
        this.nota = nota;
    }

    public LocalDateTime getQuando() {
        return quando;
    }

    public String getAutor() {
        return autor;
    }

    public String getAccao() {
        return accao;
    }

    public EstadoObjecto getPara() {
        return para;
    }

    @Override
    public String toString() {
        String estados = (de == null ? "-" : de.name()) + " -> " + para.name();
        return quando.toString().replace('T', ' ').substring(0, 16) + " | " + autor + " | " + accao
                + " | " + estados + (nota == null || nota.isEmpty() ? "" : " | " + nota);
    }
}
