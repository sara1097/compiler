import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Scanner {
    private List<Token> tokens;
    private int errorCount;
    private StringBuilder output;

    private static final Map<String, String> KEYWORDS = new HashMap<>();
    private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "=", "==", "<", ">", "!=", "<=", ">=", "&&", "||", "~", "->");
    private static final Set<String> SYMBOLS = Set.of("{", "}", "(", ")", "[", "]", ",", ";", "@|", "$|#", "^");

    private boolean insideMultilineComment = false;

    static {
        KEYWORDS.put("Type", "Class");
        KEYWORDS.put("Class", "Class");
        KEYWORDS.put("DerivedFrom", "Inheritance");
        KEYWORDS.put("TrueFor", "Condition");
        KEYWORDS.put("Else", "Condition");
        KEYWORDS.put("Ity", "Integer");
        KEYWORDS.put("Sity", "SInteger");
        KEYWORDS.put("Cwq", "Character");
        KEYWORDS.put("CwqSequence", "String");
        KEYWORDS.put("Ifity", "Float");
        KEYWORDS.put("Sifity", "SFloat");
        KEYWORDS.put("Valueless", "Void");
        KEYWORDS.put("Logical", "Boolean");
        KEYWORDS.put("Endthis", "Break");
        KEYWORDS.put("However", "Loop");
        KEYWORDS.put("When", "Loop");
        KEYWORDS.put("Respondwith", "Return");
        KEYWORDS.put("Srap", "Struct");
        KEYWORDS.put("Scan", "Switch");
        KEYWORDS.put("Conditionof", "Switch");
        KEYWORDS.put("Require", "Inclusion");
        KEYWORDS.put("{", "Braces");
        KEYWORDS.put("}", "Braces");
        KEYWORDS.put("[", "Braces");
        KEYWORDS.put("]", "Braces");
        KEYWORDS.put("(", "Braces");
        KEYWORDS.put(")", "Braces");
        KEYWORDS.put(";", "Line Delimiter");
        KEYWORDS.put(",", "Separator");
    }


    public Scanner() {
        tokens = new ArrayList<>();
        output = new StringBuilder();
        errorCount = 0;
    }

    public List<Token> scanFile(String filePath) throws IOException {
        tokens.clear();
        output.setLength(0);
        errorCount = 0;
        insideMultilineComment = false;

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            scanLine(line, lineNumber);
        }

        reader.close();
        return tokens;
    }

    private void scanLine(String line, int lineNumber) {
        // Handle multiline comments
        if (insideMultilineComment) {
            if (line.contains(">/")) {
                insideMultilineComment = false;
            }
            return;
        }

        if (line.contains("/<")) {
            insideMultilineComment = true;
            return;
        }

        // Remove single-line comment
        if (line.contains("/*")) {
            line = line.substring(0, line.indexOf("/*"));
        }

        // Split line by non-alphanumeric symbols (keep them using regex)
        Matcher matcher = Pattern.compile(
                "(@|\\^|#|\\$|->|==|!=|<=|>=|&&|\\|\\||[a-zA-Z_][a-zA-Z0-9_]*|\\d+|\\S)"
        ).matcher(line);


        while (matcher.find()) {
            String part = matcher.group();
            if (part.isBlank()) continue;

            Token token = identifyToken(part, lineNumber);
            tokens.add(token);
            output.append("Line ").append(lineNumber).append(": ").append(token).append("\n");
        }
    }

    private Token identifyToken(String word, int lineNumber) {
        if (KEYWORDS.containsKey(word)) {
            return new Token(word, KEYWORDS.get(word), lineNumber);
        }
        if (word.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return new Token(word, "Identifier", lineNumber);
        }

        if (word.matches("\\d+")) {
            return new Token(word, "Constant", lineNumber);
        }

        if (word.matches("\".*\"")) {
            return new Token(word, "String", lineNumber);
        }
        if (word.matches("'.'")) {
            return new Token(word, "Character", lineNumber);
        }

        if (word.equals("/*")) {
            return new Token(word, "Single Line Comment", lineNumber);
        }
        if (word.equals("/<")) {
            return new Token(word, "Multi Line Comment Start", lineNumber);
        }
        if (word.equals(">/")) {
            return new Token(word, "Multi Line Comment End", lineNumber);
        }

        if (word.equals("@") || word.equals("^")) {
            return new Token(word, "Start Symbol", lineNumber);
        }
        if (word.equals("#") || word.equals("$")) {
            return new Token(word, "End Symbol", lineNumber);
        }

        if ("+-*/".contains(word)) {
            return new Token(word, "Arithmetic Operation", lineNumber);
        }
        if ("&&||~".contains(word)) {
            return new Token(word, "Logic Operator", lineNumber);
        }
        if (word.matches("==|!=|<=|>=|<|>")) {
            return new Token(word, "Relational Operator", lineNumber);
        }

        if (word.equals("=")) {
            return new Token(word, "Assignment Operator", lineNumber);
        }
        if (word.equals("->")) {
            return new Token(word, "Access Operator", lineNumber);
        }

        if (word.equals(",") || word.equals("'") || word.equals("\"")) {
            return new Token(word, "Quotation Mark", lineNumber);
        }
        return new Token(word, "UNKNOWN", lineNumber);
    }
    public String getScannerOutput() {
        return output.toString();
    }

    public int getErrorCount() {
        return errorCount;
    }
}