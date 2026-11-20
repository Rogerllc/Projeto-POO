public class Animal {
    public static final String STATUS_DISPONIVEL = "DISPONIVEL";
    public static final String STATUS_ADOTADO = "ADOTADO";

    private final int id;
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
}
