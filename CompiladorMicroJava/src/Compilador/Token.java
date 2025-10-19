package Compilador;

class Token {
    TokenType type;
    String value;
    int code;

    public Token(TokenType type, String value, int code) {
        this.type = type;
        this.value = value;
        this.code = code;
    }

    @Override
    public String toString() {
        return "(" + type + ", " + value + ")";
    }
}