package Compilador;

import java.util.*;

public class Scanner {
    private final String input;
    private int pos = 0;

    public Scanner(String input) {
        this.input = input;
    }

    private char peek() {
        if (pos >= input.length()) return '\0';
        return input.charAt(pos);
    }

    private char advance() {
        return input.charAt(pos++);
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            advance();
        }
    }

    public Token nextToken() {
        skipWhitespace();

        char current = peek();
        if (current == '\0') {
            return new Token(TokenType.PR, "FIN", 99);
        }

        // --- Identificadores o palabras reservadas ---
        if (Character.isLetter(current)) {
            StringBuilder sb = new StringBuilder();
            while (Character.isLetterOrDigit(peek())) {
                sb.append(advance());
            }
            String word = sb.toString();

            // Palabras reservadas
            switch (word) {
                case "class": return new Token(TokenType.PR, word, 1);
                case "boolean": return new Token(TokenType.PR, word, 2);
                case "int": return new Token(TokenType.PR, word, 3);
                case "while": return new Token(TokenType.PR, word, 4);
                case "true": return new Token(TokenType.PR, word, 5);
                case "false": return new Token(TokenType.PR, word, 6);
                case "print": return new Token(TokenType.PR, word, 7);
                default:
                    return new Token(TokenType.ID, word, 18);
            }
        }
            // --- Números enteros ---
            if (Character.isDigit(current)) {
                StringBuilder sb = new StringBuilder();
                while (Character.isDigit(peek())) {
                    sb.append(advance());
                }
                return new Token(TokenType.NUM, sb.toString(), 19);
            }

        // --- Símbolos ---
        switch (advance()) {
            case '{': return new Token(TokenType.LBRACE, "{", 8);
            case '}': return new Token(TokenType.RBRACE, "}", 9);
            case '(': return new Token(TokenType.LPAREN, "(", 10);
            case ')': return new Token(TokenType.RPAREN, ")", 11);
            case ';': return new Token(TokenType.SEMI, ";", 12);
            case '=': return new Token(TokenType.ASSIGN, "=", 13);
            case '<': return new Token(TokenType.LT, "<", 14);
            case '+': return new Token(TokenType.PLUS, "+", 15);
            case '-': return new Token(TokenType.MINUS, "-", 16);
            case '*': return new Token(TokenType.TIMES, "*", 17);
        }

        throw new RuntimeException("Caracter inesperado: " + current);
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (token.code != 99);
        return tokens;
    }
}

