# PatinhasOng

Sistema Java com interface gráfica Swing para gestão de animais e processos de adoção. Os dados são armazenados em um banco SQLite local.

## Funcionamento

Todos os usuários precisam se cadastrar com uma senha de sua escolha antes de entrar no sistema. Não existem contas ou senhas predefinidas. Na opção Equipe ONG, o identificador define o perfil: e-mail para responsável e CRMV completo (por exemplo, CRMV-RN 1234) para veterinário.

- **Responsável da ONG:** entra com e-mail e senha e utiliza as telas de animais, prontuários, adotantes e adoções.
- **Veterinário:** entra com CRMV e senha, acessa as telas da equipe e pode registrar consultas com diagnóstico e prescrição.
- **Adotante:** entra com CPF e senha, consulta os animais disponíveis, responde ao questionário de adoção e acompanha seus pedidos.

O cadastro de um animal cria automaticamente seu prontuário. As vacinas e consultas ficam vinculadas a esse prontuário. Cada pedido de adoção guarda as respostas do questionário e pode ser aprovado ou recusado pela equipe. Para registrar um pedido pela equipe, o adotante já deve estar cadastrado.

Ao solicitar uma adoção, o animal fica em processo. Ao aprovar, fica adotado; ao recusar, volta a ficar disponível. A remoção de um animal também remove seu prontuário, vacinas, consultas e pedidos de adoção.

O arquivo `sistema.db` e suas tabelas são criados na primeira execução, na pasta do projeto. Os registros permanecem salvos depois de fechar o programa. As senhas são armazenadas como texto no banco.

## Compilação e execução

É necessário ter o JDK 11 ou superior instalado. O driver SQLite está na pasta `lib`.

Abra o terminal na pasta do projeto e compile:

```bash
javac -encoding UTF-8 -cp "lib/*" -d out src/Main.java src/database/*.java src/models/*.java
```

No macOS ou Linux, execute:

```bash
java --enable-native-access=ALL-UNNAMED -cp "out:lib/*" Main
```

No Windows, execute:

```bash
java --enable-native-access=ALL-UNNAMED -cp "out;lib/*" Main
```

O parâmetro `--enable-native-access=ALL-UNNAMED` permite o carregamento da biblioteca nativa do SQLite e evita o aviso nas versões recentes do Java.

No VS Code, instale a extensão **Extension Pack for Java**, abra a pasta do projeto e execute `src/Main.java` pelo botão **Run**. As configurações de código-fonte, driver e execução já estão na pasta `.vscode`.
