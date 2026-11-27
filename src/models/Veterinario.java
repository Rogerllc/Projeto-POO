package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class Veterinario {
    private final String crmv;
    private final String nome;
    private final String especialidade;
    private final String senha;

    public Veterinario(String crmv, String nome, String especialidade, String senha) {
        crmv = normalizarCrmv(crmv);
        if (!isCrmvValido(crmv)) {
            throw new IllegalArgumentException("CRMV inválido. Use o formato CRMV-UF número (ex.: CRMV-RN 1234).");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome deve ser informado.");
        }
        if (especialidade == null || especialidade.isBlank()) {
            throw new IllegalArgumentException("A especialidade deve ser informada.");
        }
        if (senha == null || senha.length() < 4) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 4 caracteres.");
        }
        this.crmv = crmv;
        this.nome = nome;
        this.especialidade = especialidade;
        this.senha = senha;
    }

    public static boolean isLoginCrmv(String identificador) {
        return identificador != null && identificador.toUpperCase(java.util.Locale.ROOT).contains("CRMV");
    }

    public static String normalizarCrmv(String crmv) {
        return crmv == null ? null : crmv.trim().toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static Veterinario autenticar(Banco db, String identificador, String senha) {
        String crmv = normalizarCrmv(identificador);
        if (!isCrmvValido(crmv)) return null;
        Veterinario v = buscarPorCrmv(db, crmv);
        if (v != null) return v.login(crmv, senha) ? v : null;

        // Corrige cadastros antigos feitos como responsável usando um CRMV.
        ResponsavelOng antigo = ResponsavelOng.buscarPorEmail(db, identificador.trim());
        if (antigo == null || !antigo.login(identificador.trim(), senha)) return null;
        v = new Veterinario(crmv, antigo.getNome(), antigo.getCargo(), senha);
        db.queryUpdate("BEGIN TRANSACTION");
        try {
            v.cadastrar(db);
            ResponsavelOng.remover(db, antigo.getEmail());
            db.queryUpdate("COMMIT");
            return v;
        } catch (RuntimeException e) {
            db.queryUpdate("ROLLBACK");
            throw e;
        }
    }

    public static boolean isCrmvValido(String crmv) {
        return crmv != null && crmv.matches("CRMV-[A-Z]{2} \\d{1,6}");
    }

    public String getCrmv() {
        return crmv;
    }

    public String getNome() {
        return nome;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public String getSenha() {
        return senha;
    }

    public boolean login(String crm, String senha) {
        return this.crmv.equals(normalizarCrmv(crm)) && this.senha.equals(senha);
    }

    public ConsultaVeterinaria realizarConsulta(ProntuarioMedico prontuario) {
        if (prontuario == null) {
            throw new IllegalArgumentException("O prontuário não pode ser nulo.");
        }
        return new ConsultaVeterinaria(0, prontuario.getId(), crmv);
    }
    
    public void prescrever(ConsultaVeterinaria c, String texto) {
        if (c == null) {
            throw new IllegalArgumentException("A consulta não pode ser nula.");
        }
        if (!crmv.equals(c.getCrmVeterinario())) {
            System.out.println("Erro: esta consulta pertence a outro veterinário.");
            return;
        }
        c.setPrescricao(texto);
        System.out.println("Prescrição registrada com sucesso!");
    }

    @Override
    public String toString() {
        return "Veterinario [crmv='" + crmv + '\'' +
                ", nome='" + nome + '\'' +
                ", especialidade='" + especialidade + '\'' + "]";
    }

    public boolean cadastrar(Banco db) {
        return db.queryUpdate(String.format("INSERT INTO veterinarios(crmv, nome, especialidade, senha) VALUES ('%s', '%s', '%s', '%s')",
                crmv.replace("'", "''"), nome.replace("'", "''"), especialidade.replace("'", "''"), senha.replace("'", "''"))) > 0;
    }

    public boolean editar(Banco db) {
        return db.queryUpdate(String.format("UPDATE veterinarios SET nome = '%s', especialidade = '%s', senha = '%s' WHERE crmv = '%s'",
                nome.replace("'", "''"), especialidade.replace("'", "''"), senha.replace("'", "''"), crmv.replace("'", "''"))) > 0;
    }

    public static boolean remover(Banco db, String crmv) {
        return db.queryUpdate(String.format("DELETE FROM veterinarios WHERE crmv = '%s'", crmv.replace("'", "''"))) > 0;
    }

    private static Veterinario ler(ResultSet rs) throws SQLException {
        return new Veterinario(rs.getString("crmv"), rs.getString("nome"), rs.getString("especialidade"), rs.getString("senha"));
    }

    public static Veterinario buscarPorCrmv(Banco db, String crmv) {
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM veterinarios WHERE crmv = '%s'", normalizarCrmv(crmv).replace("'", "''")))) {
            return rs.next() ? ler(rs) : null;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public static List<Veterinario> listar(Banco db) {
        List<Veterinario> veterinarios = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM veterinarios ORDER BY nome")) {
            while (rs.next()) veterinarios.add(ler(rs));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return veterinarios;
    }

}
