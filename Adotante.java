public class Adotante {
    private String cpf;
    private String nome;
    private String endereco;
    private boolean aprovado;

    public Adotante(String cpf, String nome, String endereco) {
        this.cpf = cpf;
        this.nome = nome;
        this.endereco = endereco;
        this.aprovado = avaliarPerfil();
    }

    public boolean avaliarPerfil() {

        if (this.cpf == null || this.nome == null || this.endereco == null || this.endereco.trim().isEmpty()) {
            this.aprovado = false;
            return false;
        }

        if (!this.cpf.matches("\\d{11}")) {
            this.aprovado = false;
            return false;
        }

        if (this.nome.trim().isEmpty() || this.nome.matches(".*\\d.*")) {
            this.aprovado = false;
            return false;
        }

        this.aprovado = true;
        return true;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
        this.avaliarPerfil();
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        this.avaliarPerfil();
    }

    public String getEndereco() {
        return this.endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
        this.avaliarPerfil();
    }

    public boolean isAprovado() {
        return this.aprovado;
    }

    @Override
    public String toString() {
        return "Adotante: " + this.nome + " | CPF: " + this.cpf + " | Aprovado: " + this.aprovado;
    }
}