package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class ConsultaVeterinaria {
    private int id;
    private final int idProntuario;
    private final String crmVeterinario;
    private final LocalDate data;
    private String diagnostico;
    private String prescricao;

    public ConsultaVeterinaria(int id, int idProntuario, String crmVeterinario) {
        this(id, idProntuario, crmVeterinario, LocalDate.now());
    }

    public ConsultaVeterinaria(int id, int idProntuario, String crmVeterinario, LocalDate data) {
        if (!Veterinario.isCrmvValido(crmVeterinario)) {
            throw new IllegalArgumentException("CRMV inválido. Use o formato CRMV-UF número (ex.: CRMV-RN 1234).");
        }
        if (id < 0 || idProntuario <= 0) {
            throw new IllegalArgumentException("O id da consulta não pode ser negativo e o id do prontuário deve ser positivo.");
        }
        this.id = id;
        this.idProntuario = idProntuario;
        this.crmVeterinario = crmVeterinario;
        this.data = data;
        this.diagnostico = "";
        this.prescricao = "";
    }

    public int getId() {
        return id;
    }

    public int getIdProntuario() {
        return idProntuario;
    }

    public String getCrmVeterinario() {
        return crmVeterinario;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public String getPrescricao() {
        return prescricao;
    }

    public void setDiagnostico(String diagnostico) {
        if (diagnostico == null || diagnostico.isBlank()) {
            throw new IllegalArgumentException("O diagnóstico deve ser informado.");
        }
        this.diagnostico = diagnostico;
    }

    public void setPrescricao(String prescricao) {
        if (prescricao == null || prescricao.isBlank()) {
            throw new IllegalArgumentException("A prescrição deve ser informada.");
        }
        this.prescricao = prescricao;
    }

    public void registrar() {
        if (diagnostico.isBlank()) {
            System.out.println("Erro: informe o diagnóstico antes de registrar a consulta.");
            return;
        }
        System.out.println("Consulta registrada com sucesso em " + data + "!");
    }

    @Override
    public String toString() {
        return "ConsultaVeterinaria [" +
                "id=" + id +
                ", idProntuario=" + idProntuario +
                ", crmVeterinario='" + crmVeterinario + '\'' +
                ", data=" + data +
                ", diagnostico='" + diagnostico + '\'' +
                ", prescricao='" + prescricao + '\'' + "]";
    }

    public boolean cadastrar(Banco db) {
        if (diagnostico.isBlank() || prescricao.isBlank()) {
            throw new IllegalArgumentException("Informe diagnóstico e prescrição.");
        }
        db.queryUpdate(String.format("INSERT INTO consultas(id_prontuario, crmv, data, diagnostico, prescricao) VALUES (%d, '%s', '%s', '%s', '%s')",
                idProntuario, crmVeterinario.replace("'", "''"), data.toString(), diagnostico.replace("'", "''"), prescricao.replace("'", "''")));
        try (ResultSet rs = db.querySearch("SELECT last_insert_rowid()")) {
            if (rs.next()) id = rs.getInt(1);
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return true;
    }

    public boolean editar(Banco db) {
        return db.queryUpdate(String.format("UPDATE consultas SET diagnostico = '%s', prescricao = '%s' WHERE id = %d",
                diagnostico.replace("'", "''"), prescricao.replace("'", "''"), id)) > 0;
    }

    public static boolean remover(Banco db, int id) {
        return db.queryUpdate(String.format("DELETE FROM consultas WHERE id = %d", id)) > 0;
    }

    public static List<ConsultaVeterinaria> listarPorProntuario(Banco db, int idProntuario) {
        List<ConsultaVeterinaria> consultas = new ArrayList<>();
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM consultas WHERE id_prontuario = %d ORDER BY data, id", idProntuario))) {
            while (rs.next()) {
                ConsultaVeterinaria c = new ConsultaVeterinaria(rs.getInt("id"), rs.getInt("id_prontuario"), rs.getString("crmv"), LocalDate.parse(rs.getString("data")));
                c.setDiagnostico(rs.getString("diagnostico"));
                c.setPrescricao(rs.getString("prescricao"));
                consultas.add(c);
            }
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return consultas;
    }

}
