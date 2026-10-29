package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pedido de quem perdeu um objecto. Fica activo durante 60 dias. */
public class Procura implements Serializable {

    public static final int DIAS_VALIDADE = 60;

    private final int id;
    private final Utilizador dono;
    private final Categoria categoria;
    private final String descricao;
    private final String cor;
    private final String marca;
    private final Zona localProvavel;
    private final LocalDate perdidoEm;
    private final LocalDate validaAte;

    private boolean activa = true;
    private boolean avisoExpiracaoEnviado;

    public Procura(int id, Utilizador dono, Categoria categoria, String descricao, String cor, String marca,
                   Zona localProvavel, LocalDate perdidoEm, LocalDateTime criadaEm) {
        this.id = id;
        this.dono = dono;
        this.categoria = categoria;
        this.descricao = descricao;
        this.cor = cor;
        this.marca = marca;
        this.localProvavel = localProvavel;
        this.perdidoEm = perdidoEm;
        this.validaAte = criadaEm.toLocalDate().plusDays(DIAS_VALIDADE);
    }

    public int getId() {
        return id;
    }

    public Utilizador getDono() {
        return dono;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCor() {
        return cor;
    }

    public String getMarca() {
        return marca;
    }

    public Zona getLocalProvavel() {
        return localProvavel;
    }

    public LocalDate getPerdidoEm() {
        return perdidoEm;
    }

    public LocalDate getValidaAte() {
        return validaAte;
    }

    public boolean isActiva() {
        return activa;
    }

    /** Fecha a procura (objecto devolvido ou prazo terminado). */
    public void encerrar() {
        this.activa = false;
    }

    public boolean isAvisoExpiracaoEnviado() {
        return avisoExpiracaoEnviado;
    }

    public void setAvisoExpiracaoEnviado(boolean enviado) {
        this.avisoExpiracaoEnviado = enviado;
    }

    @Override
    public String toString() {
        return "Procura #" + id + " | " + categoria + " | " + descricao + " | "
                + (activa ? "activa até " + validaAte : "encerrada");
    }
}
