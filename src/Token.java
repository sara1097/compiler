public class Token {
    private String text;
    private String type;
    private int line;

    public Token(String text, String type, int line) {
        this.text = text;
        this.type = type;
        this.line = line;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "Line #: " + line + " Token Text: " + text + " Token Type: " + type;
    }
}
