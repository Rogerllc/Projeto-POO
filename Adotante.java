
import java.util.ArrayList;

public class Adotante {

    private ArrayList<ProcessoAdocao> processos;

    public Adotante() {
        this.processos = new ArrayList<>();
    }

    public void cadastrarProcesso(ProcessoAdocao processo) {
        if (processo == null) {
            throw new IllegalArgumentException("O processo de adoção não pode ser nulo.");
        }

        processos.add(processo);
        System.out.println("Processo de adoção cadastrado com sucesso!");
    }

    public ProcessoAdocao buscarProcesso(int id) {
        for (ProcessoAdocao processo : processos) {
            if (processo.getId() == id) {
                return processo;
            }
        }

        return null;
    }

    public void aprovarProcesso(int id) {
        ProcessoAdocao processo = buscarProcesso(id);

        if (processo == null) {
            System.out.println("Erro: processo de adoção não encontrado.");
            return;
        }

        if (!processo.isEmAnalise()) {
            System.out.println("Erro: este processo já foi finalizado.");
            return;
        }

        processo.aprovar();
        System.out.println("Processo de adoção aprovado!");
    }

    public void recusarProcesso(int id) {
        ProcessoAdocao processo = buscarProcesso(id);

        if (processo == null) {
            System.out.println("Erro: processo de adoção não encontrado.");
            return;
        }

        if (!processo.isEmAnalise()) {
            System.out.println("Erro: este processo já foi finalizado.");
            return;
        }

        processo.recusar();
        System.out.println("Processo de adoção recusado!");
    }

    public void listarProcessos() {
        if (processos.isEmpty()) {
            System.out.println("Não existem processos de adoção cadastrados.");
            return;
        }

        for (ProcessoAdocao processo : processos) {
            System.out.println(processo);
        }
    }

}
