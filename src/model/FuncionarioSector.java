package model;

/** Funcionário da secretaria ou da biblioteca: recebe, confere e entrega objectos. */
public class FuncionarioSector extends Utilizador {

    private static final long serialVersionUID = 1L;

    private final Sector sector;

    public FuncionarioSector(int id, String nome, String numero, String email, String documento,
                             String senha, Sector sector) {
        super(id, nome, numero, email, documento, senha);
        this.sector = sector;
    }

    public Sector getSector() {
        return sector;
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.FUNCIONARIO;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + sector;
    }
}
