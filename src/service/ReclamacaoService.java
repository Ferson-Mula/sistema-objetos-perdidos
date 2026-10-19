package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import model.EstadoObjecto;
import model.EstadoProcura;
import model.EstadoReclamacao;
import model.FuncionarioSector;
import model.Gestor;
import model.Objecto;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import repository.BaseDados;
import util.Datas;
import util.Relogio;
import util.Texto;

/** P03: reclamação, verificação e devolução (RN11 a RN16). */
public class ReclamacaoService {

    public static final double LIMIAR_VERIFICACAO = 0.60;
    public static final int HORAS_PROVA_ADICIONAL = 48;
    public static final int DIAS_UTEIS_LEVANTAMENTO = 5;

    private final BaseDados base;
    private final Relogio relogio;
    private final NotificacaoService notificacoes;

    public ReclamacaoService(BaseDados base, Relogio relogio, NotificacaoService notificacoes) {
        this.base = base;
        this.relogio = relogio;
        this.notificacoes = notificacoes;
    }

    /**
     * P03, passos 1 a 4. O reclamante responde ao desafio (detalhe oculto) e junta provas.
     * Se a pontuação chegar ao limiar, o objecto fica RESERVADO para ele.
     */
    public Reclamacao submeter(Utilizador reclamante, String codigo, String respostaDetalhe, String provas,
                               Procura procura) {
        Objecto o = base.objectoPorCodigo(codigo);
        if (o == null) {
            throw new RegraNegocioException("Não existe nenhum objecto com o código " + codigo + ".");
        }
        if (o.getEstado() == EstadoObjecto.RESERVADO) {
            // Sem fila de espera: quem chega depois é simplesmente recusado.
            throw new RegraNegocioException("O objecto " + o.getCodigo()
                    + " já está em verificação por outro reclamante. Tente novamente mais tarde.");
        }
        if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
            throw new RegraNegocioException("O objecto " + o.getCodigo() + " não está disponível para reclamação.");
        }
        if (o.getAchador().getId() == reclamante.getId()) {
            throw new RegraNegocioException("Quem encontrou o objecto não o pode reclamar.");
        }
        if (Texto.vazio(respostaDetalhe)) {
            throw new RegraNegocioException("Responda à pergunta de verificação (RN11).");
        }
        if (procura != null && procura.getDono().getId() != reclamante.getId()) {
            throw new RegraNegocioException("A procura indicada pertence a outro utilizador.");
        }
        for (int i = 0; i < base.getReclamacoes().size(); i++) {
            Reclamacao r = base.getReclamacoes().get(i);
            if (r.getObjecto() == o && r.getReclamante().getId() == reclamante.getId() && estaActiva(r)) {
                throw new RegraNegocioException("Já tem uma reclamação activa para este objecto (#" + r.getId() + ").");
            }
        }

        LocalDateTime agora = relogio.agora();
        Reclamacao r = new Reclamacao(base.proximoIdReclamacao(), o, procura, reclamante, respostaDetalhe.trim(),
                provas, agora);
        r.setPontuacao(pontuarVerificacao(o, respostaDetalhe, provas, procura));
        base.getReclamacoes().add(r);

