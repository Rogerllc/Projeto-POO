package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResponsavelOng {
    private final String email;
    private final String nome;
    private final String cargo;
    private final String senha;

    public ResponsavelOng(String email, String nome, String cargo, String senha) {
        if (email == null || email.isBlank() || nome == null || nome.isBlank() || senha == null || senha.length() < 4) {
            throw new IllegalArgumentException("Informe e-mail, nome e senha de pelo menos 4 caracteres.");
        }
        this.email = email;
        this.nome = nome;
        this.cargo = cargo;
        this.senha = senha;
    }

    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getCargo() { return cargo; }

    public boolean login(String email, String senha) {
        return this.email.equalsIgnoreCase(email) && this.senha.equals(senha);
    }

    public boolean cadastrar(Banco db) {
        if (Veterinario.isLoginCrmv(email) || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Responsável da ONG deve usar e-mail. Para veterinário, informe o CRMV completo no cadastro da equipe.");
        }
        return db.queryUpdate(String.format("INSERT INTO responsaveis(email, nome, cargo, senha) VALUES ('%s', '%s', '%s', '%s')",
                email.replace("'", "''"), nome.replace("'", "''"), cargo.replace("'", "''"), senha.replace("'", "''"))) > 0;
    }

    public boolean editar(Banco db) {
        return db.queryUpdate(String.format("UPDATE responsaveis SET nome = '%s', cargo = '%s', senha = '%s' WHERE email = '%s'",
                nome.replace("'", "''"), cargo.replace("'", "''"), senha.replace("'", "''"), email.replace("'", "''"))) > 0;
    }

    public static boolean remover(Banco db, String email) {
        return db.queryUpdate(String.format("DELETE FROM responsaveis WHERE email = '%s'", email.replace("'", "''"))) > 0;
    }

    private static ResponsavelOng ler(ResultSet rs) throws SQLException {
        return new ResponsavelOng(rs.getString("email"), rs.getString("nome"), rs.getString("cargo"), rs.getString("senha"));
    }

    public static ResponsavelOng buscarPorEmail(Banco db, String email) {
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM responsaveis WHERE email = '%s'", email.replace("'", "''")))) {
            return rs.next() ? ler(rs) : null;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public static List<ResponsavelOng> listar(Banco db) {
        List<ResponsavelOng> responsaveis = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM responsaveis ORDER BY nome")) {
            while (rs.next()) responsaveis.add(ler(rs));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return responsaveis;
    }
}
