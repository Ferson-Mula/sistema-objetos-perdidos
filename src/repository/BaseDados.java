package repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Notificacao;
import model.Objecto;
import model.Procura;
import model.Reclamacao;
import model.Utilizador;

/**
 * Todos os dados do sistema em memória (listas). É gravada e carregada
 * por inteiro pela classe Persistencia.
 */
public class BaseDados implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Utilizador> utilizadores = new ArrayList<Utilizador>();
    private final List<Objecto> objectos = new ArrayList<Objecto>();
    private final List<Procura> procuras = new ArrayList<Procura>();
    private final List<Reclamacao> reclamacoes = new ArrayList<Reclamacao>();
    private final List<Notificacao> notificacoes = new ArrayList<Notificacao>();
    /** Pares "idProcura:codigoObjecto" já notificados, para não repetir avisos. */
    private final Set<String> avisosCorrespondencia = new HashSet<String>();

    private int seqUtilizador = 0;
    private int seqObjecto = 0;
    private int seqProcura = 0;
    private int seqReclamacao = 0;
    private int seqNotificacao = 0;

    public int proximoIdUtilizador() {
        return ++seqUtilizador;
    }

    public String proximoCodigoObjecto(int ano) {
        seqObjecto++;
        return "OBJ-" + ano + "-" + String.format("%04d", seqObjecto);
    }

    public int proximoIdProcura() {
        return ++seqProcura;
    }

    public int proximoIdReclamacao() {
        return ++seqReclamacao;
    }

    public int proximoIdNotificacao() {
        return ++seqNotificacao;
    }

    public List<Utilizador> getUtilizadores() {
        return utilizadores;
    }

    public List<Objecto> getObjectos() {
        return objectos;
    }

    public List<Procura> getProcuras() {
        return procuras;
    }

    public List<Reclamacao> getReclamacoes() {
        return reclamacoes;
    }

    public List<Notificacao> getNotificacoes() {
        return notificacoes;
    }

    public Set<String> getAvisosCorrespondencia() {
        return avisosCorrespondencia;
    }

    // ---- pesquisas simples (percorrem as listas)

    public Objecto objectoPorCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (int i = 0; i < objectos.size(); i++) {
            if (objectos.get(i).getCodigo().equalsIgnoreCase(codigo.trim())) {
                return objectos.get(i);
            }
        }
        return null;
    }

    public Procura procuraPorId(int id) {
        for (int i = 0; i < procuras.size(); i++) {
            if (procuras.get(i).getId() == id) {
                return procuras.get(i);
            }
        }
        return null;
    }

    public Reclamacao reclamacaoPorId(int id) {
        for (int i = 0; i < reclamacoes.size(); i++) {
            if (reclamacoes.get(i).getId() == id) {
                return reclamacoes.get(i);
            }
        }
        return null;
    }

    public Utilizador utilizadorPorNumero(String numero) {
        for (int i = 0; i < utilizadores.size(); i++) {
            if (utilizadores.get(i).getNumero().equalsIgnoreCase(numero.trim())) {
                return utilizadores.get(i);
            }
        }
        return null;
    }
}