        if (r.getPontuacao() >= LIMIAR_VERIFICACAO) {
            enviarParaVerificacao(r, o);
        } else {
            r.setEstado(EstadoReclamacao.PROVA_ADICIONAL);
            r.setPrazoResposta(agora.plusHours(HORAS_PROVA_ADICIONAL));
            notificacoes.enviar(reclamante, "Prova adicional necessária",
                    "A reclamação #" + r.getId() + " não reuniu provas suficientes. Tem " + HORAS_PROVA_ADICIONAL
                            + " horas para acrescentar mais informação.");
        }
        return r;
    }

    private void enviarParaVerificacao(Reclamacao r, Objecto o) {
        r.setEstado(EstadoReclamacao.EM_VERIFICACAO);
        o.mudarEstado(EstadoObjecto.RESERVADO, r.getReclamante().getNome(), "Reserva por reclamação",
                "Reclamação #" + r.getId(), relogio.agora());
        notificarFuncionarios(o.getSector(), "Reclamação para rever",
                "Reclamação #" + r.getId() + " do objecto " + o.getCodigo() + " aguarda verificação.");
        notificacoes.enviar(r.getReclamante(), "Reclamação em verificação",
                "A reclamação #" + r.getId() + " será revista pelo funcionário da " + o.getSector() + ".");
    }

    /** Segunda (e última) oportunidade para juntar provas. */
    public Reclamacao completarProva(Utilizador reclamante, int id, String novaResposta, String novasProvas) {
        Reclamacao r = obter(id);
        if (r.getReclamante().getId() != reclamante.getId()) {
            throw new RegraNegocioException("Esta reclamação pertence a outro utilizador.");
        }
        if (r.getEstado() != EstadoReclamacao.PROVA_ADICIONAL) {
            throw new RegraNegocioException("A reclamação #" + id + " não está à espera de provas.");
        }
        if (relogio.agora().isAfter(r.getPrazoResposta())) {
            throw new RegraNegocioException("O prazo de 48 horas para acrescentar provas terminou.");
        }
        Objecto o = r.getObjecto();
        if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
            throw new RegraNegocioException("O objecto " + o.getCodigo() + " já não está disponível.");
        }
        r.incrementarTentativas();
        r.setRespostaDetalhe((r.getRespostaDetalhe() + " " + (novaResposta == null ? "" : novaResposta)).trim());
        if (!Texto.vazio(novasProvas)) {
            r.setProvas(novasProvas);
        }
        r.setPontuacao(pontuarVerificacao(o, r.getRespostaDetalhe(), r.getProvas(), r.getProcura()));
        if (r.getPontuacao() >= LIMIAR_VERIFICACAO) {
            enviarParaVerificacao(r, o);
        } else {
            r.setEstado(EstadoReclamacao.RECUSADA);
            r.setMotivoDecisao("Verificação insuficiente após prova adicional.");
            notificacoes.enviar(reclamante, "Reclamação recusada", "Reclamação #" + id + ": " + r.getMotivoDecisao());
        }
        return r;
    }

    /** Pontuação de verificação (0 a 1): detalhe oculto (até 0,7) + provas (0,2) + procura registada (0,1). */
    public double pontuarVerificacao(Objecto o, String resposta, String provas, Procura procura) {
        double pontos;
        if (Texto.vazio(o.getDetalheOculto())) {
            pontos = (resposta != null && resposta.trim().length() >= 15) ? 0.4 : 0;
        } else {
            pontos = Texto.cobertura(o.getDetalheOculto(), resposta) * 0.7;
        }
        if (!Texto.vazio(provas)) {
            pontos += 0.2;
        }
        if (procura != null && procura.getCategoria() == o.getCategoria()
                && procura.getEstado() == EstadoProcura.ACTIVA) {
            pontos += 0.1;
        }
        return Math.round(pontos * 100.0) / 100.0;
    }

    /** P03, passo 5: o funcionário aprova ou recusa. Aprovar exige identidade conferida (RN11). */
    public Reclamacao revisar(FuncionarioSector f, int id, boolean aprovar, boolean identidadeConferida,
                              String motivo) {
        Reclamacao r = obter(id);
        Objecto o = r.getObjecto();
        if (r.getEstado() != EstadoReclamacao.EM_VERIFICACAO) {
            throw new RegraNegocioException("A reclamação #" + id + " não está em verificação (estado "
                    + r.getEstado() + ").");
        }
        if (o.getSector() != f.getSector()) {
            throw new RegraNegocioException("Esta reclamação pertence à " + o.getSector() + ".");
        }
        if (aprovar) {
            if (!identidadeConferida) {
                throw new RegraNegocioException("Só pode aprovar depois de conferir a identidade do reclamante (RN11).");
            }
            aprovarReclamacao(r, f.getNome());
        } else {
            if (Texto.vazio(motivo)) {
                throw new RegraNegocioException("Indique o motivo da recusa (RN16).");
            }
            r.setEstado(EstadoReclamacao.RECUSADA);
            r.setMotivoDecisao(motivo.trim());
            o.mudarEstado(EstadoObjecto.DISPONIVEL, f.getNome(), "Reclamação recusada", motivo.trim(),
                    relogio.agora());
            notificacoes.enviar(r.getReclamante(), "Reclamação recusada",
                    "Reclamação #" + id + " recusada: " + motivo.trim() + ". Pode pedir recurso ao Gestor.");
        }
        return r;
    }

    private void aprovarReclamacao(Reclamacao r, String autor) {
        LocalDate hoje = relogio.agora().toLocalDate();
        r.setEstado(EstadoReclamacao.APROVADA);
        r.setPrazoLevantamento(Datas.somarDiasUteis(hoje, DIAS_UTEIS_LEVANTAMENTO));
        notificacoes.enviar(r.getReclamante(), "Reclamação aprovada",
                "Levante o objecto " + r.getObjecto().getCodigo() + " na " + r.getObjecto().getSector()
                        + " até " + r.getPrazoLevantamento() + ", com o seu documento de identificação.");
    }

    /** Depois de uma recusa, o reclamante pode pedir recurso uma única vez. */
    public Reclamacao pedirRecurso(Utilizador reclamante, int id, String justificacao) {
        Reclamacao r = obter(id);
        if (r.getReclamante().getId() != reclamante.getId()) {
            throw new RegraNegocioException("Esta reclamação pertence a outro utilizador.");
        }
        if (r.getEstado() != EstadoReclamacao.RECUSADA || r.isRecursoUsado()) {
            throw new RegraNegocioException("Só é possível pedir recurso de uma reclamação recusada, uma vez.");
        }
        if (Texto.vazio(justificacao)) {
            throw new RegraNegocioException("Escreva a justificação do recurso.");
        }
        Objecto o = r.getObjecto();
        if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
            throw new RegraNegocioException("O objecto " + o.getCodigo() + " já não está disponível para recurso.");
        }
        o.mudarEstado(EstadoObjecto.RESERVADO, reclamante.getNome(), "Reserva por recurso", "Reclamação #" + id,
                relogio.agora());
        r.setEstado(EstadoReclamacao.EM_RECURSO);
        r.setRespostaDetalhe(r.getRespostaDetalhe() + " | Recurso: " + justificacao.trim());
        notificarGestores("Recurso para decidir", "Reclamação #" + id + " do objecto " + o.getCodigo() + ".");
        return r;
    }

    /** O Gestor decide o recurso, com motivo registado (RN14). */
    public Reclamacao decidirRecurso(Gestor g, int id, boolean aprovar, String motivo) {
        Reclamacao r = obter(id);
        if (r.getEstado() != EstadoReclamacao.EM_RECURSO) {
            throw new RegraNegocioException("A reclamação #" + id + " não está em recurso.");
        }
        if (Texto.vazio(motivo)) {
            throw new RegraNegocioException("Indique o motivo da decisão (RN14).");
        }
        r.setRecursoUsado(true);
        r.setMotivoDecisao(motivo.trim());
        if (aprovar) {
            aprovarReclamacao(r, g.getNome());
        } else {
            r.setEstado(EstadoReclamacao.RECUSADA);
            r.getObjecto().mudarEstado(EstadoObjecto.DISPONIVEL, g.getNome(), "Recurso recusado", motivo.trim(),
                    relogio.agora());
            notificacoes.enviar(r.getReclamante(), "Recurso recusado", "Reclamação #" + id + ": " + motivo.trim());
        }
        return r;
    }

    /** P03, passos 8 e 9: levantamento presencial. Fica registado quem entregou e a quem (RN15). */
    public Reclamacao entregar(FuncionarioSector f, int id, boolean documentoConferido) {
        Reclamacao r = obter(id);
        Objecto o = r.getObjecto();
        if (r.getEstado() != EstadoReclamacao.APROVADA) {
            throw new RegraNegocioException("A reclamação #" + id + " não está aprovada (estado " + r.getEstado() + ").");
        }
        if (o.getSector() != f.getSector()) {
            throw new RegraNegocioException("O objecto está na " + o.getSector() + ".");
        }
        if (!documentoConferido) {
            throw new RegraNegocioException("Confira o documento de identificação antes de entregar (RN11).");
        }
        if (relogio.agora().toLocalDate().isAfter(r.getPrazoLevantamento())) {
            throw new RegraNegocioException("O prazo de levantamento terminou. Execute a rotina de prazos.");
        }
        o.mudarEstado(EstadoObjecto.DEVOLVIDO, f.getNome(), "Entrega ao dono",
                "Entregue a " + r.getReclamante().getNome() + " (documento "
                        + r.getReclamante().getDocumentoIdentificacao() + " conferido)", relogio.agora());
        r.setEstado(EstadoReclamacao.CONCLUIDA);
        if (r.getProcura() != null) {
            r.getProcura().setEstado(EstadoProcura.ENCERRADA);
        }
        notificacoes.enviar(r.getReclamante(), "Objecto devolvido",
                "Objecto " + o.getCodigo() + " entregue. Procura encerrada.");
        notificacoes.enviar(o.getAchador(), "Obrigado!", "O objecto " + o.getCodigo() + " foi devolvido ao dono.");
        return r;
    }

    // ---- consultas

    public Reclamacao obter(int id) {
        Reclamacao r = base.reclamacaoPorId(id);
        if (r == null) {
            throw new RegraNegocioException("Não existe a reclamação #" + id + ".");
        }
        return r;
    }

    public List<Reclamacao> doSectorNoEstado(Sector sector, EstadoReclamacao estado) {
        List<Reclamacao> lista = new ArrayList<Reclamacao>();
        for (int i = 0; i < base.getReclamacoes().size(); i++) {
            Reclamacao r = base.getReclamacoes().get(i);
            if (r.getObjecto().getSector() == sector && r.getEstado() == estado) {
                lista.add(r);
            }
        }
        return lista;
    }

    public List<Reclamacao> noEstado(EstadoReclamacao estado) {
        List<Reclamacao> lista = new ArrayList<Reclamacao>();
        for (int i = 0; i < base.getReclamacoes().size(); i++) {
            if (base.getReclamacoes().get(i).getEstado() == estado) {
                lista.add(base.getReclamacoes().get(i));
            }
        }
        return lista;
    }

    public List<Reclamacao> doUtilizador(Utilizador u) {
        List<Reclamacao> lista = new ArrayList<Reclamacao>();
        for (int i = 0; i < base.getReclamacoes().size(); i++) {
            if (base.getReclamacoes().get(i).getReclamante().getId() == u.getId()) {
                lista.add(base.getReclamacoes().get(i));
            }
        }
        return lista;
    }

    private boolean estaActiva(Reclamacao r) {
        EstadoReclamacao e = r.getEstado();
        return e == EstadoReclamacao.PROVA_ADICIONAL || e == EstadoReclamacao.EM_VERIFICACAO
                || e == EstadoReclamacao.APROVADA || e == EstadoReclamacao.EM_RECURSO;
    }

    private void notificarFuncionarios(Sector sector, String assunto, String mensagem) {
        for (int i = 0; i < base.getUtilizadores().size(); i++) {
            Utilizador u = base.getUtilizadores().get(i);
            if (u instanceof FuncionarioSector && ((FuncionarioSector) u).getSector() == sector) {
                notificacoes.enviar(u, assunto, mensagem);
            }
        }
    }

    private void notificarGestores(String assunto, String mensagem) {
        for (int i = 0; i < base.getUtilizadores().size(); i++) {
            Utilizador u = base.getUtilizadores().get(i);
            if (u instanceof Gestor) {
                notificacoes.enviar(u, assunto, mensagem);
            }
        }
    }
}
