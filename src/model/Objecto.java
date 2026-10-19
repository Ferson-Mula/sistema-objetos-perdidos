package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Objecto encontrado e entregue (ou a entregar) a um sector. */
public class Objecto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String codigo;
    private final Categoria categoria;
    private final String descricao;
    private final String cor;
    private final String marca;
    private final Zona local;
    private final LocalDateTime dataEncontrado;
    private final String foto;
    private final String detalheOculto;
    private final Utilizador achador;
    private final Sector sector;
    private final LocalDateTime criadoEm;

    private EstadoObjecto estado = EstadoObjecto.PRE_REGISTADO;
    private String localizacaoFisica;
    private LocalDateTime entradaEm;
    private LocalDate prazoGuarda;
    private boolean avisoPrazoEnviado;
    private Destino destino;
    private final List<Movimento> historico = new ArrayList<Movimento>();

    public Objecto(String codigo, Categoria categoria, String descricao, String cor, String marca, Zona local,
                   LocalDateTime dataEncontrado, String foto, String detalheOculto, Utilizador achador,
                   Sector sector, LocalDateTime criadoEm) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.cor = cor;
        this.marca = marca;
        this.local = local;
        this.dataEncontrado = dataEncontrado;
        this.foto = foto;
        this.detalheOculto = detalheOculto;
        this.achador = achador;
        this.sector = sector;
        this.criadoEm = criadoEm;
        historico.add(new Movimento(criadoEm, achador.getNome(), "Pré-registo", null, EstadoObjecto.PRE_REGISTADO,
                "Entrega prevista na " + sector));
    }

    /** Única forma de mudar o estado: valida a transição e regista o movimento (RN16). */
    public void mudarEstado(EstadoObjecto novo, String autor, String accao, String nota, LocalDateTime quando) {
        if (!estado.podePassarPara(novo)) {
            throw new RegraNegocioException("Transição inválida do objecto " + codigo + ": " + estado + " -> " + novo);
        }
        historico.add(new Movimento(quando, autor, accao, estado, novo, nota));
        estado = novo;
    }

    public void registarEntrada(String localizacaoFisica, LocalDateTime quando) {
        this.localizacaoFisica = localizacaoFisica;
        this.entradaEm = quando;
        this.prazoGuarda = quando.toLocalDate().plusDays(categoria.getPrazoGuardaDias());
    }

    public String getCodigo() {
        return codigo;
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

    public Zona getLocal() {
        return local;
    }

    public LocalDateTime getDataEncontrado() {
        return dataEncontrado;
    }

    public String getFoto() {
        return foto;
    }

    public String getDetalheOculto() {
        return detalheOculto;
    }

    public Utilizador getAchador() {
        return achador;
    }

    public Sector getSector() {
        return sector;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public EstadoObjecto getEstado() {
        return estado;
    }

    public String getLocalizacaoFisica() {
        return localizacaoFisica;
    }

    public LocalDateTime getEntradaEm() {
        return entradaEm;
    }

    public LocalDate getPrazoGuarda() {
        return prazoGuarda;
    }

    public boolean isAvisoPrazoEnviado() {
        return avisoPrazoEnviado;
    }

    public void setAvisoPrazoEnviado(boolean avisoPrazoEnviado) {
        this.avisoPrazoEnviado = avisoPrazoEnviado;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }

    public List<Movimento> getHistorico() {
        return historico;
    }

    /** Momento em que o objecto foi devolvido, ou null. */
    public LocalDateTime getDevolvidoEm() {
        for (int i = 0; i < historico.size(); i++) {
            if (historico.get(i).getPara() == EstadoObjecto.DEVOLVIDO) {
                return historico.get(i).getQuando();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return codigo + " | " + categoria + " | " + descricao + " | " + estado;
    }
}
