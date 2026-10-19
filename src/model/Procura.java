package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pedido de procura de um objecto perdido (P02). */
public class Procura implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Utilizador dono;
    private final Categoria categoria;
    private final String descricao;
    private final String cor;
    private final String marca;
    private final Zona localProvavel;
    private final LocalDate perdidoEm;
    private final String detalheOculto;
    private final LocalDateTime criadaEm;
    private final LocalDate validaAte;

    private EstadoProcura estado = EstadoProcura.ACTIVA;
    private boolean avisoExpiracaoEnviado;

    public Procura(int id, Utilizador dono, Categoria categoria, String descricao, String cor, String marca,
                   Zona localProvavel, LocalDate perdidoEm, String detalheOculto, LocalDateTime criadaEm,
                   int diasValidade) {
        this.id = id;
        this.dono = dono;
        this.categoria = categoria;
        this.descricao = descricao;
        this.cor = cor;
        this.marca = marca;
        this.localProvavel = localProvavel;
        this.perdidoEm = perdidoEm;
        this.detalheOculto = detalheOculto;
        this.criadaEm = criadaEm;
        this.validaAte = criadaEm.toLocalDate().plusDays(diasValidade);
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

    public String getDetalheOculto() {
        return detalheOculto;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDate getValidaAte() {
        return validaAte;
    }

    public EstadoProcura getEstado() {
        return estado;
    }

    public void setEstado(EstadoProcura estado) {
        this.estado = estado;
    }

    public boolean isAvisoExpiracaoEnviado() {
        return avisoExpiracaoEnviado;
    }

    public void setAvisoExpiracaoEnviado(boolean v) {
        this.avisoExpiracaoEnviado = v;
    }

    @Override
    public String toString() {
        return "Procura #" + id + " | " + categoria + " | " + descricao + " | " + estado + " | válida até " + validaAte;
    }
}
