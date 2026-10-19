package model;

/** Gestor do serviço: decide recursos, aprova destinos e consulta relatórios. */
public class Gestor extends Utilizador {

    private static final long serialVersionUID = 1L;

    public Gestor(int id, String nome, String numero, String email, String documento, String senha) {
        super(id, nome, numero, email, documento, senha);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.GESTOR;
    }
}
