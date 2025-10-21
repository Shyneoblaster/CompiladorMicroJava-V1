package Compilador;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Interface extends JFrame {

    private JTextArea txtProg, txtTokens, txtError, txtParser, txtSemantico, txtCI, txtCO;
    private JButton btnParser, btnTokens, btnSintactico, btnTablaSimbolos, btnIntermedio, btnObjeto;
    private JMenuBar menuBar;
    private JMenu menuArchivo;
    private JMenuItem itemNuevo, itemAbrir, itemGuardarComo, itemSalir;

    public Interface() {
        setTitle("Compilador de la gramática MicroJava");
        setSize(900, 600);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menú
        menuBar = new JMenuBar();
        menuArchivo = new JMenu("Archivo");

        itemNuevo = new JMenuItem("Nuevo");
        itemAbrir = new JMenuItem("Abrir");
        itemGuardarComo = new JMenuItem("Guardar Como");
        itemSalir = new JMenuItem("Salir");

        // Acciones
        itemNuevo.addActionListener(e -> limpiarCampos());
        itemAbrir.addActionListener(e -> abrirArchivo());
        itemGuardarComo.addActionListener(e -> guardarArchivo());
        itemSalir.addActionListener(e -> System.exit(0));

        menuArchivo.add(itemNuevo);
        menuArchivo.add(itemAbrir);
        menuArchivo.add(itemGuardarComo);
        menuArchivo.addSeparator();
        menuArchivo.add(itemSalir);

        menuBar.add(menuArchivo);
        setJMenuBar(menuBar);

        // Panel superior
        JPanel panelSuperior = new JPanel(new GridLayout(1, 3, 10, 10));

        txtProg = crearPanelConTitulo("Programa fuente", panelSuperior);
        txtTokens = crearPanelConTitulo("Tokens", panelSuperior);

        // Panel derecho dentro del superior
        JPanel panelDerecho = new JPanel(new GridLayout(3, 1, 10, 10));
        txtError = crearPanelConTitulo("Errores", panelDerecho);
        txtParser = crearPanelConTitulo("Parser", panelDerecho);
        txtSemantico = crearPanelConTitulo("Semantico", panelDerecho);

        panelSuperior.add(panelDerecho);

        // Panel inferior
        JPanel panelInferior = new JPanel(new GridLayout(1, 2, 10, 10));

        txtCI = crearPanelConTitulo("Codigo Intermedio", panelInferior);
        txtCO = crearPanelConTitulo("Codigo Objeto", panelInferior);

        // Panel principal
        JPanel panelPrincipal = new JPanel(new GridLayout(2, 1, 10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelPrincipal.add(panelSuperior);
        panelPrincipal.add(panelInferior);

        add(panelPrincipal);

        setVisible(true);
    }

    private JTextArea crearPanelConTitulo(String titulo, JPanel contenedor) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea txtArea = new JTextArea();
        txtArea.setFont(new Font("Monospaced", Font.PLAIN, 24));
        JScrollPane scroll = new JScrollPane(txtArea);
        JLabel lbl = new JLabel(titulo, SwingConstants.CENTER);

        switch (titulo){

            case "Programa fuente":
                lbl.setPreferredSize(new Dimension(100, 30));
                txtArea.addKeyListener(new java.awt.event.KeyAdapter() {
                    @Override
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        reiniciarTokens();
                    }
                    private void reiniciarTokens() {
                        btnParser.setEnabled(false);
                        btnSintactico.setEnabled(false);
                        btnTablaSimbolos.setEnabled(false);
                        btnIntermedio.setEnabled(false);
                        txtTokens.setText("");
                        txtParser.setText("");
                        txtSemantico.setText("");
                        txtCI.setText("");
                    }
                });
                panel.add(lbl, BorderLayout.NORTH);
                break;

            case "Tokens":
                btnTokens = new JButton("Tokens");
                btnTokens.setPreferredSize(new Dimension(90, 30));
                btnTokens.addActionListener(e -> {
                    try {

                        String codigo = txtProg.getText();

                        Scanner scanner = new Scanner(codigo);
                        java.util.List<Token> tokens = scanner.tokenize();


                        StringBuilder sb = new StringBuilder();
                        for (Token t : tokens) {
                            sb.append(t).append("\n");
                        }

                        txtArea.setText(sb.toString());
                        txtError.setText("");
                        btnParser.setEnabled(true);
                    } catch (Exception ex) {
                        txtError.setText("Error en análisis léxico:\n" + ex.getMessage());
                        txtTokens.setText("");
                    }
                });
                panel.add(btnTokens, BorderLayout.NORTH);
                break;

            case "Parser":
                btnParser = new JButton("Parser");
                btnParser.setEnabled(false);
                btnParser.setPreferredSize(new Dimension(90, 30));
                btnParser.addActionListener(e -> {
                    try {
                        Scanner scanner = new Scanner(txtProg.getText());
                        java.util.List<Token> tokens = scanner.tokenize();

                        Parser parser = new Parser();
                        parser.Parser((ArrayList<Token>) tokens);

                        txtArea.setText("Parser ejecutado con éxito.\nEl programa es válido.");
                        txtError.setText("");
                        btnSintactico.setEnabled(true);
                    } catch (Exception ex) {
                        txtArea.setText(ex.getMessage());
                    }
                });
                panel.add(btnParser, BorderLayout.NORTH);
                break;

            case "Semantico":
                btnSintactico = new JButton("Analisis Semantico");
                btnSintactico.setEnabled(false);
                btnSintactico.setPreferredSize(new Dimension(160, 30));
                btnSintactico.addActionListener(e -> {
                    try {
                        Scanner scanner = new Scanner(txtProg.getText());
                        java.util.List<Token> tokens = scanner.tokenize();

                        Semantico semantico = new Semantico();
                        semantico.Semantico((ArrayList<Token>) tokens);

                        txtArea.setText("Análisis semántico realizado con éxito.");
                        txtError.setText("");

                        btnTablaSimbolos.setEnabled(true);
                        btnIntermedio.setEnabled(true);
                    } catch (Exception ex) {
                        txtArea.setText(ex.getMessage());
                    }
                });
                panel.add(btnSintactico, BorderLayout.NORTH);

                btnTablaSimbolos = new JButton("Tabla de Símbolos");
                btnTablaSimbolos.setEnabled(false);
                btnTablaSimbolos.setPreferredSize(new Dimension(160, 30));
                btnTablaSimbolos.addActionListener(e -> {
                    try {
                        Scanner scanner = new Scanner(txtProg.getText());
                        java.util.List<Token> tokens = scanner.tokenize();

                        Semantico semantico = new Semantico();
                        semantico.Semantico((ArrayList<Token>) tokens);

                        ArrayList<Simbolo> simbolos = semantico.getSimbolos();

                        // Mostrar en JTable
                        JTable tabla = new JTable(new ModeloTablaSimbolo(simbolos));
                        JScrollPane scrollTabla = new JScrollPane(tabla);

                        JFrame frameTabla = new JFrame("Tabla de Símbolos");
                        frameTabla.setSize(600, 300);
                        frameTabla.add(scrollTabla);
                        frameTabla.setVisible(true);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Error al mostrar tabla:\n" + ex.getMessage());
                    }
                });
                panel.add(btnTablaSimbolos, BorderLayout.SOUTH);

                break;

            case "Codigo Intermedio":
                btnIntermedio = new JButton("Codigo Intermedio");
                btnIntermedio.setEnabled(false);
                btnIntermedio.setPreferredSize(new Dimension(80, 30));
                btnIntermedio.addActionListener(e -> {
                    try {
                        // Obtener el programa fuente
                        String codigoFuente = txtProg.getText();

                        // Tokenizar el programa
                        Scanner scanner = new Scanner(codigoFuente);
                        java.util.List<Token> tokens = scanner.tokenize();

                        // Analizar sintácticamente
                        Parser parser = new Parser();
                        parser.Parser((ArrayList<Token>) tokens);

                        // Realizar análisis semántico
                        Semantico semantico = new Semantico();
                        semantico.Semantico((ArrayList<Token>) tokens);

                        // Obtener la tabla de símbolos
                        HashMap<String, String> tablaSimbolos = semantico.getTablaSimbolos();

                        // Generar código intermedio
                        ArrayList<Triple> instrucciones = semantico.getInstruccionesIntermedias();
                        CodigoIntermedio codigoIntermedio = new CodigoIntermedio(tablaSimbolos);
                        String codigoGenerado = codigoIntermedio.generarCodigo(instrucciones);

                        // Mostrar el código intermedio en el área de texto
                        txtCI.setText(codigoGenerado);
                        txtError.setText("");
                    } catch (Exception ex) {
                        txtCI.setText("Error al generar código intermedio:\n" + ex.getMessage());
                    }
                });
                panel.add(btnIntermedio, BorderLayout.NORTH);
                break;

            case "Codigo Objeto":
                btnObjeto = new JButton("Codigo Objeto");
                btnObjeto.setEnabled(false);
                btnObjeto.setPreferredSize(new Dimension(80, 30));
                panel.add(btnObjeto, BorderLayout.NORTH);
                break;

            default:
                lbl.setPreferredSize(new Dimension(100, 30));
                panel.add(lbl, BorderLayout.NORTH);
                break;
        }

        panel.add(scroll, BorderLayout.CENTER);
        contenedor.add(panel);

        return txtArea;
    }

    private void limpiarCampos() {
        txtProg.setText("");
        txtTokens.setText("");
        txtError.setText("");
        txtCI.setText("");
        txtCO.setText("");
    }

    private void abrirArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                txtProg.read(br, null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al abrir archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                archivo = new File(archivo.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
                txtProg.write(bw);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivo", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Interface::new);
    }
}