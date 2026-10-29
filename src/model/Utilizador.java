package model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Pessoa que usa o sistema: estudante, docente, funcionário de um sector ou gestor.
 * O perfil diz o que a pessoa pode fazer; o sector só é usado pelos funcionários.
 */
public class Utilizador implements Serializable {

    private final String numero;
    private final String nome;
    private final String email;
    private final String documento;
    private final String senhaGuardada;
    private final Perfil perfil;
    private final Sector sector;
    private final List<String> notificacoes = new ArrayList<>();
    private int lidas = 0;

    public Utilizador(String numero, String nome, String email, String documento, String senha,
                      Perfil perfil, Sector sector) {
        this.numero = numero;
        this.nome = nome;
        this.email = email;
        this.documento = documento;
        this.senhaGuardada = esconder(senha);
        this.perfil = perfil;
        this.sector = sector;
    }

    public boolean verificarSenha(String senha) {
        return senhaGuardada.equals(esconder(senha));
    }

    /** Guarda-se só o "resumo" da palavra-passe (SHA-256), nunca a palavra-passe. */
    private static String esconder(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void receber(String mensagem) {
        notificacoes.add(mensagem);
    }

    public List<String> getNotificacoes() {
        return notificacoes;
    }

    public int naoLidas() {
        return notificacoes.size() - lidas;
    }

    public void marcarComoLidas() {
        lidas = notificacoes.size();
    }

    public boolean ehFuncionarioDe(Sector s) {
        return perfil == Perfil.FUNCIONARIO && sector == s;
    }

    public String getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getDocumento() {
        return documento;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public Sector getSector() {
        return sector;
    }

    /** Dois utilizadores são o mesmo se tiverem o mesmo número. */
    @Override
    public boolean equals(Object outro) {
        return outro instanceof Utilizador && ((Utilizador) outro).numero.equals(numero);
    }

    @Override
    public int hashCode() {
        return numero.hashCode();
    }

    @Override
    public String toString() {
        return nome + " (" + perfil + (sector != null ? ", " + sector : "") + ")";
    }
}
