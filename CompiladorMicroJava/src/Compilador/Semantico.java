package Compilador;

import java.util.HashMap;
import java.util.ArrayList;

public class Semantico {

    private ArrayList<Token> tokens;
    private int pos = 0;
    private boolean encontrado;

    // Tabla de símbolos (nombre → tipo)
    private HashMap<String, String> symbolTable = new HashMap<>();
    public HashMap<String, String> getTablaSimbolos() {
        return symbolTable;
    }
    // Add this field to the Semantico class
    private ArrayList<Triple> instruccionesIntermedias = new ArrayList<>();

    // Implement the getInstruccionesIntermedias method
    public ArrayList<Triple> getInstruccionesIntermedias() {
        return instruccionesIntermedias;
    }
    // Lista de símbolos para mostrar en JTable
    private ArrayList<Simbolo> simbolos = new ArrayList<>();

    // Contador de direcciones
    private int currentDir = 0;

    public void Semantico(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.encontrado = false;
        this.symbolTable.clear();
        this.simbolos.clear();
        this.currentDir = 0;
        ProdProgram();
    }

    public ArrayList<Simbolo> getSimbolos() {
        return simbolos;
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
                    ProdStatements(); // múltiples statements
                    if (match(9)) { // }
                        if (match(99)) { // EOF
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

    // --- Statement
    private void ProdStatement() {
        if (match(4)) { // while
            if (match(10)) { // (
                String tipoExpr = ProdExpression();
                if (!"boolean".equals(tipoExpr)) {
                    throw new RuntimeException("La condición del while debe ser booleana.");
                }
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
                String tipoExpr = ProdExpression();
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
            String varName = tokens.get(pos - 1).value;
            if (!symbolTable.containsKey(varName)) {
                throw new RuntimeException("Variable '" + varName + "' no declarada (uso en asignación).");
            }
            if (match(13)) { // =
                String tipoExpr = ProdExpression();
                String tipoVar = symbolTable.get(varName);
                if (!tipoVar.equals(tipoExpr)) {
                    throw new RuntimeException("Incompatibilidad de tipos en asignación: variable '" + varName +
                            "' es " + tipoVar + " y se intenta asignar " + tipoExpr + ".");
                }
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
        String tipo = ProdType(); // ""boolean" o "int"
        if (match(18)) { // ID
            String varName = tokens.get(pos - 1).value;
            if (symbolTable.containsKey(varName)) {
                throw new RuntimeException("Variable '" + varName + "' ya declarada previamente.");
            }

            // Valor inicial por defecto
            String valorInicial = tipo.equals("int") ? "0" : "false";

            // Calcular dirección
            int direccion = currentDir;
            if (tipo.equals("int")) {
                currentDir += 2;
            } else if (tipo.equals("boolean")) {
                currentDir += 1;
            }

            // Guardar en tabla semántica
            symbolTable.put(varName, tipo);

            // Guardar en tabla de símbolos para JTable
            simbolos.add(new Simbolo("IDENTIFICADOR", varName, tipo, valorInicial, direccion));

            if (!match(12)) throw new RuntimeException("Se esperaba ';' en declaración de variable.");
        } else {
            throw new RuntimeException("Se esperaba identificador en declaración de variable.");
        }
    }

    // --- Type ::= boolean | int
    private String ProdType() {
        if (match(2)) return "boolean";
        if (match(3)) return "int";
        throw new RuntimeException("Se esperaba 'boolean' o 'int'.");
    }

    // --- Expression
    // --- Expression ::= SimpleExpression (RelOp SimpleExpression)?
    private String ProdExpression() {
        String leftType = ProdSimpleExpression();

        // Solo relacionales <,
        if (peek().code == 14) {
            advance(); // consumimos el operador relacional
            String rightType = ProdSimpleExpression();

            if (!"int".equals(leftType) || !"int".equals(rightType)) {
                throw new RuntimeException("Operador relacional requiere operandos int. Encontrado: " +
                        leftType + " y " + rightType);
            }

            return "boolean";
        }

        return leftType; // devuelve int o boolean
    }


    // --- SimpleExpression ::= Term ( ( + | - ) Term )*
    private String ProdSimpleExpression() {
        String type = ProdTerm();

        while (peek().code == 15 || peek().code == 16) { // 15:+  16:-
            advance();
            String rightType = ProdTerm();
            if (!"int".equals(type) || !"int".equals(rightType)) {
                throw new RuntimeException("Operador aritmético requiere operandos int.");
            }
            type = "int";
        }

        return type;
    }

    // --- Term ::= Factor ( ( * ) Factor )*
    private String ProdTerm() {
        String type = ProdFactor();

        while (peek().code == 17) { // 17:*
            advance();
            String rightType = ProdFactor();
            if (!"int".equals(type) || !"int".equals(rightType)) {
                throw new RuntimeException("Operador aritmético requiere operandos int.");
            }
            type = "int";
        }

        return type;
    }

    // --- Factor ::= número | true | false | identificador
    private String ProdFactor() {
        if (match(5)) return "boolean"; // true
        if (match(6)) return "boolean"; // false
        if (match(19)) return "int";    // número

        if (match(18)) { // identificador
            String name = tokens.get(pos - 1).value;
            if (!symbolTable.containsKey(name)) {
                throw new RuntimeException("Variable '" + name + "' no declarada.");
            }
            return symbolTable.get(name);
        }

        throw new RuntimeException("Factor inesperado: " + peek());
    }
}