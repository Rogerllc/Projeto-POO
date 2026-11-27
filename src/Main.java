import database.Banco;
import models.*;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Main extends JFrame {

    private static final Color FUNDO = new Color(246, 248, 247);
    private static final Color BORDA = new Color(220, 224, 228);
    private static final Color TEXTO = new Color(31, 41, 55);
    private static final Color CINZA = new Color(107, 114, 128);
    private static final Color VERDE = new Color(15, 118, 110);
    private static final Color VERMELHO = new Color(185, 28, 28);
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Banco db;

    private ResponsavelOng ongLogada = null;
    private Veterinario vetLogado = null;
    private Adotante adotanteLogado = null;
    private String aba = "Animais", filtro = "TODOS";
    private Animal animalAtual = null;
    private final JPanel raiz = new JPanel(new BorderLayout());

    public Main() {
        db = new Banco();
        criarTabelas(db);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { db.desconectar(); }
        });
        setTitle("PatinhasOng - Gestão e Adoção de Animais");
        setSize(1080, 720);
        setMinimumSize(new Dimension(920, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(raiz);
        atualizarTela();
    }

    public static void criarTabelas(Banco db) {
        db.queryUpdate("CREATE TABLE IF NOT EXISTS responsaveis (email TEXT PRIMARY KEY COLLATE NOCASE, nome TEXT NOT NULL, cargo TEXT NOT NULL, senha TEXT NOT NULL)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS veterinarios (crmv TEXT PRIMARY KEY, nome TEXT NOT NULL, especialidade TEXT NOT NULL, senha TEXT NOT NULL)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS adotantes (cpf TEXT PRIMARY KEY, nome TEXT NOT NULL, endereco TEXT NOT NULL, senha TEXT NOT NULL)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS animais (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, especie TEXT NOT NULL, idade INTEGER NOT NULL CHECK(idade >= 0), status TEXT NOT NULL DEFAULT 'DISPONIVEL' CHECK(status IN ('DISPONIVEL','EM_PROCESSO','ADOTADO')))");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS prontuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, id_animal INTEGER NOT NULL UNIQUE REFERENCES animais(id) ON DELETE CASCADE)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS vacinas (id INTEGER PRIMARY KEY AUTOINCREMENT, id_prontuario INTEGER NOT NULL REFERENCES prontuarios(id) ON DELETE CASCADE, nome TEXT NOT NULL)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS consultas (id INTEGER PRIMARY KEY AUTOINCREMENT, id_prontuario INTEGER NOT NULL REFERENCES prontuarios(id) ON DELETE CASCADE, crmv TEXT NOT NULL REFERENCES veterinarios(crmv), data TEXT NOT NULL, diagnostico TEXT NOT NULL, prescricao TEXT NOT NULL)");
        db.queryUpdate("CREATE TABLE IF NOT EXISTS processos (id INTEGER PRIMARY KEY AUTOINCREMENT, id_animal INTEGER NOT NULL REFERENCES animais(id) ON DELETE CASCADE, cpf TEXT NOT NULL REFERENCES adotantes(cpf), data TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'EM_ANALISE' CHECK(status IN ('EM_ANALISE','APROVADO','RECUSADO')), espaco INTEGER NOT NULL, tempo INTEGER NOT NULL, autorizacao TEXT NOT NULL, familia INTEGER NOT NULL)");
        db.queryUpdate("CREATE UNIQUE INDEX IF NOT EXISTS processo_ativo ON processos(id_animal) WHERE status IN ('EM_ANALISE', 'APROVADO')");
    }

    private boolean isEquipeOng() { return ongLogada != null || vetLogado != null; }

    private boolean isNomeValido(String nome) {
        return nome != null && !nome.trim().isEmpty() && nome.trim().matches("[\\p{L} ]+");
    }

    private void atualizarTela() {
        raiz.removeAll();
        if (!isEquipeOng() && adotanteLogado == null) {
            raiz.add(criarTelaLogin(), BorderLayout.CENTER);
        } else {
            raiz.add(criarBarraTopo(), BorderLayout.NORTH);
            JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 24));
            centro.setBackground(FUNDO);
            centro.add(criarConteudo());

            JScrollPane scroll = new JScrollPane(centro);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            raiz.add(scroll, BorderLayout.CENTER);
        }
        raiz.revalidate();
        raiz.repaint();
    }

    private JPanel criarTelaLogin() {
        JPanel fundo = new JPanel(new GridBagLayout());
        fundo.setBackground(FUNDO);

        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(430, 390));

        JLabel logo = new JLabel("PatinhasOng");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(VERDE);

        JComboBox<String> cbPerfil = new JComboBox<>(new String[]{"Equipe ONG (Admin / Veterinário)", "Adotante"});
        cbPerfil.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbPerfil.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblId = new JLabel("E-mail ou CRMV:");
        JTextField txtId = new JTextField();
        txtId.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtId.setAlignmentX(LEFT_ALIGNMENT);

        JPasswordField txtSenha = new JPasswordField();
        txtSenha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtSenha.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnCadastrar = btn("Cadastrar novo membro da Equipe", Color.WHITE, TEXTO, e -> {
            if (cbPerfil.getSelectedIndex() == 0) cadastrarEquipe();
            else cadastrarAdotante(false);
        });
        btnCadastrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnCadastrar.setAlignmentX(LEFT_ALIGNMENT);

        cbPerfil.addActionListener(e -> {
            boolean equipe = cbPerfil.getSelectedIndex() == 0;
            lblId.setText(equipe ? "E-mail ou CRMV:" : "CPF (11 números):");
            btnCadastrar.setText(equipe ? "Cadastrar novo membro da Equipe" : "Criar conta de Adotante");
        });

        JButton btnEntrar = btn("Entrar", VERDE, Color.WHITE, e -> autenticar(cbPerfil.getSelectedIndex() == 0, txtId.getText().trim(), new String(txtSenha.getPassword())));
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnEntrar.setAlignmentX(LEFT_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(14));
        card.add(new JLabel("Perfil de acesso:"));
        card.add(cbPerfil);
        card.add(Box.createVerticalStrut(10));
        card.add(lblId);
        card.add(txtId);
        card.add(Box.createVerticalStrut(10));
        card.add(new JLabel("Senha:"));
        card.add(txtSenha);
        card.add(Box.createVerticalStrut(18));
        card.add(btnEntrar);
        card.add(Box.createVerticalStrut(8));
        card.add(btnCadastrar);

        fundo.add(card);
        return fundo;
    }

    private void autenticar(boolean equipe, String user, String senha) {
        if (equipe) {
            if (Veterinario.isLoginCrmv(user)) {
                if (!Veterinario.isCrmvValido(Veterinario.normalizarCrmv(user))) {
                    JOptionPane.showMessageDialog(this, "Informe o CRMV completo, por exemplo: CRMV-RN 1234.");
                    return;
                }
                Veterinario v = autenticarVetBanco(user, senha);
                if (v != null) { ongLogada = null; vetLogado = v; atualizarTela(); return; }
            } else {
                ResponsavelOng r = autenticarOngBanco(user, senha);
                if (r != null) { vetLogado = null; ongLogada = r; atualizarTela(); return; }
            }
        } else {
            Adotante a = autenticarAdotanteBanco(user, senha);
            if (a != null) { adotanteLogado = a; atualizarTela(); return; }
        }
        JOptionPane.showMessageDialog(this, "Credenciais inválidas.", "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel criarBarraTopo() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Color.WHITE);
        barra.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA), new EmptyBorder(10, 32, 10, 32)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        esq.setOpaque(false);
        JLabel logo = new JLabel("PatinhasOng");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(VERDE);
        esq.add(logo);

        String[] abas = isEquipeOng() ? new String[]{"Animais", "Prontuários", "Adotantes", "Adoções"} : new String[]{"Animais", "Minhas Adoções"};
        for (String nomeAba : abas) {
            JLabel link = new JLabel(nomeAba);
            link.setFont(new Font("Segoe UI", nomeAba.equals(aba) ? Font.BOLD : Font.PLAIN, 14));
            link.setForeground(nomeAba.equals(aba) ? VERDE : TEXTO);
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { aba = nomeAba; animalAtual = null; atualizarTela(); }
            });
            esq.add(link);
        }

        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        dir.setOpaque(false);
        String usuario = ongLogada != null ? ongLogada.getNome() + " (" + ongLogada.getCargo() + ")"
                : (vetLogado != null ? vetLogado.getNome() + " (" + vetLogado.getCrmv() + ")" : adotanteLogado.getNome() + " (Adotante)");
        dir.add(new JLabel(usuario));
        dir.add(btn("Sair", Color.WHITE, TEXTO, e -> {
            ongLogada = null; vetLogado = null; adotanteLogado = null; aba = "Animais"; filtro = "TODOS"; animalAtual = null; atualizarTela();
        }));

        barra.add(esq, BorderLayout.WEST);
        barra.add(dir, BorderLayout.EAST);
        return barra;
    }

    private JPanel criarConteudo() {
        if (animalAtual != null) return criarTelaFicha(animalAtual);
        if (!aba.equals("Animais")) return criarTelaSecundaria();

        JPanel pag = paginaBase("Animais resgatados", isEquipeOng() ? "+ Cadastrar animal" : null, e -> cadastrarAnimal());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.setAlignmentX(LEFT_ALIGNMENT);
        String[][] opcoes = {{"Todos", "TODOS"}, {"Disponíveis", Animal.STATUS_DISPONIVEL}, {"Em processo", "EM_PROCESSO"}, {"Adotados", Animal.STATUS_ADOTADO}};
        for (String[] f : opcoes) {
            boolean ativo = filtro.equals(f[1]);
            filtros.add(btn(f[0], ativo ? TEXTO : Color.WHITE, ativo ? Color.WHITE : TEXTO, e -> { filtro = f[1]; atualizarTela(); }));
        }
        pag.add(filtros);
        pag.add(Box.createVerticalStrut(14));

        JPanel tabela = card();
        tabela.setLayout(new BoxLayout(tabela, BoxLayout.Y_AXIS));
        tabela.add(linhaAnimal(new String[]{"ID", "Nome", "Espécie / Raça", "Idade", "Status", "Última consulta", ""}, true, null));

        for (Animal a : listarAnimais()) {
            if (!filtro.equals("TODOS") && !filtro.equals(a.getStatusAdocao())) continue;
            ProntuarioMedico pm = buscarProntuario(a.getId());
            String ult = (pm != null && pm.getDataUltimaConsulta() != null) ? pm.getDataUltimaConsulta().format(DATA_BR) : "-";
            tabela.add(linhaAnimal(new String[]{String.format("%03d", a.getId()), a.getNome(), a.getEspecie(), a.getIdade() + " ano(s)", a.getStatusAdocao(), ult, "Ver ficha"}, false, a));
        }
        pag.add(tabela);
        return pag;
    }

    private JPanel linhaAnimal(String[] cols, boolean header, Animal a) {
        JPanel row = new JPanel(new GridLayout(1, 7, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA), new EmptyBorder(8, 10, 8, 10)));

        for (int i = 0; i < cols.length; i++) {
            if (header) {
                JLabel l = new JLabel(cols[i]); l.setForeground(CINZA); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); row.add(l);
            } else if (i == 4) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); p.setOpaque(false); p.add(tagAnimal(cols[i])); row.add(p);
            } else if (i == 6) {
                JLabel link = new JLabel("Ver ficha", SwingConstants.RIGHT);
                link.setForeground(VERDE); link.setFont(new Font("Segoe UI", Font.BOLD, 13));
                link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { animalAtual = a; atualizarTela(); } });
                JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                acoes.setOpaque(false);
                acoes.add(link);
                if (isEquipeOng()) acoes.add(menuAcoes(() -> editarAnimal(a), () -> removerAnimal(a)));
                row.add(acoes);
            } else {
                JLabel l = new JLabel(cols[i]); if (i == 1) l.setFont(new Font("Segoe UI", Font.BOLD, 13)); row.add(l);
            }
        }
        return row;
    }

    private JPanel criarTelaFicha(Animal animal) {
        JPanel pag = new JPanel();
        pag.setLayout(new BoxLayout(pag, BoxLayout.Y_AXIS));
        pag.setBackground(FUNDO);
        pag.add(larguraFixa());

        JLabel voltar = new JLabel("← Voltar para animais");
        voltar.setForeground(CINZA); voltar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        voltar.setAlignmentX(LEFT_ALIGNMENT);
        voltar.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { animalAtual = null; atualizarTela(); } });
        pag.add(voltar);
        pag.add(Box.createVerticalStrut(10));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false); topo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); topo.setAlignmentX(LEFT_ALIGNMENT);
        JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); titulo.setOpaque(false);
        JLabel nome = new JLabel(animal.getNome()); nome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titulo.add(nome); titulo.add(tagAnimal(animal.getStatusAdocao()));
        topo.add(titulo, BorderLayout.WEST);

        if (isEquipeOng()) {
            topo.add(btn("Remover animal", new Color(254, 242, 242), VERMELHO, e -> removerAnimal(animal)), BorderLayout.EAST);
        }
        pag.add(topo);

        JLabel sub = new JLabel(animal.getEspecie() + " · " + animal.getIdade() + " ano(s) · ID " + String.format("%03d", animal.getId()));
        sub.setForeground(CINZA); sub.setAlignmentX(LEFT_ALIGNMENT);
        pag.add(sub);
        pag.add(Box.createVerticalStrut(18));

        JPanel grid = new JPanel(new GridLayout(1, 2, 18, 0));
        grid.setOpaque(false); grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.add(cardProntuario(animal));
        grid.add(cardAdocao(animal));
        pag.add(grid);
        return pag;
    }

    private JPanel cardProntuario(Animal animal) {
        JPanel c = card(); c.setLayout(new BorderLayout());
        JPanel corpo = new JPanel(); corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS)); corpo.setOpaque(false);

        JLabel t = new JLabel("Prontuário médico"); t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        corpo.add(t); corpo.add(Box.createVerticalStrut(8));

        ProntuarioMedico pm = buscarProntuario(animal.getId());
        String ult = (pm != null && pm.getDataUltimaConsulta() != null) ? pm.getDataUltimaConsulta().format(DATA_BR) : "Nenhuma";
        corpo.add(new JLabel("Última consulta: " + ult));
        corpo.add(Box.createVerticalStrut(12));

        corpo.add(subtitulo("Vacinas"));
        if (pm == null || pm.getVacinas().isEmpty()) corpo.add(new JLabel("Nenhuma vacina registrada."));
        else for (String v : pm.getVacinas()) corpo.add(linhaPar(v, ult, CINZA));

        corpo.add(Box.createVerticalStrut(12));
        corpo.add(subtitulo("Consultas"));
        if (pm == null || pm.getConsultas().isEmpty()) corpo.add(new JLabel("Nenhuma consulta registrada."));
        else for (ConsultaVeterinaria cv : pm.getConsultas()) {
            JLabel item = new JLabel("<html><b>" + cv.getData().format(DATA_BR) + " (" + cv.getCrmVeterinario() + ")</b><br>Diag.: " + cv.getDiagnostico() + " | Presc.: " + cv.getPrescricao() + "</html>");
            item.setBorder(new EmptyBorder(4, 0, 4, 0));
            corpo.add(item);
        }
        c.add(corpo, BorderLayout.NORTH);

        if (isEquipeOng()) {
            JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); rodape.setOpaque(false);
            rodape.setBorder(new EmptyBorder(14, 0, 0, 0));
            rodape.add(btn("+ Vacina", Color.WHITE, TEXTO, e -> {
                String vac = JOptionPane.showInputDialog(this, "Nome da vacina:");
                if (vac != null && !vac.isBlank()) { salvarVacinaBanco(animal.getId(), vac.trim()); atualizarTela(); }
            }));
            rodape.add(btn("+ Consulta", VERDE, Color.WHITE, e -> registrarConsulta(animal)));
            c.add(rodape, BorderLayout.SOUTH);
        }
        return c;
    }

    private JPanel cardAdocao(Animal animal) {
        JPanel c = card(); c.setLayout(new BorderLayout());
        JPanel corpo = new JPanel(); corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS)); corpo.setOpaque(false);

        JLabel t = new JLabel("Pedido de adoção"); t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        corpo.add(t); corpo.add(Box.createVerticalStrut(10));

        ProcessoAdocao proc = buscarProcessoAnimal(animal.getId());

        if (adotanteLogado != null) {
            if (Animal.STATUS_DISPONIVEL.equals(animal.getStatusAdocao())) {
                corpo.add(new JLabel("<html>Animal disponível! Responda o questionário abaixo para solicitar adoção.</html>"));
                c.add(corpo, BorderLayout.NORTH);
                c.add(btn("Solicitar adoção", VERDE, Color.WHITE, e -> solicitarAdocao(animal, true)), BorderLayout.SOUTH);
                return c;
            } else if (proc == null || !proc.getCpfAdotante().equals(adotanteLogado.getCpf())) {
                corpo.add(new JLabel("Animal já adotado ou em processo com outro interessado."));
                c.add(corpo, BorderLayout.NORTH);
                return c;
            }
        }

        if (proc == null) {
            corpo.add(new JLabel("Nenhum pedido de adoção registrado."));
            if (isEquipeOng() && Animal.STATUS_DISPONIVEL.equals(animal.getStatusAdocao())) {
                c.add(btn("+ Registrar pedido", VERDE, Color.WHITE, e -> solicitarAdocao(animal, false)), BorderLayout.SOUTH);
            }
            c.add(corpo, BorderLayout.NORTH);
            return c;
        }

        Adotante ad = buscarAdotante(proc.getCpfAdotante());
        String nomeAd = ad != null ? ad.getNome() : proc.getCpfAdotante();
        String endAd = ad != null ? ad.getEndereco() : "Não informado";

        JPanel cab = new JPanel(new BorderLayout()); cab.setOpaque(false); cab.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); cab.setAlignmentX(LEFT_ALIGNMENT);
        cab.add(new JLabel("<html><b>" + nomeAd + "</b> · CPF: " + proc.getCpfAdotante() + "<br>Data: " + proc.getDataPedido().format(DATA_BR) + "</html>"), BorderLayout.WEST);
        cab.add(tagProcesso(proc), BorderLayout.EAST);
        corpo.add(cab);
        corpo.add(Box.createVerticalStrut(10));

        if (proc.isAprovado() || Animal.STATUS_ADOTADO.equals(animal.getStatusAdocao())) {
            JPanel box = new JPanel(new BorderLayout());
            box.setBackground(new Color(209, 250, 229)); box.setBorder(new EmptyBorder(8, 10, 8, 10));
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64)); box.setAlignmentX(LEFT_ALIGNMENT);
            box.add(new JLabel("<html><b style='color:#047857;'>Adotado por " + nomeAd + "</b><br>CPF: " + proc.getCpfAdotante() + " · Endereço: " + endAd + "</html>"));
            corpo.add(box);
        } else {
            corpo.add(new JLabel("Endereço: " + endAd));
        }

        corpo.add(Box.createVerticalStrut(12));
        corpo.add(subtitulo("Avaliação do perfil"));
        ProcessoAdocao av = proc;
        if (av != null) {
            corpo.add(linhaPar("Espaço adequado", av.isEspaco() ? "Sim" : "Não", av.isEspaco() ? VERDE : VERMELHO));
            corpo.add(linhaPar("Tempo disponível", av.isTempo() ? "Sim" : "Não", av.isTempo() ? VERDE : VERMELHO));
            corpo.add(linhaPar("Autorização do local", av.getAutorizacao(), av.getAutorizacao().equals("Sim") ? VERDE : Color.ORANGE.darker()));
            corpo.add(linhaPar("Família de acordo", av.isFamilia() ? "Sim" : "Não", av.isFamilia() ? VERDE : VERMELHO));
        }
        c.add(corpo, BorderLayout.NORTH);

        if (isEquipeOng()) {
            JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); rodape.setOpaque(false);
            rodape.setBorder(new EmptyBorder(14, 0, 0, 0));
            if (proc.isEmAnalise()) {
                rodape.add(btn("Recusar", Color.WHITE, TEXTO, e -> { recusarProcessoBanco(proc, animal); atualizarTela(); }));
                rodape.add(btn("Aprovar adoção", VERDE, Color.WHITE, e -> aprovarAdocao(animal, proc, ad, av)));
            } else if (proc.isRecusado() && Animal.STATUS_DISPONIVEL.equals(animal.getStatusAdocao())) {
                rodape.add(btn("+ Novo pedido", VERDE, Color.WHITE, e -> solicitarAdocao(animal, false)));
            }
            c.add(rodape, BorderLayout.SOUTH);
        }
        return c;
    }

    private void aprovarAdocao(Animal animal, ProcessoAdocao proc, Adotante ad, ProcessoAdocao av) {
        if (Animal.STATUS_ADOTADO.equals(animal.getStatusAdocao())) {
            JOptionPane.showMessageDialog(this, "Este animal já foi adotado!");
            return;
        }
        if (av != null && (!av.isEspaco() || !av.isTempo() || !av.isFamilia() || !av.getAutorizacao().equals("Sim"))) {
            if (JOptionPane.showConfirmDialog(this, "Há pendências no perfil do adotante. Aprovar mesmo assim?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        }
        if (ad != null) ad.avaliarPerfil();
        aprovarProcessoBanco(proc, animal);
        atualizarTela();
    }

    private void removerAnimal(Animal animal) {
        if (JOptionPane.showConfirmDialog(this, "Remover " + animal.getNome() + "?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            removerAnimalBanco(animal.getId());
            animalAtual = null;
            atualizarTela();
        }
    }

    private JPanel criarTelaSecundaria() {
        boolean isAdot = aba.equals("Adotantes");
        JPanel pag = paginaBase(aba, isAdot ? "+ Cadastrar adotante" : null, e -> cadastrarAdotante(true));
        JPanel lista = card();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));

        if (aba.equals("Prontuários")) {
            for (ProntuarioMedico pm : listarProntuarios()) {
                Animal a = buscarAnimal(pm.getIdAnimal());
                String vacinas = pm.getVacinas().isEmpty() ? "Nenhuma" : pm.getVacinas().toString();
                String ult = pm.getDataUltimaConsulta() != null ? pm.getDataUltimaConsulta().format(DATA_BR) : "-";
                JLabel link = new JLabel("Abrir ficha");
                link.setFont(new Font("Segoe UI", Font.BOLD, 14));
                link.setForeground(VERDE);
                link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                link.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { aba = "Animais"; animalAtual = a; atualizarTela(); } });
                lista.add(linhaLista("Prontuário #" + pm.getId() + " — " + (a != null ? a.getNome() : pm.getIdAnimal()) + " | Vacinas: " + vacinas + " | Última: " + ult, acoesDaLista(link, () -> editarProntuario(pm), () -> excluirProntuario(pm))));
            }
        } else if (isAdot) {
            for (Adotante ad : listarAdotantes()) {
                lista.add(linhaLista(ad.getNome() + " · CPF: " + ad.getCpf() + " · Endereço: " + ad.getEndereco(), acoesDaLista(null, () -> editarAdotante(ad), () -> excluirAdotante(ad))));
            }
        } else {
            for (ProcessoAdocao p : listarProcessos()) {
                if (adotanteLogado != null && !p.getCpfAdotante().equals(adotanteLogado.getCpf())) continue;
                Animal a = buscarAnimal(p.getIdAnimal());
                Adotante ad = buscarAdotante(p.getCpfAdotante());
                lista.add(linhaLista("Processo #" + p.getId() + " · Animal: " + (a != null ? a.getNome() : p.getIdAnimal()) + " · Adotante: " + (ad != null ? ad.getNome() : p.getCpfAdotante()) + " · " + p.getDataPedido().format(DATA_BR), tagProcesso(p)));
            }
        }
        pag.add(lista);
        return pag;
    }

    private JButton menuAcoes(Runnable editar, Runnable excluir) {
        JButton botao = new JButton("⋮");
        botao.setFont(new Font("SansSerif", Font.BOLD, 20));
        botao.setMargin(new Insets(0, 8, 0, 8));
        botao.setBorderPainted(false);
        botao.setContentAreaFilled(false);
        botao.setFocusPainted(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setToolTipText("Ações");
        botao.getAccessibleContext().setAccessibleName("Ações: editar ou excluir");
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("Editar");
        JMenuItem itemExcluir = new JMenuItem("Excluir");
        itemEditar.addActionListener(e -> editar.run());
        itemExcluir.addActionListener(e -> excluir.run());
        menu.add(itemEditar);
        menu.add(itemExcluir);
        botao.addActionListener(e -> menu.show(botao, 0, botao.getHeight()));
        return botao;
    }

    private JPanel acoesDaLista(JComponent link, Runnable editar, Runnable excluir) {
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        if (link != null) acoes.add(link);
        if (isEquipeOng()) acoes.add(menuAcoes(editar, excluir));
        return acoes;
    }

    private void editarAnimal(Animal animal) {
        if (!isEquipeOng()) return;
        JTextField nome = new JTextField(animal.getNome());
        JTextField especie = new JTextField(animal.getEspecie());
        JTextField idade = new JTextField(String.valueOf(animal.getIdade()));
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Nome:", nome, "Espécie / Raça:", especie, "Idade (anos):", idade},
                "Editar animal", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (nome.getText().isBlank() || especie.getText().isBlank()) throw new IllegalArgumentException("Informe nome e espécie.");
            Animal atualizado = new Animal(animal.getId(), nome.getText().trim(), especie.getText().trim(), Integer.parseInt(idade.getText().trim()));
            atualizado.editar(db);
            atualizarTela();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
    }

    private void editarAdotante(Adotante adotante) {
        if (!isEquipeOng()) return;
        JTextField nome = new JTextField(adotante.getNome());
        JTextField endereco = new JTextField(adotante.getEndereco());
        if (JOptionPane.showConfirmDialog(this, new Object[]{"CPF: " + adotante.getCpf(), "Nome:", nome, "Endereço:", endereco},
                "Editar adotante", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (!isNomeValido(nome.getText()) || endereco.getText().isBlank()) throw new IllegalArgumentException("Informe nome válido e endereço.");
            new Adotante(adotante.getCpf(), nome.getText().trim(), endereco.getText().trim(), adotante.getSenha()).editar(db);
            atualizarTela();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
    }

    private void excluirAdotante(Adotante adotante) {
        if (!isEquipeOng()) return;
        if (JOptionPane.showConfirmDialog(this, "Excluir o cadastro de " + adotante.getNome() + "?", "Excluir adotante",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            for (ProcessoAdocao processo : listarProcessos()) {
                if (processo.getCpfAdotante().equals(adotante.getCpf())) {
                    JOptionPane.showMessageDialog(this, "Não é possível excluir este adotante porque há pedidos de adoção vinculados ao cadastro.");
                    return;
                }
            }
            Adotante.remover(db, adotante.getCpf());
            atualizarTela();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
    }

    private void editarProntuario(ProntuarioMedico prontuario) {
        if (!isEquipeOng()) return;
        JPanel campos = new JPanel();
        campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
        campos.add(new JLabel("Vacinas (uma por linha):"));
        JTextArea vacinas = new JTextArea(String.join("\n", prontuario.getVacinas()), 4, 35);
        campos.add(new JScrollPane(vacinas));
        java.util.List<JTextField> diagnosticos = new java.util.ArrayList<>();
        java.util.List<JTextField> prescricoes = new java.util.ArrayList<>();
        for (ConsultaVeterinaria consulta : prontuario.getConsultas()) {
            campos.add(new JLabel("Consulta " + consulta.getData().format(DATA_BR) + " — " + consulta.getCrmVeterinario()));
            JTextField diagnostico = new JTextField(consulta.getDiagnostico());
            JTextField prescricao = new JTextField(consulta.getPrescricao());
            diagnosticos.add(diagnostico);
            prescricoes.add(prescricao);
            campos.add(new JLabel("Diagnóstico:")); campos.add(diagnostico);
            campos.add(new JLabel("Prescrição:")); campos.add(prescricao);
        }
        JScrollPane scroll = new JScrollPane(campos);
        scroll.setPreferredSize(new Dimension(450, 350));
        if (JOptionPane.showConfirmDialog(this, scroll, "Editar prontuário #" + prontuario.getId(),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            for (int i = 0; i < diagnosticos.size(); i++) {
                prontuario.getConsultas().get(i).setDiagnostico(diagnosticos.get(i).getText().trim());
                prontuario.getConsultas().get(i).setPrescricao(prescricoes.get(i).getText().trim());
            }
            java.util.List<String> novasVacinas = new java.util.ArrayList<>();
            for (String vacina : vacinas.getText().split("\\R")) if (!vacina.isBlank()) novasVacinas.add(vacina.trim());
            prontuario.editar(db, novasVacinas);
            atualizarTela();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
    }

    private void excluirProntuario(ProntuarioMedico prontuario) {
        if (!isEquipeOng()) return;
        if (JOptionPane.showConfirmDialog(this, "Excluir o prontuário #" + prontuario.getId()
                + "? Todas as vacinas e consultas deste prontuário serão apagadas. O animal será mantido.",
                "Excluir prontuário", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            ProntuarioMedico.remover(db, prontuario.getId());
            atualizarTela();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
    }

    private ProntuarioMedico obterOuCriarProntuario(int idAnimal) {
        ProntuarioMedico prontuario = buscarProntuario(idAnimal);
        if (prontuario == null) {
            ProntuarioMedico.cadastrar(db, idAnimal);
            prontuario = buscarProntuario(idAnimal);
        }
        return prontuario;
    }

    private JPanel linhaLista(String texto, JComponent direita) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA), new EmptyBorder(13, 4, 13, 4)));

        JLabel info = new JLabel(texto);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        row.add(info, BorderLayout.WEST);

        if (direita != null) {
            JPanel boxDir = new JPanel(new GridBagLayout());
            boxDir.setOpaque(false);
            boxDir.add(direita);
            row.add(boxDir, BorderLayout.EAST);
        }
        return row;
    }

    private void cadastrarEquipe() {
        JTextField nome = new JTextField(), login = new JTextField(), cargoEsp = new JTextField();
        JPasswordField senha = new JPasswordField();
        Object[] campos = {"Nome (apenas letras):", nome, "E-mail (Responsável) ou CRMV completo (Veterinário):", login, "Cargo ou Especialidade:", cargoEsp, "Senha (mín. 4):", senha};

        if (JOptionPane.showConfirmDialog(this, campos, "Novo Membro da Equipe", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (!isNomeValido(nome.getText()) || login.getText().isBlank() || senha.getPassword().length < 4) {
                JOptionPane.showMessageDialog(this, "Verifique os dados! Nome deve ter apenas letras e senha mín. 4 dígitos.");
                return;
            }
            try {
                if (!Veterinario.isLoginCrmv(login.getText())) {
                    String cargo = cargoEsp.getText().isBlank() ? "Gestor(a)" : cargoEsp.getText().trim();
                    salvarOngBanco(new ResponsavelOng(login.getText().trim(), nome.getText().trim(), cargo, new String(senha.getPassword())));
                } else {
                    salvarVetBanco(new Veterinario(login.getText().trim().toUpperCase(), nome.getText().trim(), cargoEsp.getText().trim(), new String(senha.getPassword())));
                }
                JOptionPane.showMessageDialog(this, "Cadastrado com sucesso!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        }
    }

    private void cadastrarAdotante(boolean atualizar) {
        JTextField cpf = new JTextField(), nome = new JTextField(), end = new JTextField();
        JPasswordField senha = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"CPF (11 números):", cpf, "Nome (apenas letras):", nome, "Endereço:", end, "Senha (mín. 4):", senha}, "Novo Adotante", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (!ProcessoAdocao.isCpfValido(cpf.getText().trim()) || !isNomeValido(nome.getText()) || end.getText().isBlank() || senha.getPassword().length < 4) {
                JOptionPane.showMessageDialog(this, "Verifique CPF (11 dígitos), nome (apenas letras), endereço e senha!");
                return;
            }
            salvarAdotanteBanco(new Adotante(cpf.getText().trim(), nome.getText().trim(), end.getText().trim(), new String(senha.getPassword())));
            JOptionPane.showMessageDialog(this, "Adotante cadastrado!");
            if (atualizar) atualizarTela();
        }
    }

    private void solicitarAdocao(Animal animal, boolean peloAdotante) {
        JTextField cpf = new JTextField();
        JComboBox<String> esp = new JComboBox<>(new String[]{"Sim", "Não"}), tmp = new JComboBox<>(new String[]{"Sim", "Não"});
        JComboBox<String> aut = new JComboBox<>(new String[]{"Sim", "Pendente", "Não"}), fam = new JComboBox<>(new String[]{"Sim", "Não"});

        Object[] campos = peloAdotante
                ? new Object[]{"Possui espaço adequado?", esp, "Tem tempo disponível?", tmp, "Autorização do local:", aut, "Família de acordo?", fam}
                : new Object[]{"CPF do adotante já cadastrado (11 números):", cpf, "Espaço adequado?", esp, "Tempo disponível?", tmp, "Autorização do local:", aut, "Família de acordo?", fam};

        if (JOptionPane.showConfirmDialog(this, campos, "Solicitar adoção - " + animal.getNome(), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            String cpfFinal = peloAdotante ? adotanteLogado.getCpf() : cpf.getText().trim();
            if (!ProcessoAdocao.isCpfValido(cpfFinal)) { JOptionPane.showMessageDialog(this, "CPF inválido!"); return; }
            if (buscarAdotante(cpfFinal) == null) {
                JOptionPane.showMessageDialog(this, "Adotante não cadastrado. Faça o cadastro com uma senha escolhida antes de registrar o pedido.");
                return;
            }
            ProcessoAdocao av = new ProcessoAdocao(0, cpfFinal, animal.getId());
            av.setAvaliacao(esp.getSelectedItem().equals("Sim"), tmp.getSelectedItem().equals("Sim"), (String) aut.getSelectedItem(), fam.getSelectedItem().equals("Sim"));
            solicitarAdocaoBanco(animal, av);
            atualizarTela();
        }
    }

    private void cadastrarAnimal() {
        JTextField nome = new JTextField(), esp = new JTextField(), idade = new JTextField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Nome:", nome, "Espécie / Raça (ex: Cão (SRD)):", esp, "Idade (anos):", idade}, "Cadastrar animal", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                salvarAnimalBanco(nome.getText().trim(), esp.getText().trim(), Integer.parseInt(idade.getText().trim()));
                atualizarTela();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        }
    }

    private void registrarConsulta(Animal animal) {
        if (vetLogado == null) {
            JOptionPane.showMessageDialog(this, "Entre com um login de Veterinário para registrar consultas.");
            return;
        }
        JTextField diag = new JTextField(), presc = new JTextField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Veterinário: " + vetLogado.getNome(), "Diagnóstico:", diag, "Prescrição:", presc}, "Nova consulta", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                ConsultaVeterinaria c = vetLogado.realizarConsulta(obterOuCriarProntuario(animal.getId()));
                c.setDiagnostico(diag.getText());
                vetLogado.prescrever(c, presc.getText());
                c.registrar();
                salvarConsultaBanco(c);
                atualizarTela();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage()); }
        }
    }

    private JPanel paginaBase(String titulo, String txtBtn, ActionListener acao) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(FUNDO);
        p.add(larguraFixa());
        JPanel topo = new JPanel(new BorderLayout()); topo.setOpaque(false); topo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); topo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel t = new JLabel(titulo); t.setFont(new Font("Segoe UI", Font.BOLD, 22)); topo.add(t, BorderLayout.WEST);
        if (txtBtn != null) topo.add(btn(txtBtn, VERDE, Color.WHITE, acao), BorderLayout.EAST);
        p.add(topo); p.add(Box.createVerticalStrut(16));
        return p;
    }

    private JPanel larguraFixa() {
        JPanel p = new JPanel(); p.setOpaque(false);
        p.setPreferredSize(new Dimension(920, 0)); p.setMaximumSize(new Dimension(920, 0)); p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private JPanel card() {
        JPanel c = new JPanel(); c.setBackground(Color.WHITE); c.setAlignmentX(LEFT_ALIGNMENT);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDA, 1), new EmptyBorder(18, 18, 18, 18)));
        return c;
    }

    private JButton btn(String txt, Color bg, Color fg, ActionListener acao) {
        JButton b = new BotaoArredondado(txt, bg, fg); b.addActionListener(acao); return b;
    }

    private JLabel subtitulo(String txt) {
        JLabel l = new JLabel(txt); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(CINZA); return l;
    }

    private JPanel linhaPar(String esq, String dir, Color corDir) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26)); p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(new JLabel(esq), BorderLayout.WEST);
        JLabel d = new JLabel(dir); d.setFont(new Font("Segoe UI", Font.BOLD, 12)); d.setForeground(corDir); p.add(d, BorderLayout.EAST);
        return p;
    }

    private JLabel tagAnimal(String st) {
        if ("EM_PROCESSO".equals(st)) return new EtiquetaArredondada("Em processo", new Color(254, 243, 199), new Color(180, 83, 9));
        if (Animal.STATUS_ADOTADO.equals(st)) return new EtiquetaArredondada("Adotado", new Color(237, 233, 254), new Color(109, 40, 217));
        return new EtiquetaArredondada("Disponível", new Color(209, 250, 229), new Color(4, 120, 87));
    }

    private JLabel tagProcesso(ProcessoAdocao p) {
        if (p.isAprovado()) return new EtiquetaArredondada(p.getStatusFormatado(), new Color(209, 250, 229), new Color(4, 120, 87));
        if (p.isRecusado()) return new EtiquetaArredondada(p.getStatusFormatado(), new Color(254, 226, 226), VERMELHO);
        return new EtiquetaArredondada(p.getStatusFormatado(), new Color(254, 243, 199), new Color(180, 83, 9));
    }

    static class BotaoArredondado extends JButton {
        BotaoArredondado(String txt, Color bg, Color fg) {
            super(txt); setBackground(bg); setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(7, 14, 7, 14));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            if (Color.WHITE.equals(getBackground())) { g2.setColor(BORDA); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class EtiquetaArredondada extends JLabel {
        EtiquetaArredondada(String txt, Color bg, Color fg) {
            super(txt); setBackground(bg); setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private ResponsavelOng autenticarOngBanco(String email, String senha) {
        ResponsavelOng r = ResponsavelOng.buscarPorEmail(db, email);
        return r != null && r.login(email, senha) ? r : null;
    }

    private Veterinario autenticarVetBanco(String crmv, String senha) {
        return Veterinario.autenticar(db, crmv, senha);
    }

    private Adotante autenticarAdotanteBanco(String cpf, String senha) {
        Adotante a = Adotante.buscarPorCpf(db, cpf);
        return a != null && a.login(cpf, senha) ? a : null;
    }

    private void salvarOngBanco(ResponsavelOng r) { r.cadastrar(db); }
    private void salvarVetBanco(Veterinario v) { v.cadastrar(db); }
    private void salvarAdotanteBanco(Adotante a) { a.cadastrar(db); }
    private void salvarAnimalBanco(String nome, String especie, int idade) { new Animal(0, nome, especie, idade).cadastrar(db); }
    private void removerAnimalBanco(int idAnimal) { Animal.remover(db, idAnimal); }
    private void salvarVacinaBanco(int idAnimal, String vacina) { obterOuCriarProntuario(idAnimal).salvarVacina(db, vacina); }
    private void salvarConsultaBanco(ConsultaVeterinaria consulta) { consulta.cadastrar(db); }

    private void solicitarAdocaoBanco(Animal animal, ProcessoAdocao pedido) {
        pedido.cadastrar(db);
        animal.atualizarStatus("EM_PROCESSO");
    }

    private void aprovarProcessoBanco(ProcessoAdocao proc, Animal animal) {
        proc.finalizar(db, true);
        animal.atualizarStatus(Animal.STATUS_ADOTADO);
    }

    private void recusarProcessoBanco(ProcessoAdocao proc, Animal animal) {
        proc.finalizar(db, false);
        animal.atualizarStatus(Animal.STATUS_DISPONIVEL);
    }

    private List<Animal> listarAnimais() { return Animal.listar(db); }
    private List<ProntuarioMedico> listarProntuarios() { return ProntuarioMedico.listar(db); }
    private List<Adotante> listarAdotantes() { return Adotante.listar(db); }
    private List<ProcessoAdocao> listarProcessos() { return ProcessoAdocao.listar(db); }

    private Animal buscarAnimal(int id) { return Animal.buscarPorId(db, id); }
    private ProntuarioMedico buscarProntuario(int idAnimal) { return ProntuarioMedico.buscarPorAnimal(db, idAnimal); }
    private Adotante buscarAdotante(String cpf) { return Adotante.buscarPorCpf(db, cpf); }
    private ProcessoAdocao buscarProcessoAnimal(int idAnimal) { return ProcessoAdocao.buscarPorAnimal(db, idAnimal); }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, erro) -> {
            erro.printStackTrace();
            Runnable mostrar = () -> JOptionPane.showMessageDialog(null, erro.getMessage(), "Erro no sistema", JOptionPane.ERROR_MESSAGE);
            if (SwingUtilities.isEventDispatchThread()) mostrar.run();
            else SwingUtilities.invokeLater(mostrar);
        });
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
