package Compilador;

import java.util.ArrayList;

public class Parser {

    private ArrayList<Token> tokens;
    private int pos = 0;
    private boolean encontrado;

    public void Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        ProdProgram();
    }

    private Token peek() {
        if (pos >= tokens.size()) return tokens.get(tokens.size() - 1); // EOF
        return tokens.get(pos);
    }

    private Token advance() {
        return tokens.get(pos++);
    }

    private boolean match(int code) {
        if (peek().code == code) {
            advance();
            return true;
        }
        return false;
    }

    // --- Program ::= class Identifier { Statement* } <EOF>
    private void ProdProgram() {
        if (match(1)) { // class
            if (match(18)) { // Identifier
                if (match(8)) { // {
                    ProdStatements(); // multiples statements
                    if (match(9)) { // }
                        if (match(99)) {
                            encontrado = true;
                        } else {
                            throw new RuntimeException("Se esperaba 'FIN' al final del programa.");
                        }
                    } else {
                        throw new RuntimeException("Se esperaba '}' al final del bloque.");
                    }
                } else {
                    throw new RuntimeException("Se esperaba '{' después del identificador de la clase.");
                }
            } else {
                throw new RuntimeException("Se esperaba un identificador después de 'class'.");
            }
        } else {
            throw new RuntimeException("El programa debe iniciar con 'class'.");
        }
    }

    // --- Statement* ::= (Statement)*
    private void ProdStatements() {
        while (peek().code == 4   // while
                || peek().code == 7   // print
                || peek().code == 18  // ID
                || peek().code == 2   // boolean
                || peek().code == 3)  // int
        {
            ProdStatement();
        }
    }

    // --- Statement ::= while (...) { Statement* }
    //                 | print ( Expression ) ;
    //                 | Identifier = Expression ;
    //                 | VarDeclaration
    private void ProdStatement() {
        if (match(4)) { // while
            if (match(10)) { // (
                ProdExpression();
                if (match(11)) { // )
                    if (match(8)) { // {
                        ProdStatements();
                        if (!match(9)) throw new RuntimeException("Se esperaba '}' en while.");
                    } else {
                        throw new RuntimeException("Se esperaba '{' en while.");
                    }
                } else {
                    throw new RuntimeException("Se esperaba ')' en while.");
                }
            } else {
                throw new RuntimeException("Se esperaba '(' en while.");
            }

        } else if (match(7)) { // print
            if (match(10)) { // (
                ProdExpression();
                if (match(11)) { // )
                    if (!match(12)) throw new RuntimeException("Se esperaba ';' en print.");
                } else {
                    throw new RuntimeException("Se esperaba ')' en print.");
                }
            } else {
                throw new RuntimeException("Se esperaba '(' en print.");
            }

        } else if (peek().code == 18) { // ID asignación
            advance();
            if (match(13)) { // =
                ProdExpression();
                if (!match(12)) throw new RuntimeException("Se esperaba ';' en asignación.");
            } else {
                throw new RuntimeException("Se esperaba '=' en asignación.");
            }

        } else if (peek().code == 2 || peek().code == 3) {
            ProdVarDeclaration();

        } else {
            throw new RuntimeException("Sentencia inesperada: " + peek());
        }
    }

    // --- VarDeclaration ::= Type Identifier ;
    private void ProdVarDeclaration() {
        ProdType();
        if (match(18)) { // ID
            if (!match(12)) throw new RuntimeException("Se esperaba ';' en declaración de variable.");
        } else {
            throw new RuntimeException("Se esperaba identificador en declaración de variable.");
        }
    }

    // --- Type ::= boolean | int
    private void ProdType() {
        if (!(match(2) || match(3))) {
            throw new RuntimeException("Se esperaba 'boolean' o 'int'.");
        }
    }

    // --- Expression ::= Identifier (< | + | - | * ) Identifier
    //                  | true | false | Identifier | Integer
    private void ProdExpression() {
        if (match(18) || match(19)) {
            while (peek().code == 14 || peek().code == 15 || peek().code == 16 || peek().code == 17) {
                advance(); // consumir operador
                if (!match(18) && !match(19)) {
                    throw new RuntimeException("Se esperaba identificador o número después del operador.");
                }
            }
        }else if(match(5) || match(6)){
            return;
        }else{
            throw new RuntimeException("Expresión inesperada: " + peek());
        }
    }
}
