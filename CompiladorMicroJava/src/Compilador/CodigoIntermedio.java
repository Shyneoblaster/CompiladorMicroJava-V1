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
        generarSeccionDatos(instrucciones);
        generarSeccionCodigo(instrucciones);
        generarPie(instrucciones);
        return codigo.toString();
    }

    private void generarEncabezado() {
        codigo.append("\tTITLE Programa\n");
        codigo.append("\t.MODEL SMALL\n");
        codigo.append("\t.STACK 100h\n");
    }

    private void generarSeccionDatos(ArrayList<Triple> instrucciones) {
        codigo.append("\t.DATA\n");
        for (String variable : tablaSimbolos.keySet()) {
            String tipo = tablaSimbolos.get(variable);
            if ("int".equalsIgnoreCase(tipo)) {
                codigo.append("\t").append(variable).append("\tDW ? ; Variable int ").append(variable).append("\n");
            } else if ("boolean".equalsIgnoreCase(tipo)) {
                codigo.append("\t").append(variable).append("\tDB ? ; Variable boolean ").append(variable).append("\n");
            } else {
                throw new RuntimeException("Tipo de variable no soportado: " + tipo);
            }
        }
    }
    private void generarSeccionCodigo(ArrayList<Triple> instrucciones) {
        codigo.append("\t.CODE\n");
        codigo.append("MAIN \tPROC \tFAR\n");
        codigo.append("\t.STARTUP\n\n");

        for (Triple instruccion : instrucciones) {
            switch (instruccion.operador) {
                case "ASSIGN":
                    codigo.append("\t; ").append(instruccion.arg1).append(" = ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \t").append(instruccion.arg1).append(", AX\n");
                    break;

                case "WHILE":
                    codigo.append("WHILE_").append(contadorWhile).append("_START:\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg1).append("\n");
                    codigo.append("\tCMP \tAX, ").append(instruccion.arg2).append("\n");
                    codigo.append("\tJGE \tWHILE_").append(contadorWhile).append("_END\n");
                    contadorWhile++;
                    break;

                case "END_WHILE":
                    contadorWhile--;
                    codigo.append("\tJMP \tWHILE_").append(contadorWhile).append("_START\n");
                    codigo.append("WHILE_").append(contadorWhile).append("_END:\n");
                    break;

                case "PRINT":
                    codigo.append("\t; println(").append(instruccion.arg1).append(")\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg1).append("\n");
                    codigo.append("\tCALL \tPrintNumber\n");
                    codigo.append("\tMOV \tDL, 13\n");
                    codigo.append("\tMOV \tAH, 02h\n");
                    codigo.append("\tINT \t21h\n");
                    codigo.append("\tMOV \tDL, 10\n");
                    codigo.append("\tMOV \tAH, 02h\n");
                    codigo.append("\tINT \t21h\n");
                    break;

                case "ADD":
                    codigo.append("\t; ").append(instruccion.arg1).append(" + ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg1).append("\n");
                    codigo.append("\tADD \tAX, ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \t").append(instruccion.arg1).append(", AX\n");
                    break;

                case "MINUS":
                    codigo.append("\t; ").append(instruccion.arg1).append(" - ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg1).append("\n");
                    codigo.append("\tSUB \tAX, ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \t").append(instruccion.arg1).append(", AX\n");
                    break;

                case "MUL":
                    codigo.append("\t; ").append(instruccion.arg1).append(" * ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMOV \tAX, ").append(instruccion.arg1).append("\n");
                    codigo.append("\tMOV \tBX, ").append(instruccion.arg2).append("\n");
                    codigo.append("\tMUL \tBX").append("\n");
                    codigo.append("\tMOV \t").append(instruccion.arg1).append(", AX\n");
                    break;
                default:
                    throw new RuntimeException("Operador no soportado: " + instruccion.operador);
            }
        }
    }

    private void generarPie(ArrayList<Triple> instrucciones) {
        codigo.append("\t; Terminar el programa\n");
        codigo.append("\tMOV \tAH, 4CH\n");
        codigo.append("\tINT \t21H\n");
        codigo.append("\t.EXIT\n");
        codigo.append("MAIN \tENDP\n");

        // Verificar si hay alguna instrucción PRINT
        boolean contienePrint = instrucciones.stream().anyMatch(instr -> "PRINT".equals(instr.operador));

        if (contienePrint) {
            codigo.append("PrintNumber \tPROC\n");
            codigo.append("\tPUSH \tAX\n");
            codigo.append("\tPUSH \tBX\n");
            codigo.append("\tPUSH \tCX\n");
            codigo.append("\tPUSH \tDX\n");
            codigo.append("\tMOV \tCX, 0\n");
            codigo.append("\tMOV \tBX, 10\n");
            codigo.append("CONVERT:\n");
            codigo.append("\tXOR \tDX, DX\n");
            codigo.append("\tDIV \tBX\n");
            codigo.append("\tPUSH \tDX\n");
            codigo.append("\tINC \tCX\n");
            codigo.append("\tTEST \tAX, AX\n");
            codigo.append("\tJNZ \tCONVERT\n");
            codigo.append("PRINT_LOOP:\n");
            codigo.append("\tPOP \tDX\n");
            codigo.append("\tADD \tDL, '0'\n");
            codigo.append("\tMOV \tAH, 02h\n");
            codigo.append("\tINT \t21h\n");
            codigo.append("\tLOOP \tPRINT_LOOP\n");
            codigo.append("\tPOP \tDX\n");
            codigo.append("\tPOP \tCX\n");
            codigo.append("\tPOP \tBX\n");
            codigo.append("\tPOP \tAX\n");
            codigo.append("\tRET\n");
            codigo.append("\tPrintNumber \tENDP\n");
        }

        codigo.append("\tEND \tMAIN\n");
    }
}