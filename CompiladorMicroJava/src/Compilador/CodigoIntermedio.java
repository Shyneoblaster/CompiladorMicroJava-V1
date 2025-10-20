package Compilador;

import java.util.ArrayList;
import java.util.HashMap;

public class CodigoIntermedio {
    private StringBuilder codigo;
    private HashMap<String, String> tablaSimbolos;
    private int contadorWhile;

    public CodigoIntermedio(HashMap<String, String> tablaSimbolos) {
        this.codigo = new StringBuilder();
        this.tablaSimbolos = tablaSimbolos;
        this.contadorWhile = 0;
    }

    public String generarCodigo(ArrayList<Triple> instrucciones) {
        generarEncabezado();
        generarSeccionDatos();
        generarSeccionCodigo(instrucciones);
        generarPie();
        return codigo.toString();
    }

    private void generarEncabezado() {
        codigo.append("\tTITLE Programa\n");
        codigo.append("\t.MODEL SMALL\n");
        codigo.append("\t.STACK 100h\n");
    }

    private void generarSeccionDatos() {
        codigo.append("\t.DATA\n");
        for (String variable : tablaSimbolos.keySet()) {
            codigo.append("\t"+variable).append("\tDW ? ; Variable ").append(variable).append("\n");
        }
    }

    private void generarSeccionCodigo(ArrayList<Triple> instrucciones) {
        codigo.append("\t.CODE\n");
        codigo.append("MAIN \tPROC \tFAR\n");
        codigo.append("\t.STARTUP\n\n");

        for (Triple instruccion : instrucciones) {
            switch (instruccion.operador) {
                case "ASSIGN":
                    codigo.append("    ; ").append(instruccion.arg1).append(" = ").append(instruccion.arg2).append("\n");
                    codigo.append("    MOV AX, ").append(instruccion.arg2).append("\n");
                    codigo.append("    MOV ").append(instruccion.arg1).append(", AX\n");
                    break;

                case "WHILE":
                    codigo.append("WHILE_").append(contadorWhile).append("_START:\n");
                    codigo.append("    MOV AX, ").append(instruccion.arg1).append("\n");
                    codigo.append("    CMP AX, ").append(instruccion.arg2).append("\n");
                    codigo.append("    JGE WHILE_").append(contadorWhile).append("_END\n");
                    contadorWhile++;
                    break;

                case "END_WHILE":
                    contadorWhile--;
                    codigo.append("    JMP WHILE_").append(contadorWhile).append("_START\n");
                    codigo.append("WHILE_").append(contadorWhile).append("_END:\n");
                    break;

                case "PRINT":
                    codigo.append("    ; println(").append(instruccion.arg1).append(")\n");
                    codigo.append("    MOV AX, ").append(instruccion.arg1).append("\n");
                    codigo.append("    CALL PrintNumber\n");
                    codigo.append("    MOV DL, 13\n");
                    codigo.append("    MOV AH, 02h\n");
                    codigo.append("    INT 21h\n");
                    codigo.append("    MOV DL, 10\n");
                    codigo.append("    MOV AH, 02h\n");
                    codigo.append("    INT 21h\n");
                    break;

                case "ADD":
                    codigo.append("    ; ").append(instruccion.arg1).append(" + ").append(instruccion.arg2).append("\n");
                    codigo.append("    MOV AX, ").append(instruccion.arg1).append("\n");
                    codigo.append("    ADD AX, ").append(instruccion.arg2).append("\n");
                    codigo.append("    MOV ").append(instruccion.arg1).append(", AX\n");
                    break;

                default:
                    throw new RuntimeException("Operador no soportado: " + instruccion.operador);
            }
        }
    }

    private void generarPie() {
        codigo.append("    ; Terminar el programa\n");
        codigo.append("    MOV AH, 4CH\n");
        codigo.append("    INT 21H\n");
        codigo.append("    .EXIT\n");
        codigo.append("MAIN ENDP\n");
        codigo.append("PrintNumber PROC\n");
        codigo.append("    PUSH AX\n");
        codigo.append("    PUSH BX\n");
        codigo.append("    PUSH CX\n");
        codigo.append("    PUSH DX\n");
        codigo.append("    MOV CX, 0\n");
        codigo.append("    MOV BX, 10\n");
        codigo.append("CONVERT:\n");
        codigo.append("    XOR DX, DX\n");
        codigo.append("    DIV BX\n");
        codigo.append("    PUSH DX\n");
        codigo.append("    INC CX\n");
        codigo.append("    TEST AX, AX\n");
        codigo.append("    JNZ CONVERT\n");
        codigo.append("PRINT_LOOP:\n");
        codigo.append("    POP DX\n");
        codigo.append("    ADD DL, '0'\n");
        codigo.append("    MOV AH, 02h\n");
        codigo.append("    INT 21h\n");
        codigo.append("    LOOP PRINT_LOOP\n");
        codigo.append("    POP DX\n");
        codigo.append("    POP CX\n");
        codigo.append("    POP BX\n");
        codigo.append("    POP AX\n");
        codigo.append("    RET\n");
        codigo.append("PrintNumber ENDP\n");
        codigo.append("END MAIN\n");
    }
}