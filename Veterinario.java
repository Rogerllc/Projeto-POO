public class Veterinario {
    private final String crmv;
    private final String nome;
    private final String especialidade;
    private final String senha;
    private int proximoIdConsulta = 1;

    public Veterinario(String crmv, String nome, String especialidade, String senha) {
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

    // Formato aceito: CRMV-UF número (ex.: CRMV-RN 1234)
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

    public boolean login(String crm, String senha) {
        return this.crmv.equals(crm) && this.senha.equals(senha);
    }

    // Cria uma consulta para o animal. O prontuário é 1:1 com o animal,
    // então idProntuario = id do animal.
    public ConsultaVeterinaria realizarConsulta(Animal a) {
        if (a == null) {
            throw new IllegalArgumentException("O animal não pode ser nulo.");
        }
        return new ConsultaVeterinaria(proximoIdConsulta++, a.getId(), crmv);
    }

    // Só o veterinário que realizou a consulta pode prescrever nela
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
}
