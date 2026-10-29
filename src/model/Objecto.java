package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Um objecto encontrado, entregue (ou a entregar) a um sector. */
public class Objecto implements Serializable {

    private final String codigo;
    private final Categoria categoria;
    private final String descricao;
    private final String cor;
    private final String marca;
    private final Zona local;
    private final LocalDateTime encontradoEm;
    private final String foto;
    private final String detalheOculto;   // só os funcionários o veem
    private final Utilizador achador;
    private final Sector sector;
    private final LocalDateTime criadoEm;

    private EstadoObjecto estado = EstadoObjecto.PRE_REGISTADO;
    private String localizacaoFisica;
    private LocalDateTime entradaEm;
    private LocalDate prazoGuarda;
    private boolean avisoPrazoEnviado;
    private String destino;
    private final List<String> historico = new ArrayList<>();

    public Objecto(String codigo, Categoria categoria, String descricao, String cor, String marca, Zona local,
                   LocalDateTime encontradoEm, String foto, String detalheOculto, Utilizador achador,
                   Sector sector, LocalDateTime criadoEm) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.cor = cor;
        this.marca = marca;
        this.local = local;
        this.encontradoEm = encontradoEm;
        this.foto = foto;
        this.detalheOculto = detalheOculto;
        this.achador = achador;
        this.sector = sector;
        this.criadoEm = criadoEm;
        historico.add(formatar(criadoEm, achador.getNome(), "Pré-registo", "-> " + estado));
    }

    /** Única forma de mudar o estado: confere se a mudança é permitida e regista-a no histórico. */
    public void mudarEstado(EstadoObjecto novo, String autor, String accao, LocalDateTime quando) {
        if (!estado.podePassarPara(novo)) {
            throw new IllegalArgumentException("Mudança inválida no objecto " + codigo + ": " + estado + " -> " + novo);
        }
        historico.add(formatar(quando, autor, accao, estado + " -> " + novo));
        estado = novo;
    }

    /** Chamado quando o funcionário confirma a entrada: guarda o local e calcula o prazo de guarda. */
    public void registarEntrada(String localizacaoFisica, LocalDateTime quando) {
        this.localizacaoFisica = localizacaoFisica;
        this.entradaEm = quando;
        this.prazoGuarda = quando.toLocalDate().plusDays(categoria.getPrazoGuardaDias());
    }

    /** O que qualquer pessoa pode ver no catálogo. Nunca mostra o detalhe oculto. */
    public String resumoPublico() {
        String texto = codigo + " | " + categoria;
        if (categoria.isSensivel()) {
            texto += " | (dados reservados)";
        } else {
            texto += " | " + descricao + (foto == null || foto.isBlank() ? "" : " | foto: " + foto);
        }
        return texto + " | cor: " + cor + " | encontrado: " + local + ", " + encontradoEm.toLocalDate()
                + " | guardado na " + sector;
    }

    private static String formatar(LocalDateTime quando, String autor, String accao, String mudanca) {
        return quando.toString().replace('T', ' ').substring(0, 16) + " | " + autor + " | " + accao + " | " + mudanca;
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

    public LocalDateTime getEncontradoEm() {
        return encontradoEm;
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

    public void setAvisoPrazoEnviado(boolean enviado) {
        this.avisoPrazoEnviado = enviado;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public List<String> getHistorico() {
        return historico;
    }

    @Override
    public String toString() {
        return codigo + " | " + categoria + " | " + descricao + " | " + estado;
    }
}
