package model;

public class Estudante extends Utilizador {

    private static final long serialVersionUID = 1L;

    public Estudante(int id, String nome, String numero, String email, String documento, String senha) {
        super(id, nome, numero, email, documento, senha);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.ESTUDANTE;
    }
}
