package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Banco {
    private final Connection db;
    private final Statement statement;

    public Banco() { this("sistema.db"); }

    public Banco(String arquivo) {
        try {
            db = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
            statement = db.createStatement();
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao conectar ao banco: " + e.getMessage(), e);
        }
    }

    public int queryUpdate(String sql) {
        try {
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco: " + e.getMessage(), e);
        }
    }

    public ResultSet querySearch(String sql) {
        try {
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro na consulta: " + e.getMessage(), e);
        }
    }

    public void desconectar() {
        try {
            try { statement.close(); } finally { db.close(); }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao desconectar: " + e.getMessage(), e);
        }
    }
}
