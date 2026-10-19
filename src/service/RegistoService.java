package service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import model.Categoria;
import model.EstadoObjecto;
import model.FuncionarioSector;
import model.Objecto;
import model.Procura;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import model.Zona;
import repository.BaseDados;
import util.Relogio;
import util.Texto;

/** P01 (registo de objecto encontrado) e primeira parte do P02 (registo de perda). */
public class RegistoService {

    public static final int DIAS_VALIDADE_PROCURA = 60;

    private final BaseDados base;
    private final Relogio relogio;
    private final NotificacaoService notificacoes;
    private final CorrespondenciaService correspondencia;

    public RegistoService(BaseDados base, Relogio relogio, NotificacaoService notificacoes,
                          CorrespondenciaService correspondencia) {
        this.base = base;
        this.relogio = relogio;
        this.notificacoes = notificacoes;
        this.correspondencia = correspondencia;
    }

    /** P01, passos 2 a 4: o achador regista o objecto e recebe um código (RN01, RN02, RN03). */
    public Objecto registarEncontrado(Utilizador achador, Categoria categoria, String descricao, String cor,
                                      String marca, Zona local, LocalDateTime dataEncontrado, String foto,
                                      String detalheOculto, Sector sector) {
        if (achador == null || categoria == null || local == null || dataEncontrado == null || sector == null) {
            throw new RegraNegocioException("Categoria, local, data e sector são obrigatórios.");
        }
        if (Texto.vazio(descricao) || descricao.trim().length() < 5) {
            throw new RegraNegocioException("A descrição deve ter pelo menos 5 caracteres (RN02).");
        }
        if (dataEncontrado.isAfter(relogio.agora())) {
            throw new RegraNegocioException("A data em que o objecto foi encontrado não pode estar no futuro.");
        }
        if (categoria.isFotoObrigatoria() && Texto.vazio(foto)) {
            throw new RegraNegocioException("A foto é obrigatória para a categoria " + categoria + " (RN02).");
        }
        verificarDuplicado(achador, categoria, descricao, local);

        String codigo = base.proximoCodigoObjecto(relogio.agora().getYear());
        Objecto o = new Objecto(codigo, categoria, descricao.trim(), cor, marca, local, dataEncontrado, foto,
                detalheOculto, achador, sector, relogio.agora());
        base.getObjectos().add(o);
        notificacoes.enviar(achador, "Pré-registo criado",
                "Código " + codigo + ". Entregue o objecto na " + sector + " em 48 horas (RN05).");
        return o;
    }

    private void verificarDuplicado(Utilizador achador, Categoria categoria, String descricao, Zona local) {
        for (int i = 0; i < base.getObjectos().size(); i++) {
            Objecto o = base.getObjectos().get(i);
            boolean aberto = o.getEstado() == EstadoObjecto.PRE_REGISTADO || o.getEstado() == EstadoObjecto.DISPONIVEL;
            if (aberto && o.getAchador().getId() == achador.getId() && o.getCategoria() == categoria
                    && o.getLocal() == local && Texto.normalizar(o.getDescricao()).equals(Texto.normalizar(descricao))) {
                throw new RegraNegocioException("Possível duplicado: já registou este objecto (" + o.getCodigo() + ").");
            }
        }
    }

    /**
     * P01, passos 6 e 7: o funcionário confere o objecto com o registo.
     * Se estiver conforme, passa a DISPONÍVEL e o sistema procura donos (RN03).
     */
    public Objecto confirmarEntrada(FuncionarioSector f, String codigo, boolean conforme, String localizacaoFisica,
                                    String nota) {
        Objecto o = base.objectoPorCodigo(codigo);
        if (o == null) {
            throw new RegraNegocioException("Não existe nenhum objecto com o código " + codigo + " (RN01).");
        }
        if (o.getEstado() != EstadoObjecto.PRE_REGISTADO) {
            throw new RegraNegocioException("O objecto " + o.getCodigo() + " não está à espera de entrega (estado "
                    + o.getEstado() + ").");
        }
        if (o.getSector() != f.getSector()) {
            throw new RegraNegocioException("Este objecto foi destinado à " + o.getSector() + ".");
        }
        LocalDateTime agora = relogio.agora();
        if (!conforme) {
            o.mudarEstado(EstadoObjecto.CANCELADO, f.getNome(), "Entrada recusada", nota, agora);
            notificacoes.enviar(o.getAchador(), "Entrada recusada",
                    "O objecto " + o.getCodigo() + " não estava conforme o registo. " + (nota == null ? "" : nota));
            return o;
        }
        if (Texto.vazio(localizacaoFisica)) {
            throw new RegraNegocioException("Indique a localização física (prateleira, armário) do objecto (RN03).");
        }
        o.registarEntrada(localizacaoFisica.trim(), agora);
        o.mudarEstado(EstadoObjecto.DISPONIVEL, f.getNome(), "Entrada confirmada",
                "Guardado em " + localizacaoFisica.trim(), agora);
        notificacoes.enviar(o.getAchador(), "Objecto recebido",
                "O objecto " + o.getCodigo() + " está registado e visível no catálogo. Obrigado!");
        avisarTitularSeIdentificavel(o);
        correspondencia.avisarProcurasCompativeis(o);
        return o;
    }

    /** RN06: documentos e cartões com o número do titular no detalhe oculto avisam directamente o titular. */
    private void avisarTitularSeIdentificavel(Objecto o) {
        if ((o.getCategoria() == Categoria.DOCUMENTO || o.getCategoria() == Categoria.CARTAO)
                && !Texto.vazio(o.getDetalheOculto())) {
            for (int i = 0; i < base.getUtilizadores().size(); i++) {
                Utilizador u = base.getUtilizadores().get(i);
                String doc = u.getDocumentoIdentificacao();
                if (u.getNumero().equalsIgnoreCase(o.getDetalheOculto().trim())
                        || (!Texto.vazio(doc) && doc.equalsIgnoreCase(o.getDetalheOculto().trim()))) {
                    notificacoes.enviar(u, "Documento encontrado",
                            "Foi entregue na " + o.getSector() + " um documento seu (" + o.getCodigo() + ").");
                }
            }
        }
    }

    /** P02, passos 2 a 5: regista a perda e procura logo candidatos (RN07, RN10). */
    public Procura registarPerda(Utilizador dono, Categoria categoria, String descricao, String cor, String marca,
                                 Zona localProvavel, LocalDate perdidoEm, String detalheOculto) {
        if (dono == null || categoria == null || localProvavel == null || perdidoEm == null) {
            throw new RegraNegocioException("Categoria, local provável e data são obrigatórios.");
        }
        if (Texto.vazio(descricao) || descricao.trim().length() < 5) {
            throw new RegraNegocioException("A descrição deve ter pelo menos 5 caracteres.");
        }
        if (perdidoEm.isAfter(relogio.agora().toLocalDate())) {
            throw new RegraNegocioException("A data da perda não pode estar no futuro.");
        }
        Procura p = new Procura(base.proximoIdProcura(), dono, categoria, descricao.trim(), cor, marca,
                localProvavel, perdidoEm, detalheOculto, relogio.agora(), DIAS_VALIDADE_PROCURA);
        base.getProcuras().add(p);
        notificacoes.enviar(dono, "Procura registada",
                "Procura #" + p.getId() + " activa até " + p.getValidaAte() + ". Será avisado se aparecer algo compatível.");
        correspondencia.avisarCandidatos(p);
        return p;
    }
}
