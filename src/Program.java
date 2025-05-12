import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
public class Program {
    private static JFrame frame;
    private static JTextArea codeArea;
    private static JTextArea outputArea;
    private static JTabbedPane outputTabs;
    private static JTextArea scannerOutput;
    private static JTextArea parserOutput;
    private static JButton scanButton;
    private static JButton parseButton;
    private static JButton loadButton;
    private static JButton saveButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        // Set up the main frame
        frame = new JFrame("Project#2 Compiler - Scanner & Parser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);

        // Define colors
        Color babyPink = new Color(255, 182, 193);
        Color lavender = new Color(230, 230, 250);
        Color pastelPurple = new Color(221, 160, 221);
        Color pastelGreen = new Color(152, 251, 152);
        Color pastelBlue = new Color(173, 216, 230);

        // Set up the layout
        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout(10, 10));
        container.setBackground(lavender);

        // Set up the menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        frame.setJMenuBar(menuBar);

        // Set up the code area
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Source Code"));
        leftPanel.setBackground(lavender);

        codeArea = new JTextArea();
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeArea.setTabSize(4);
        codeArea.setBackground(Color.WHITE);
        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        leftPanel.add(codeScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(lavender);

        loadButton = new JButton("Load File");
        loadButton.setBackground(pastelBlue);
        saveButton = new JButton("Save File");
        saveButton.setBackground(pastelBlue);
        scanButton = new JButton("Scan");
        scanButton.setBackground(pastelGreen);
        parseButton = new JButton("Parse");
        parseButton.setBackground(pastelPurple);

        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(scanButton);
        buttonPanel.add(parseButton);

        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set up the output area with tabs
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        rightPanel.setBackground(lavender);

        outputTabs = new JTabbedPane();

        scannerOutput = new JTextArea();
        scannerOutput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scannerOutput.setEditable(false);
        JScrollPane scannerScrollPane = new JScrollPane(scannerOutput);

        parserOutput = new JTextArea();
        parserOutput.setFont(new Font("Monospaced", Font.PLAIN, 14));
        parserOutput.setEditable(false);
        JScrollPane parserScrollPane = new JScrollPane(parserOutput);

        outputTabs.addTab("Scanner Output", scannerScrollPane);
        outputTabs.addTab("Parser Output", parserScrollPane);

        rightPanel.add(outputTabs, BorderLayout.CENTER);

        // Add the panels to the main container
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(600);
        container.add(splitPane, BorderLayout.CENTER);

        // Event handlers
        openItem.addActionListener(e -> loadFile());
        saveItem.addActionListener(e -> saveFile());
        exitItem.addActionListener(e -> System.exit(0));

        loadButton.addActionListener(e -> loadFile());
        saveButton.addActionListener(e -> saveFile());
        scanButton.addActionListener(e -> scanCode());
        parseButton.addActionListener(e -> parseCode());

        // Display the frame
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                String content = Files.readString(selectedFile.toPath());
                codeArea.setText(content);
                JOptionPane.showMessageDialog(frame, "File loaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error loading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                Files.writeString(selectedFile.toPath(), codeArea.getText());
                JOptionPane.showMessageDialog(frame, "File saved successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void scanCode() {
        try {
            // Save code to a temporary file
            File tempFile = new File("temp_code.txt");
            Files.writeString(tempFile.toPath(), codeArea.getText());

            // Run the scanner
            Scanner scanner = new Scanner();
            List<Token> tokens = scanner.scanFile(tempFile.getPath());

            // Display scanner output
            scannerOutput.setText(scanner.getScannerOutput());
            outputTabs.setSelectedIndex(0); // Show the scanner tab

            JOptionPane.showMessageDialog(frame, "Scanning completed with " + scanner.getErrorCount() + " errors",
                    "Scan Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error during scanning: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void parseCode() {
        try {
            // First scan the code
            File tempFile = new File("temp_code.txt");
            Files.writeString(tempFile.toPath(), codeArea.getText());

            Scanner scanner = new Scanner();
            List<Token> tokens = scanner.scanFile(tempFile.getPath());

            // Display scanner output
            scannerOutput.setText(scanner.getScannerOutput());

            // Then parse if scanning completed successfully
            if (scanner.getErrorCount() == 0) {
                Parser parser = new Parser(tokens);
                parser.parseProgram();

                // Display parser output
                parserOutput.setText(parser.getParserOutput());
                outputTabs.setSelectedIndex(1); // Show the parser tab

                JOptionPane.showMessageDialog(frame, "Parsing completed", "Parse Complete", JOptionPane.INFORMATION_MESSAGE);
            } else {
                parserOutput.setText("Cannot parse due to scanner errors.\n\n" + scanner.getScannerOutput());
                outputTabs.setSelectedIndex(1); // Show the parser tab

                JOptionPane.showMessageDialog(frame, "Cannot parse - fix scanner errors first",
                        "Scan Errors", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error during parsing: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
