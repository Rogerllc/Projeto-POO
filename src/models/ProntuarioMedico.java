package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProntuarioMedico {
    private final int id;
    private final int idAnimal;
    private String historico;
    private List<String> vacinas;
    private List<ConsultaVeterinaria> consultas;
    private LocalDate dataUltimaConsulta;

    public ProntuarioMedico() {
        this(0, 0);
    }

    public ProntuarioMedico(int id, int idAnimal) {
        this.id = id;
        this.idAnimal = idAnimal;
        historico = "";
        vacinas = new ArrayList<>();
        consultas = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getIdAnimal() {
        return idAnimal;
    }

    public void adicionarVacina(String vacina) {
        if (vacina == null || vacina.isBlank()) {
            throw new IllegalArgumentException("A vacina deve ser informada.");
        }
        vacinas.add(vacina);
        historico += "Vacina: " + vacina + "\n";
    }

    public void registrarConsulta(ConsultaVeterinaria consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("A consulta deve ser informada.");
        }
        consultas.add(consulta);
        dataUltimaConsulta = consulta.getData();
        historico += "Consulta em " + consulta.getData() + ": " + consulta.getDiagnostico() + "\n";
    }

    public String getHistorico() {
        return historico;
    }

    public List<String> getVacinas() {
        return vacinas;
    }

    public List<ConsultaVeterinaria> getConsultas() {
        return consultas;
    }

    public LocalDate getDataUltimaConsulta() {
        return dataUltimaConsulta;
    }

    @Override
    public String toString() {
        return "ProntuarioMedico [vacinas=" + vacinas + ", consultas=" + consultas.size()
                + ", dataUltimaConsulta=" + dataUltimaConsulta + "]";
    }

    public static boolean cadastrar(Banco db, int idAnimal) {
        return db.queryUpdate(String.format("INSERT INTO prontuarios(id_animal) VALUES (%d)", idAnimal)) > 0;
    }

    public boolean salvarVacina(Banco db, String vacina) {
        if (vacina == null || vacina.isBlank()) throw new IllegalArgumentException("Informe a vacina.");
        return db.queryUpdate(String.format("INSERT INTO vacinas(id_prontuario, nome) VALUES (%d, '%s')", id, vacina.replace("'", "''"))) > 0;
    }

    public static boolean removerVacina(Banco db, int idVacina) {
        return db.queryUpdate(String.format("DELETE FROM vacinas WHERE id = %d", idVacina)) > 0;
    }

    private void carregarHistorico(Banco db) {
        try (ResultSet rs = db.querySearch(String.format("SELECT nome FROM vacinas WHERE id_prontuario = %d ORDER BY id", id))) {
            while (rs.next()) adicionarVacina(rs.getString("nome"));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        for (ConsultaVeterinaria c : ConsultaVeterinaria.listarPorProntuario(db, id)) registrarConsulta(c);
    }

    public static ProntuarioMedico buscarPorAnimal(Banco db, int idAnimal) {
        ProntuarioMedico pm = null;
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM prontuarios WHERE id_animal = %d", idAnimal))) {
            if (rs.next()) pm = new ProntuarioMedico(rs.getInt("id"), rs.getInt("id_animal"));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        if (pm != null) pm.carregarHistorico(db);
        return pm;
    }

    public static List<ProntuarioMedico> listar(Banco db) {
        List<ProntuarioMedico> prontuarios = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM prontuarios ORDER BY id")) {
            while (rs.next()) prontuarios.add(new ProntuarioMedico(rs.getInt("id"), rs.getInt("id_animal")));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        for (ProntuarioMedico pm : prontuarios) pm.carregarHistorico(db);
        return prontuarios;
    }

    public boolean editar(Banco db, List<String> novasVacinas) {
        db.queryUpdate("BEGIN TRANSACTION");
        try {
            // Mantém os IDs das vacinas existentes quando seus nomes são corrigidos.
            List<Integer> ids = new ArrayList<>();
            try (ResultSet rs = db.querySearch(String.format("SELECT id FROM vacinas WHERE id_prontuario = %d ORDER BY id", id))) {
                while (rs.next()) ids.add(rs.getInt("id"));
            }
            for (int i = 0; i < novasVacinas.size(); i++) {
                String vacina = novasVacinas.get(i);
                if (vacina == null || vacina.isBlank()) throw new IllegalArgumentException("Informe a vacina.");
                if (i < ids.size()) {
                    db.queryUpdate(String.format("UPDATE vacinas SET nome = '%s' WHERE id = %d AND id_prontuario = %d", vacina.replace("'", "''"), ids.get(i), id));
                } else salvarVacina(db, vacina);
            }
            for (int i = novasVacinas.size(); i < ids.size(); i++) removerVacina(db, ids.get(i));
            for (ConsultaVeterinaria consulta : consultas) consulta.editar(db);
            db.queryUpdate("COMMIT");
            return true;
        } catch (SQLException | RuntimeException e) {
            db.queryUpdate("ROLLBACK");
            throw new IllegalStateException("Não foi possível editar o prontuário: " + e.getMessage(), e);
        }
    }

    public static boolean remover(Banco db, int id) {
        return db.queryUpdate(String.format("DELETE FROM prontuarios WHERE id = %d", id)) > 0;
    }

}
