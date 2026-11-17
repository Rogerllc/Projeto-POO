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
        this.id = id;
        this.cpfAdotante = cpfAdotante;
        this.idAnimal = idAnimal;
        this.dataPedido = LocalDate.now();
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
}