package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class Animal {
    public static final String STATUS_DISPONIVEL = "DISPONIVEL";
    public static final String STATUS_ADOTADO = "ADOTADO";

    private int id;
    private final String nome;
    private final String especie;
    private final int idade;
    private String statusAdocao = STATUS_DISPONIVEL;

    public Animal(int id, String nome, String especie, int idade) {
        if (idade < 0) {
            throw new IllegalArgumentException("A idade do animal não pode ser negativa.");
        }
        this.id = id;
        this.nome = nome;
        this.especie = especie;
        this.idade = idade;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEspecie() {
        return especie;
    }

    public int getIdade() {
        return idade;
    }

    public String getStatusAdocao() {
        return statusAdocao;
    }

    public void atualizarStatus(String novoStatus) {
        if (novoStatus == null || novoStatus.isBlank()) {
            throw new IllegalArgumentException("O status de adoção deve ser informado.");
        }
        this.statusAdocao = novoStatus;
    }

    public boolean cadastrar(Banco db) {
        if (nome == null || nome.isBlank() || especie == null || especie.isBlank()) {
            throw new IllegalArgumentException("Informe nome e espécie.");
        }
        db.queryUpdate("BEGIN TRANSACTION");
        try {
            db.queryUpdate(String.format("INSERT INTO animais(nome, especie, idade) VALUES ('%s', '%s', %d)",
                    nome.replace("'", "''"), especie.replace("'", "''"), idade));
            try (ResultSet rs = db.querySearch("SELECT last_insert_rowid()")) {
                if (rs.next()) id = rs.getInt(1);
            }
            ProntuarioMedico.cadastrar(db, id);
            db.queryUpdate("COMMIT");
            return true;
        } catch (SQLException | RuntimeException e) {
            db.queryUpdate("ROLLBACK");
            throw new IllegalStateException("Não foi possível cadastrar o animal: " + e.getMessage(), e);
        }
    }

    public boolean editar(Banco db) {
        return db.queryUpdate(String.format("UPDATE animais SET nome = '%s', especie = '%s', idade = %d WHERE id = %d",
                nome.replace("'", "''"), especie.replace("'", "''"), idade, id)) > 0;
    }

    public boolean salvarStatus(Banco db) {
        return db.queryUpdate(String.format("UPDATE animais SET status = '%s' WHERE id = %d",
                statusAdocao.replace("'", "''"), id)) > 0;
    }

    public static boolean remover(Banco db, int id) {
        return db.queryUpdate(String.format("DELETE FROM animais WHERE id = %d", id)) > 0;
    }

    private static Animal ler(ResultSet rs) throws SQLException {
        Animal a = new Animal(rs.getInt("id"), rs.getString("nome"), rs.getString("especie"), rs.getInt("idade"));
        a.atualizarStatus(rs.getString("status"));
        return a;
    }

    public static Animal buscarPorId(Banco db, int id) {
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM animais WHERE id = %d", id))) {
            return rs.next() ? ler(rs) : null;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public static List<Animal> listar(Banco db) {
        List<Animal> animais = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM animais ORDER BY id")) {
            while (rs.next()) animais.add(ler(rs));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return animais;
    }

}
