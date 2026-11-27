package models;

import database.Banco;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class ProcessoAdocao {
    public static final String STATUS_EM_ANALISE = "EM_ANALISE";
    public static final String STATUS_APROVADO = "APROVADO";
    public static final String STATUS_RECUSADO = "RECUSADO";

    private int id;
    private String cpfAdotante;
    private int idAnimal;
    private LocalDate dataPedido;
    private String status = STATUS_EM_ANALISE;

    public ProcessoAdocao(int id, String cpfAdotante, int idAnimal) {
        this(id, cpfAdotante, idAnimal, LocalDate.now(), STATUS_EM_ANALISE);
    }

    public ProcessoAdocao(int id, String cpfAdotante, int idAnimal, LocalDate dataPedido, String status) {
        this.id = id;
        this.cpfAdotante = cpfAdotante;
        this.idAnimal = idAnimal;
        this.dataPedido = dataPedido;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getCpfAdotante() {
        return cpfAdotante;
    }

    public int getIdAnimal() {
        return idAnimal;
    }

    public LocalDate getDataPedido() {
        return dataPedido;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusFormatado() {
        if (STATUS_EM_ANALISE.equals(this.status)) {
            return "Em análise";
        } else if (STATUS_APROVADO.equals(this.status)) {
            return "Aprovado";
        } else if (STATUS_RECUSADO.equals(this.status)) {
            return "Recusado";
        }
        return this.status;
    }

    public void aprovar() {
        if (!isEmAnalise()) {
            System.out.println("Erro: este processo já foi finalizado.");
            return;
        }
        this.status = STATUS_APROVADO;
    }

    public void recusar() {
        if (!isEmAnalise()) {
            System.out.println("Erro: este processo já foi finalizado.");
            return;
        }
        this.status = STATUS_RECUSADO;
    }

    public boolean isEmAnalise() {
        return STATUS_EM_ANALISE.equals(this.status);
    }

    public boolean isAprovado() {
        return STATUS_APROVADO.equals(this.status);
    }

    public boolean isRecusado() {
        return STATUS_RECUSADO.equals(this.status);
    }

    public static boolean isCpfValido(String cpfAdotante) {
        if (cpfAdotante == null || cpfAdotante.length() != 11) {
            return false;
        }
        for (int i = 0; i < cpfAdotante.length(); i++) {
            if (!Character.isDigit(cpfAdotante.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "ProcessoAdocao [" +
                "id=" + id +
                ", cpfAdotante='" + cpfAdotante + '\'' +
                ", idAnimal=" + idAnimal +
                ", dataPedido=" + dataPedido +
                ", status='" + status + '\'' + "]";
    }

    private boolean espaco;
    private boolean tempo;
    private boolean familia;
    private String autorizacao = "Pendente";

    public void setAvaliacao(boolean espaco, boolean tempo, String autorizacao, boolean familia) {
        this.espaco = espaco;
        this.tempo = tempo;
        this.autorizacao = autorizacao;
        this.familia = familia;
    }

    public boolean isEspaco() { return espaco; }
    public boolean isTempo() { return tempo; }
    public boolean isFamilia() { return familia; }
    public String getAutorizacao() { return autorizacao; }

    public boolean cadastrar(Banco db) {
        if (Adotante.buscarPorCpf(db, cpfAdotante) == null) {
            throw new IllegalArgumentException("Cadastre o adotante antes de solicitar a adoção.");
        }
        db.queryUpdate("BEGIN TRANSACTION");
        try {
            if (db.queryUpdate(String.format("UPDATE animais SET status = 'EM_PROCESSO' WHERE id = %d AND status = 'DISPONIVEL'", idAnimal)) != 1) {
                throw new IllegalStateException("Este animal não está disponível para adoção.");
            }
            db.queryUpdate(String.format("INSERT INTO processos(id_animal, cpf, data, espaco, tempo, autorizacao, familia) VALUES (%d, '%s', '%s', %d, %d, '%s', %d)",
                    idAnimal, cpfAdotante.replace("'", "''"), dataPedido.toString(), espaco ? 1 : 0, tempo ? 1 : 0, autorizacao.replace("'", "''"), familia ? 1 : 0));
            try (ResultSet rs = db.querySearch("SELECT last_insert_rowid()")) {
                if (rs.next()) id = rs.getInt(1);
            }
            db.queryUpdate("COMMIT");
            return true;
        } catch (SQLException | RuntimeException e) {
            db.queryUpdate("ROLLBACK");
            throw new IllegalStateException("Não foi possível registrar o pedido: " + e.getMessage(), e);
        }
    }

    public boolean finalizar(Banco db, boolean aprovado) {
        String novoStatus = aprovado ? STATUS_APROVADO : STATUS_RECUSADO;
        db.queryUpdate("BEGIN TRANSACTION");
        try {
            if (db.queryUpdate(String.format("UPDATE processos SET status = '%s' WHERE id = %d AND id_animal = %d AND status = 'EM_ANALISE'", novoStatus, id, idAnimal)) != 1) {
                throw new IllegalStateException("Pedido não encontrado ou já finalizado.");
            }
            if (db.queryUpdate(String.format("UPDATE animais SET status = '%s' WHERE id = %d AND status = 'EM_PROCESSO'", aprovado ? "ADOTADO" : "DISPONIVEL", idAnimal)) != 1) {
                throw new IllegalStateException("O estado do animal mudou. Reabra a ficha.");
            }
            db.queryUpdate("COMMIT");
            status = novoStatus;
            return true;
        } catch (RuntimeException e) {
            db.queryUpdate("ROLLBACK");
            throw e;
        }
    }

    private static ProcessoAdocao ler(ResultSet rs) throws SQLException {
        ProcessoAdocao p = new ProcessoAdocao(rs.getInt("id"), rs.getString("cpf"), rs.getInt("id_animal"), LocalDate.parse(rs.getString("data")), rs.getString("status"));
        p.setAvaliacao(rs.getInt("espaco") == 1, rs.getInt("tempo") == 1, rs.getString("autorizacao"), rs.getInt("familia") == 1);
        return p;
    }

    public static List<ProcessoAdocao> listar(Banco db) {
        List<ProcessoAdocao> processos = new ArrayList<>();
        try (ResultSet rs = db.querySearch("SELECT * FROM processos ORDER BY id")) {
            while (rs.next()) processos.add(ler(rs));
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return processos;
    }

    public static ProcessoAdocao buscarPorAnimal(Banco db, int idAnimal) {
        try (ResultSet rs = db.querySearch(String.format("SELECT * FROM processos WHERE id_animal = %d ORDER BY id DESC LIMIT 1", idAnimal))) {
            return rs.next() ? ler(rs) : null;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

}
