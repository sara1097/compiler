import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Scanner {
    private static Map<String, String> keywords = new HashMap<>();
    private List<Token> tokens = new ArrayList<>();
    private int errorCount = 0;
    private List<String> errors = new ArrayList<>();

    static {
        keywords.put("Type", "Class");
        keywords.put("DerivedFrom", "Inheritance");
        keywords.put("TrueFor", "Condition");
        keywords.put("Else", "Condition");
        keywords.put("Ity", "Integer");
        keywords.put("Sity", "SInteger");
        keywords.put("Cwq", "Character");
        keywords.put("CwqSequence", "String");
        keywords.put("Ifity", "Float");
        keywords.put("Sifity", "SFloat");
        keywords.put("Valueless", "Void");
        keywords.put("Logical", "Boolean");
        keywords.put("Endthis", "Break");
        keywords.put("However", "Loop");
        keywords.put("When", "Loop");
        keywords.put("Respondwith", "Return");
        keywords.put("Srap", "Struct");
        keywords.put("Scan", "Switch");
        keywords.put("Conditionof", "Switch");
        keywords.put("Require", "Inclusion");
    }

    public List<Token> scanFile(String filePath) {
        tokens.clear();
        errorCount = 0;
        errors.clear();

        try {
            processFile(filePath);
            return tokens;
        } catch (IOException e) {
            errors.add("Error reading file: " + e.getMessage());
            errorCount++;
            return tokens;
        }
    }

    private void processFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            errors.add("File not found: " + filePath);
            errorCount++;
            return;
        }

        Stack<String> fileStack = new Stack<>();
        fileStack.push(filePath);

        Set<String> processedFiles = new HashSet<>();
        processedFiles.add(filePath);

        while (!fileStack.isEmpty()) {
            String currentFile = fileStack.pop();
            scanFileContent(currentFile, fileStack, processedFiles);
        }
    }

    private void scanFileContent(String filePath, Stack<String> fileStack, Set<String> processedFiles) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        boolean inMultilineComment = false;

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber).trim();

            // Skip empty lines
            if (line.isEmpty()) continue;

            // Check for require command at the beginning of the line
            if (line.startsWith("Require") && line.contains(".txt")) {
                Matcher fileMatcher = Pattern.compile("Require\\s*\\(\\s*([\\w.]+)\\s*\\)").matcher(line);
                if (fileMatcher.find()) {
                    String includeFile = fileMatcher.group(1);
                    if (!processedFiles.contains(includeFile)) {
                        File file = new File(includeFile);
                        if (file.exists()) {
                            fileStack.push(includeFile);
                            processedFiles.add(includeFile);
                        }
                    }
                    continue;
                }
            }

            // Process multiline comments
            if (inMultilineComment) {
                int endCommentIndex = line.indexOf(">/");
                if (endCommentIndex != -1) {
                    tokens.add(new Token(">/ (Comment End)", "Comment", lineNumber + 1));
                    inMultilineComment = false;
                    line = line.substring(endCommentIndex + 2).trim();
                    if (line.isEmpty()) continue;
                } else {
                    continue; // Skip this line as it's part of a comment
                }
            }

            // Process the line
            int index = 0;
            while (index < line.length()) {
                // Skip whitespace
                while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
                    index++;
                }
                if (index >= line.length()) break;

                // Check for start of multiline comment
                if (index + 1 < line.length() && line.charAt(index) == '/' && line.charAt(index + 1) == '<') {
                    tokens.add(new Token("/< (Comment Start)", "Comment", lineNumber + 1));
                    inMultilineComment = true;
                    index += 2;
                    int endCommentIndex = line.indexOf(">/ ", index);
                    if (endCommentIndex != -1) {
                        tokens.add(new Token(">/ (Comment End)", "Comment", lineNumber + 1));
                        inMultilineComment = false;
                        index = endCommentIndex + 2;
                    } else {
                        break; // Rest of the line is comment
                    }
                    continue;
                }

                // Check for single line comment
                if (index + 1 < line.length() && line.charAt(index) == '/' && line.charAt(index + 1) == '*') {
                    tokens.add(new Token("/* (Comment)", "Comment", lineNumber + 1));
                    break; // Rest of the line is comment
                }

                // Check for start and end symbols
                if (line.charAt(index) == '@' || line.charAt(index) == '^') {
                    tokens.add(new Token(String.valueOf(line.charAt(index)), "Start Symbol", lineNumber + 1));
                    index++;
                    continue;
                }
                if (line.charAt(index) == '$' || line.charAt(index) == '#') {
                    tokens.add(new Token(String.valueOf(line.charAt(index)), "End Symbol", lineNumber + 1));
                    index++;
                    continue;
                }

                // Check for braces, brackets, parentheses
                if (line.charAt(index) == '{' || line.charAt(index) == '}' ||
                        line.charAt(index) == '[' || line.charAt(index) == ']' ||
                        line.charAt(index) == '(' || line.charAt(index) == ')') {
                    tokens.add(new Token(String.valueOf(line.charAt(index)), "Braces", lineNumber + 1));
                    index++;
                    continue;
                }

                // Check for operators
                if (index + 1 < line.length()) {
                    String twoChars = line.substring(index, index + 2);
                    if (twoChars.equals("==") || twoChars.equals("!=") ||
                            twoChars.equals("<=") || twoChars.equals(">=") ||
                            twoChars.equals("&&") || twoChars.equals("||") ||
                            twoChars.equals("->")) {

                        String opType = "";
                        if (twoChars.equals("==") || twoChars.equals("!=") ||
                                twoChars.equals("<=") || twoChars.equals(">=")) {
                            opType = "Relational Operator";
                        } else if (twoChars.equals("&&") || twoChars.equals("||")) {
                            opType = "Logic Operator";
                        } else if (twoChars.equals("->")) {
                            opType = "Access Operator";
                        }

                        tokens.add(new Token(twoChars, opType, lineNumber + 1));
                        index += 2;
                        continue;
                    }
                }

                // Check for single operators
                if (line.charAt(index) == '+' || line.charAt(index) == '-' ||
                        line.charAt(index) == '*' || line.charAt(index) == '/') {
                    tokens.add(new Token(String.valueOf(line.charAt(index)), "Arithmetic Operation", lineNumber + 1));
                    index++;
                    continue;
                }

                if (line.charAt(index) == '=' || line.charAt(index) == '<' ||
                        line.charAt(index) == '>' || line.charAt(index) == '~') {
                    String opType = line.charAt(index) == '=' ? "Assignment Operator" :
                            (line.charAt(index) == '~' ? "Logic Operator" : "Relational Operator");
                    tokens.add(new Token(String.valueOf(line.charAt(index)), opType, lineNumber + 1));
                    index++;
                    continue;
                }

                // Check for semicolon
                if (line.charAt(index) == ';') {
                    tokens.add(new Token(";", "Semicolon", lineNumber + 1));
                    index++;
                    continue;
                }

                // Check for comma
                if (line.charAt(index) == ',') {
                    tokens.add(new Token(",", "Comma", lineNumber + 1));
                    index++;
                    continue;
                }

                // Check for string literals
                if (line.charAt(index) == '"') {
                    int endQuote = line.indexOf('"', index + 1);
                    if (endQuote != -1) {
                        String str = line.substring(index, endQuote + 1);
                        tokens.add(new Token(str, "String Literal", lineNumber + 1));
                        index = endQuote + 1;
                    } else {
                        reportError("Unclosed string literal", lineNumber + 1);
                        index = line.length(); // Skip to end of line
                    }
                    continue;
                }

                // Check for character literals
                if (line.charAt(index) == '\'') {
                    int endQuote = line.indexOf('\'', index + 1);
                    if (endQuote != -1) {
                        String charLiteral = line.substring(index, endQuote + 1);
                        tokens.add(new Token(charLiteral, "Character Literal", lineNumber + 1));
                        index = endQuote + 1;
                    } else {
                        reportError("Unclosed character literal", lineNumber + 1);
                        index = line.length(); // Skip to end of line
                    }
                    continue;
                }

                // Check for numbers
                if (Character.isDigit(line.charAt(index))) {
                    int start = index;
                    while (index < line.length() && (Character.isDigit(line.charAt(index)) || line.charAt(index) == '.')) {
                        index++;
                    }
                    String num = line.substring(start, index);
                    tokens.add(new Token(num, "Constant", lineNumber + 1));
                    continue;
                }

                // Check for identifiers and keywords
                if (Character.isLetter(line.charAt(index)) || line.charAt(index) == '_') {
                    int start = index;
                    while (index < line.length() && (Character.isLetterOrDigit(line.charAt(index)) || line.charAt(index) == '_')) {
                        index++;
                    }
                    String word = line.substring(start, index);

                    // Check if it's a keyword
                    if (keywords.containsKey(word)) {
                        tokens.add(new Token(word, keywords.get(word), lineNumber + 1));
                    } else {
                        tokens.add(new Token(word, "Identifier", lineNumber + 1));
                    }
                    continue;
                }

                // Unknown character
                reportError("Unknown character: " + line.charAt(index), lineNumber + 1);
                index++;
            }
        }
    }

    private void reportError(String message, int lineNumber) {
        String errorMsg = "Line #: " + lineNumber + " Error in Token Text: " + message;
        errors.add(errorMsg);
        System.out.println(errorMsg);
        errorCount++;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getScannerOutput() {
        StringBuilder output = new StringBuilder();
        for (Token token : tokens) {
            output.append(token.toString()).append("\n");
        }
        for (String error : errors) {
            output.append(error).append("\n");
        }
        output.append("Total NO of errors: ").append(errorCount);
        return output.toString();
    }
}