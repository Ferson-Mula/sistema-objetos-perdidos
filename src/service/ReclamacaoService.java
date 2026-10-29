package service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import funcional.Consultas;
import funcional.Texto;
import model.EstadoObjecto;
import model.EstadoReclamacao;
import model.Objecto;
import model.Perfil;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Utilizador;
import repository.BaseDados;

/** P03: reclamação, verificação, recurso e entrega (RN11 a RN16). */
public class ReclamacaoService {

    public static final int HORAS_PROVA_ADICIONAL = 48;
    public static final int DIAS_UTEIS_LEVANTAMENTO = 5;

    private final BaseDados base;
    private final Relogio relogio;

    public ReclamacaoService(BaseDados base, Relogio relogio) {
        this.base = base;
        this.relogio = relogio;
    }

    /**
     * O reclamante responde à pergunta sobre o detalhe oculto e junta provas.
     * Se a prova for suficiente (regras), o objecto fica RESERVADO e o funcionário é avisado.
     * Toda a verificação e a reserva acontecem dentro do mesmo "synchronized":
     * assim, dois reclamantes ao mesmo tempo nunca reservam o mesmo objecto (RN12).
     */
    public Reclamacao submeter(Utilizador reclamante, String codigo, String resposta, String provas,
                               Procura procura) {
        synchronized (base) {
            Objecto o = base.objecto(codigo);
            if (o == null) {
                throw new RegraNegocioException("Não existe nenhum objecto com o código " + codigo + ".");
            }
            if (o.getEstado() == EstadoObjecto.RESERVADO) {
                throw new RegraNegocioException("O objecto " + codigo
                        + " já está em verificação por outro reclamante. Tente mais tarde.");
            }
            if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
                throw new RegraNegocioException("O objecto " + codigo + " não está disponível para reclamação.");
            }
            if (o.getAchador().equals(reclamante)) {
                throw new RegraNegocioException("Quem encontrou o objecto não o pode reclamar.");
            }
            if (Texto.vazio(resposta)) {
                throw new RegraNegocioException("Responda à pergunta de verificação (RN11).");
            }
            if (procura != null && !procura.getDono().equals(reclamante)) {
                throw new RegraNegocioException("A procura indicada pertence a outro utilizador.");
            }
            boolean jaTem = base.reclamacoes.stream().anyMatch(r -> r.getObjecto() == o
                    && r.getReclamante().equals(reclamante) && r.estaActiva());
            if (jaTem) {
                throw new RegraNegocioException("Já tem uma reclamação activa para este objecto.");
            }

            Reclamacao r = new Reclamacao(base.proximoIdReclamacao(), o, procura, reclamante, resposta.trim(), provas);
            base.reclamacoes.add(r);
            avaliar(r);
            return r;
        }
    }

    /** Pede à parte funcional+lógica se a prova chega e actualiza o estado da reclamação. */
    private void avaliar(Reclamacao r) {
        Objecto o = r.getObjecto();
        if (Consultas.provaSuficiente(r)) {
            r.setEstado(EstadoReclamacao.EM_VERIFICACAO);
            o.mudarEstado(EstadoObjecto.RESERVADO, r.getReclamante().getNome(), "Reserva por reclamação",
                    relogio.agora());
            base.avisarFuncionarios(o.getSector(), "Reclamação para rever",
                    "Reclamação #" + r.getId() + " do objecto " + o.getCodigo() + " aguarda verificação.");
            base.avisar(r.getReclamante(), "Reclamação em verificação",
                    "A reclamação #" + r.getId() + " será revista pelo funcionário da " + o.getSector() + ".");
        } else if (r.getTentativas() == 1) {
            r.setEstado(EstadoReclamacao.PROVA_ADICIONAL);
            r.setPrazoResposta(relogio.agora().plusHours(HORAS_PROVA_ADICIONAL));
            base.avisar(r.getReclamante(), "Prova adicional necessária", "A reclamação #" + r.getId()
                    + " não reuniu prova suficiente. Tem " + HORAS_PROVA_ADICIONAL + " horas para acrescentar mais.");
        } else {
            recusar(r, "Verificação insuficiente após prova adicional.");
        }
    }

    /** Segunda (e última) oportunidade para juntar provas. */
    public Reclamacao completarProva(Utilizador reclamante, int id, String novaResposta, String novasProvas) {
        synchronized (base) {
            Reclamacao r = obter(id);
            if (!r.getReclamante().equals(reclamante)) {
                throw new RegraNegocioException("Esta reclamação pertence a outro utilizador.");
            }
            if (r.getEstado() != EstadoReclamacao.PROVA_ADICIONAL) {
                throw new RegraNegocioException("A reclamação #" + id + " não está à espera de provas.");
            }
            if (relogio.agora().isAfter(r.getPrazoResposta())) {
                throw new RegraNegocioException("O prazo de " + HORAS_PROVA_ADICIONAL + " horas terminou.");
            }
            if (r.getObjecto().getEstado() != EstadoObjecto.DISPONIVEL) {
                throw new RegraNegocioException("O objecto " + r.getObjecto().getCodigo() + " já não está disponível.");
            }
            r.maisUmaTentativa();
            r.setResposta((r.getResposta() + " " + (novaResposta == null ? "" : novaResposta)).trim());
            if (!Texto.vazio(novasProvas)) {
                r.setProvas(novasProvas);
            }
            avaliar(r);
            return r;
        }
    }

    /** O funcionário aprova ou recusa. Aprovar exige ter conferido a identidade (RN11). */
    public Reclamacao revisar(Utilizador funcionario, int id, boolean aprovar, boolean identidadeConferida,
                              String motivo) {
        synchronized (base) {
            Reclamacao r = obter(id);
            Objecto o = r.getObjecto();
            if (!funcionario.ehFuncionarioDe(o.getSector())) {
                throw new RegraNegocioException("Esta reclamação pertence à " + o.getSector() + ".");
            }
            if (r.getEstado() != EstadoReclamacao.EM_VERIFICACAO) {
                throw new RegraNegocioException("A reclamação #" + id + " não está em verificação.");
            }
            if (aprovar) {
                if (!identidadeConferida) {
                    throw new RegraNegocioException("Confira a identidade do reclamante antes de aprovar (RN11).");
                }
                aprovarReclamacao(r);
            } else {
                if (Texto.vazio(motivo)) {
                    throw new RegraNegocioException("Indique o motivo da recusa (RN16).");
                }
                o.mudarEstado(EstadoObjecto.DISPONIVEL, funcionario.getNome(), "Reclamação recusada", relogio.agora());
                recusar(r, motivo.trim());
            }
            return r;
        }
    }

    private void aprovarReclamacao(Reclamacao r) {
        LocalDate hoje = relogio.agora().toLocalDate();
        r.setEstado(EstadoReclamacao.APROVADA);
        r.setPrazoLevantamento(Consultas.somarDiasUteis(hoje, DIAS_UTEIS_LEVANTAMENTO));
        base.avisar(r.getReclamante(), "Reclamação aprovada", "Levante o objecto " + r.getObjecto().getCodigo()
                + " na " + r.getObjecto().getSector() + " até " + r.getPrazoLevantamento()
                + ", com o seu documento de identificação.");
    }

    private void recusar(Reclamacao r, String motivo) {
        r.setEstado(EstadoReclamacao.RECUSADA);
        r.setMotivo(motivo);
        base.avisar(r.getReclamante(), "Reclamação recusada", "Reclamação #" + r.getId() + ": " + motivo
                + (r.isRecursoUsado() ? "" : " Pode pedir recurso ao Gestor."));
    }

    /** Depois de uma recusa, o reclamante pode pedir recurso uma única vez. */
    public Reclamacao pedirRecurso(Utilizador reclamante, int id, String justificacao) {
        synchronized (base) {
            Reclamacao r = obter(id);
            if (!r.getReclamante().equals(reclamante)) {
                throw new RegraNegocioException("Esta reclamação pertence a outro utilizador.");
            }
            if (r.getEstado() != EstadoReclamacao.RECUSADA || r.isRecursoUsado()) {
                throw new RegraNegocioException("Só se pode pedir recurso de uma reclamação recusada, uma vez.");
            }
            if (Texto.vazio(justificacao)) {
                throw new RegraNegocioException("Escreva a justificação do recurso.");
            }
            Objecto o = r.getObjecto();
            if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
                throw new RegraNegocioException("O objecto " + o.getCodigo() + " já não está disponível.");
            }
            o.mudarEstado(EstadoObjecto.RESERVADO, reclamante.getNome(), "Reserva por recurso", relogio.agora());
            r.setEstado(EstadoReclamacao.EM_RECURSO);
            r.setResposta(r.getResposta() + " | Recurso: " + justificacao.trim());
            base.avisarGestores("Recurso para decidir", "Reclamação #" + id + " do objecto " + o.getCodigo() + ".");
            return r;
        }
    }

    /** O Gestor decide o recurso e tem de escrever o motivo (RN14). */
    public Reclamacao decidirRecurso(Utilizador gestor, int id, boolean aprovar, String motivo) {
        synchronized (base) {
            if (gestor.getPerfil() != Perfil.GESTOR) {
                throw new RegraNegocioException("Só o Gestor pode decidir recursos.");
            }
            Reclamacao r = obter(id);
            if (r.getEstado() != EstadoReclamacao.EM_RECURSO) {
                throw new RegraNegocioException("A reclamação #" + id + " não está em recurso.");
            }
            if (Texto.vazio(motivo)) {
                throw new RegraNegocioException("Indique o motivo da decisão (RN14).");
            }
            r.setRecursoUsado(true);
            r.setMotivo(motivo.trim());
            if (aprovar) {
                aprovarReclamacao(r);
            } else {
                r.getObjecto().mudarEstado(EstadoObjecto.DISPONIVEL, gestor.getNome(), "Recurso recusado",
                        relogio.agora());
                recusar(r, motivo.trim());
            }
            return r;
        }
    }

    /** Levantamento presencial: fica registado quem entregou e a quem (RN15). */
    public Reclamacao entregar(Utilizador funcionario, int id, boolean documentoConferido) {
        synchronized (base) {
            Reclamacao r = obter(id);
            Objecto o = r.getObjecto();
            if (!funcionario.ehFuncionarioDe(o.getSector())) {
                throw new RegraNegocioException("O objecto está na " + o.getSector() + ".");
            }
            if (r.getEstado() != EstadoReclamacao.APROVADA) {
                throw new RegraNegocioException("A reclamação #" + id + " não está aprovada.");
            }
            if (!documentoConferido) {
                throw new RegraNegocioException("Confira o documento de identificação antes de entregar (RN11).");
            }
            LocalDateTime agora = relogio.agora();
            if (agora.toLocalDate().isAfter(r.getPrazoLevantamento())) {
                throw new RegraNegocioException("O prazo de levantamento terminou.");
            }
            o.mudarEstado(EstadoObjecto.DEVOLVIDO, funcionario.getNome(),
                    "Entregue a " + r.getReclamante().getNome() + " (documento "
                            + r.getReclamante().getDocumento() + " conferido)", agora);
            r.setEstado(EstadoReclamacao.CONCLUIDA);
            if (r.getProcura() != null) {
                r.getProcura().encerrar();
            }
            base.avisar(r.getReclamante(), "Objecto devolvido", "Objecto " + o.getCodigo() + " entregue.");
            base.avisar(o.getAchador(), "Obrigado!", "O objecto " + o.getCodigo() + " foi devolvido ao dono.");
            return r;
        }
    }

    public Reclamacao obter(int id) {
        Reclamacao r = base.reclamacao(id);
        if (r == null) {
            throw new RegraNegocioException("Não existe a reclamação #" + id + ".");
        }
        return r;
    }
}
