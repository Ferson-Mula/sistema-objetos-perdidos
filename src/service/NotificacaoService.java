package service;

import java.util.ArrayList;
import java.util.List;

import model.Notificacao;
import model.Utilizador;
import repository.BaseDados;
import util.Relogio;

/**
 * Envia notificações (simuladas). O envio é feito logo na chamada: quem chama
 * espera que a notificação seja criada antes de continuar.
 */
public class NotificacaoService {

    private final BaseDados base;
    private final Relogio relogio;
    private boolean mostrarNaConsola;

    public NotificacaoService(BaseDados base, Relogio relogio, boolean mostrarNaConsola) {
        this.base = base;
        this.relogio = relogio;
        this.mostrarNaConsola = mostrarNaConsola;
    }

    public void enviar(Utilizador destinatario, String assunto, String mensagem) {
        Notificacao n = new Notificacao(base.proximoIdNotificacao(), destinatario, assunto, mensagem,
                relogio.agora());
        base.getNotificacoes().add(n);
        if (mostrarNaConsola) {
            System.out.println("   [notificação para " + destinatario.getNome() + "] " + assunto + ": " + mensagem);
        }
    }

    public List<Notificacao> doUtilizador(Utilizador u) {
        List<Notificacao> lista = new ArrayList<Notificacao>();
        for (int i = 0; i < base.getNotificacoes().size(); i++) {
            Notificacao n = base.getNotificacoes().get(i);
            if (n.getDestinatario().getId() == u.getId()) {
                lista.add(n);
            }
        }
        return lista;
    }

    public void marcarTodasLidas(Utilizador u) {
        List<Notificacao> lista = doUtilizador(u);
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).setLida(true);
        }
    }

    public void setMostrarNaConsola(boolean v) {
        this.mostrarNaConsola = v;
    }
}
