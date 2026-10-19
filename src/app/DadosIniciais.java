package app;

import model.Sector;

/** Contas de demonstração (nomes fictícios). Palavra-passe de todas: 1234. */
public final class DadosIniciais {

    public static final String SENHA = "1234";

    private DadosIniciais() {
    }

    public static void popular(Sistema s) {
        s.autenticacao.criarEstudante("Ana Macie", "20230001", "ana.macie@ujc.ac.mz", "110100200300A", SENHA);
        s.autenticacao.criarEstudante("Bruno Tembe", "20230002", "bruno.tembe@ujc.ac.mz", "110100200301B", SENHA);
        s.autenticacao.criarEstudante("Carla Nhaca", "20230003", "carla.nhaca@ujc.ac.mz", "110100200302C", SENHA);
        s.autenticacao.criarDocente("Dr. Daniel Cuamba", "D001", "daniel.cuamba@ujc.ac.mz", "110100200303D", SENHA);
        s.autenticacao.criarFuncionario("Marta Sitoe", "F001", "marta.sitoe@ujc.ac.mz", "110100200304E", SENHA,
                Sector.SECRETARIA);
        s.autenticacao.criarFuncionario("Carlos Mondlane", "F002", "carlos.mondlane@ujc.ac.mz", "110100200305F",
                SENHA, Sector.BIBLIOTECA);
        s.autenticacao.criarGestor("Direcção de Serviços", "G001", "servicos@ujc.ac.mz", "110100200306G", SENHA);
    }
}
