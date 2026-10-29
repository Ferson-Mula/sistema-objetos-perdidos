package repository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import model.Objecto;
import model.Perfil;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;

/**
 * "Base de dados" do sistema: listas em memória que são guardadas num ficheiro.
 * Todos os serviços usam esta classe para ler e guardar informação.
 */
public class BaseDados implements Serializable {

    public final List<Utilizador> utilizadores = new ArrayList<>();
    public final List<Objecto> objectos = new ArrayList<>();
    public final List<Procura> procuras = new ArrayList<>();
    public final List<Reclamacao> reclamacoes = new ArrayList<>();

    private int ultimoObjecto = 0;
    private int ultimaProcura = 0;
    private int ultimaReclamacao = 0;

    /** Se for verdadeiro, as notificações também aparecem no ecrã (útil na demonstração). */
    private transient boolean mostrarNotificacoes = false;

    public void setMostrarNotificacoes(boolean mostrar) {
        this.mostrarNotificacoes = mostrar;
    }

    // ------------------------------------------------------------ utilizadores

    public Utilizador criarUtilizador(String numero, String nome, String email, String documento, String senha,
                                      Perfil perfil, Sector sector) {
        if (utilizador(numero) != null) {
            throw new RegraNegocioException("Já existe um utilizador com o número " + numero + ".");
        }
        if (senha == null || senha.length() < 4) {
            throw new RegraNegocioException("A palavra-passe deve ter pelo menos 4 caracteres.");
        }
        Utilizador u = new Utilizador(numero, nome, email, documento, senha, perfil, sector);
        utilizadores.add(u);
        return u;
    }

    public Utilizador utilizador(String numero) {
        return utilizadores.stream().filter(u -> u.getNumero().equalsIgnoreCase(numero)).findFirst().orElse(null);
    }

    /** Devolve o utilizador se o número e a palavra-passe estiverem certos; senão devolve null. */
    public Utilizador entrar(String numero, String senha) {
        Utilizador u = utilizador(numero);
        return (u != null && u.verificarSenha(senha)) ? u : null;
    }

    // ------------------------------------------------------------ procurar por código ou número

    public Objecto objecto(String codigo) {
        return objectos.stream().filter(o -> o.getCodigo().equalsIgnoreCase(codigo)).findFirst().orElse(null);
    }

    public Reclamacao reclamacao(int id) {
        return reclamacoes.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
    }

    public Procura procura(int id) {
        return procuras.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    // ------------------------------------------------------------ numeração

    /** Códigos do tipo AP-2026-0001 (RN01). */
    public String proximoCodigoObjecto(int ano) {
        ultimoObjecto++;
        return String.format("AP-%d-%04d", ano, ultimoObjecto);
    }

    public int proximoIdProcura() {
        return ++ultimaProcura;
    }

    public int proximoIdReclamacao() {
        return ++ultimaReclamacao;
    }

    // ------------------------------------------------------------ notificações

    /** Guarda a mensagem na caixa do utilizador (num sistema real seria um email ou SMS). */
    public void avisar(Utilizador u, String assunto, String mensagem) {
        u.receber(assunto + ": " + mensagem);
        if (mostrarNotificacoes) {
            System.out.println("      [aviso para " + u.getNome() + "] " + assunto + " - " + mensagem);
        }
    }

    public void avisarFuncionarios(Sector sector, String assunto, String mensagem) {
        utilizadores.stream().filter(u -> u.ehFuncionarioDe(sector)).forEach(u -> avisar(u, assunto, mensagem));
    }

    public void avisarGestores(String assunto, String mensagem) {
        utilizadores.stream().filter(u -> u.getPerfil() == Perfil.GESTOR).forEach(u -> avisar(u, assunto, mensagem));
    }

    // ------------------------------------------------------------ ficheiro

    public void guardar(Path ficheiro) {
        try {
            Files.createDirectories(ficheiro.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(ficheiro))) {
                out.writeObject(this);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível guardar os dados: " + e.getMessage());
        }
    }

    /** Lê os dados guardados; se o ficheiro não existir devolve null. */
    public static BaseDados carregar(Path ficheiro) {
        if (!Files.exists(ficheiro)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(ficheiro))) {
            return (BaseDados) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Ficheiro de dados inválido: " + e.getMessage());
        }
    }
}
