package Compilador;

enum TokenType {
    PR,        // Palabra Reservada
    ID,        // Identificador
    NUM,    // Números enteros
    LBRACE, RBRACE, LPAREN, RPAREN,
    SEMI, ASSIGN, LT, PLUS, MINUS, TIMES,
    EOF
}
