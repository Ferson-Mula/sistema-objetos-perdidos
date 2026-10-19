package repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Grava e carrega a BaseDados num único ficheiro (serialização Java).
 * Solução simples para a fase 1: não há base de dados relacional.
 */
public final class Persistencia {

    private Persistencia() {
    }

    public static void gravar(BaseDados base, String caminho) {
        try {
            File ficheiro = new File(caminho);
            if (ficheiro.getParentFile() != null) {
                ficheiro.getParentFile().mkdirs();
            }
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ficheiro));
            try {
                out.writeObject(base);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            System.out.println("Aviso: não foi possível gravar os dados (" + e.getMessage() + ")");
        }
    }

    /** Devolve null se o ficheiro não existir ou não puder ser lido. */
    public static BaseDados carregar(String caminho) {
        File ficheiro = new File(caminho);
        if (!ficheiro.exists()) {
            return null;
        }
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(ficheiro));
            try {
                return (BaseDados) in.readObject();
            } finally {
                in.close();
            }
        } catch (Exception e) {
            System.out.println("Aviso: não foi possível carregar os dados (" + e.getMessage() + ")");
            return null;
        }
    }
}
