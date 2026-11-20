import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProntuarioMedico {
    private String historico;
    private List<String> vacinas;
    private List<ConsultaVeterinaria> consultas;
    private LocalDate dataUltimaConsulta;

    public ProntuarioMedico() {
        historico = "";
        vacinas = new ArrayList<>();
        consultas = new ArrayList<>();
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
}
