package model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Utilizador do sistema. Classe base de Estudante, Docente, FuncionarioSector e Gestor. */
public abstract class Utilizador implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String nome;
    private final String numero;
    private final String email;
    private final String documentoIdentificacao;
    private String senhaHash;

    protected Utilizador(int id, String nome, String numero, String email, String documentoIdentificacao,
                         String senha) {
        this.id = id;
        this.nome = nome;
        this.numero = numero;
        this.email = email;
        this.documentoIdentificacao = documentoIdentificacao;
        this.senhaHash = hash(senha);
    }

    public abstract Perfil getPerfil();

    public boolean verificarSenha(String senha) {
        return senhaHash.equals(hash(senha));
    }

    private static String hash(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getNumero() {
        return numero;
    }

    public String getEmail() {
        return email;
    }

    public String getDocumentoIdentificacao() {
        return documentoIdentificacao;
    }

    @Override
    public String toString() {
        return nome + " (" + getPerfil() + ", n.º " + numero + ")";
    }
}
