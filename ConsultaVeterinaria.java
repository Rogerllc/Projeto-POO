import java.time.LocalDate;

public class ConsultaVeterinaria {
    private final int id;
    private final int idProntuario;
    private final String crmVeterinario;
    private final LocalDate data;
    private String diagnostico;
    private String prescricao;

    public ConsultaVeterinaria(int id, int idProntuario, String crmVeterinario) {
        if (!Veterinario.isCrmvValido(crmVeterinario)) {
            throw new IllegalArgumentException("CRMV inválido. Use o formato CRMV-UF número (ex.: CRMV-RN 1234).");
        }
        if (id <= 0 || idProntuario <= 0) {
            throw new IllegalArgumentException("O id da consulta e o id do prontuário devem ser positivos.");
        }
        this.id = id;
        this.idProntuario = idProntuario;
        this.crmVeterinario = crmVeterinario;
        this.data = LocalDate.now();
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

    // Confirma o registro da consulta (exige diagnóstico preenchido)
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
}
