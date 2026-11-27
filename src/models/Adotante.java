package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class Adotante {
    private String cpf;
    private String nome;
    private String endereco;
    private String senha;
    private boolean aprovado;

    public Adotante(String cpf, String nome, String endereco, String senha) {
        this.cpf = cpf;
        this.nome = nome;
        this.endereco = endereco;
        this.senha = senha;
        this.aprovado = avaliarPerfil();
    }

    public Adotante(String cpf, String nome, String endereco) {
        this(cpf, nome, endereco, "");
    }

    public boolean avaliarPerfil() {
        if (this.cpf == null || this.nome == null || this.endereco == null || this.endereco.trim().isEmpty()) {
            this.aprovado = false;
            return false;
        }

        if (!this.cpf.matches("\\d{11}")) {
            this.aprovado = false;
            return false;
        }

        if (this.nome.trim().isEmpty() || this.nome.matches(".*\\d.*")) {
            this.aprovado = false;
            return false;
        }

        this.aprovado = true;
        return true;
    }

    public boolean login(String cpfDigitado, String senhaDigitada) {
        if (cpfDigitado == null || senhaDigitada == null || this.senha == null || this.senha.isEmpty()) {
            return false;
        }
        return this.cpf.equals(cpfDigitado) && this.senha.equals(senhaDigitada);
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
        this.avaliarPerfil();
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        this.avaliarPerfil();
    }

    public String getEndereco() {
        return this.endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
        this.avaliarPerfil();
    }

    public String getSenha() {
        return this.senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isAprovado() {
        return this.aprovado;
    }

    @Override
    public String toString() {
        return "Adotante: " + this.nome + " | CPF: " + this.cpf + " | Aprovado: " + this.aprovado;
    }

    public boolean cadastrar(Banco db) {
        if (!avaliarPerfil() || senha == null || senha.length() < 4) {
            throw new IllegalArgumentException("Informe CPF, nome, endereço e senha de pelo menos 4 caracteres.");
        }
        return db.queryUpdate(String.format("INSERT INTO adotantes(cpf, nome, endereco, senha) VALUES ('%s', '%s', '%s', '%s')",
                cpf.replace("'", "''"), nome.replace("'", "''"), endereco.replace("'", "''"), senha.replace("'", "''"))) > 0;
    }

    public boolean editar(Banco db) {
        return db.queryUpdate(String.format("UPDATE adotantes SET nome = '%s', endereco = '%s', senha = '%s' WHERE cpf = '%s'",
                nome.replace("'", "''"), endereco.replace("'", "''"), senha.replace("'", "''"), cpf.replace("'", "''"))) > 0;
    }

    public static boolean remover(Banco db, String cpf) {
        return db.queryUpdate(String.format("DELETE FROM adotantes WHERE cpf = '%s'", cpf.replace("'", "''"))) > 0;
    }

    private static Adotante ler(ResultSet rs) throws SQLException {
        return new Adotante(rs.getString("cpf"), rs.getString("nome"), rs.getString("endereco"), rs.getString("senha"));
    }

    public static Adotante buscarPorCpf(Banco db, String cpf) {
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM adotantes WHERE cpf = '%s'", cpf.replace("'", "''")))) {
            return rs.next() ? ler(rs) : null;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public static List<Adotante> listar(Banco db) {
        List<Adotante> adotantes = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM adotantes ORDER BY nome")) {
            while (rs.next()) adotantes.add(ler(rs));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return adotantes;
    }

}