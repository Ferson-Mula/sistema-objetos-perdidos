package service;

import model.Docente;
import model.Estudante;
import model.FuncionarioSector;
import model.Gestor;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import repository.BaseDados;
import util.Texto;

/** Contas e entrada no sistema. Os utilizadores têm número institucional e palavra-passe. */
public class AutenticacaoService {

    private final BaseDados base;

    public AutenticacaoService(BaseDados base) {
        this.base = base;
    }

    public Utilizador entrar(String numero, String senha) {
        if (Texto.vazio(numero) || senha == null) {
            throw new RegraNegocioException("Indique o número e a palavra-passe.");
        }
        Utilizador u = base.utilizadorPorNumero(numero);
        if (u == null || !u.verificarSenha(senha)) {
            throw new RegraNegocioException("Número ou palavra-passe incorrectos.");
        }
        return u;
    }

    public Estudante criarEstudante(String nome, String numero, String email, String documento, String senha) {
        validarNovaConta(nome, numero, senha);
        Estudante e = new Estudante(base.proximoIdUtilizador(), nome, numero, email, documento, senha);
        base.getUtilizadores().add(e);
        return e;
    }

    public Docente criarDocente(String nome, String numero, String email, String documento, String senha) {
        validarNovaConta(nome, numero, senha);
        Docente d = new Docente(base.proximoIdUtilizador(), nome, numero, email, documento, senha);
        base.getUtilizadores().add(d);
        return d;
    }

    public FuncionarioSector criarFuncionario(String nome, String numero, String email, String documento,
                                              String senha, Sector sector) {
        validarNovaConta(nome, numero, senha);
        FuncionarioSector f = new FuncionarioSector(base.proximoIdUtilizador(), nome, numero, email, documento,
                senha, sector);
        base.getUtilizadores().add(f);
        return f;
    }

    public Gestor criarGestor(String nome, String numero, String email, String documento, String senha) {
        validarNovaConta(nome, numero, senha);
        Gestor g = new Gestor(base.proximoIdUtilizador(), nome, numero, email, documento, senha);
        base.getUtilizadores().add(g);
        return g;
    }

    private void validarNovaConta(String nome, String numero, String senha) {
        if (Texto.vazio(nome) || Texto.vazio(numero)) {
            throw new RegraNegocioException("Nome e número são obrigatórios.");
        }
        if (senha == null || senha.length() < 4) {
            throw new RegraNegocioException("A palavra-passe deve ter pelo menos 4 caracteres.");
        }
        if (base.utilizadorPorNumero(numero) != null) {
            throw new RegraNegocioException("Já existe um utilizador com o número " + numero + ".");
        }
    }
}
