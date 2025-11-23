package Compilador;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CodigoObjeto {
    private StringBuilder codigoObjeto;
    private LinkedHashMap<String, String> tablaSimbolos;

    public CodigoObjeto(LinkedHashMap<String, String> tablaSimbolos) {
        this.codigoObjeto = new StringBuilder();
        this.tablaSimbolos = tablaSimbolos;
    }

    public String generarCodigoObjeto(ArrayList<Triple> instrucciones) {
        generarSeccionDatos();
        generarSeccionCodigo(instrucciones);
        return codigoObjeto.toString();
    }

    private void generarSeccionDatos() {
        codigoObjeto.append("////////////////////// Sección de Datos: //////////////////////\n\n");
        codigoObjeto.append("   Instrucción  |        Offset        |      Contenido\n");

        int offset = 0;
        for (String variable : tablaSimbolos.keySet()) {
            String tipo = tablaSimbolos.get(variable);
            String binario = tipo.equals("int") ? "0000 0000 0000 0000" : "0000 0000";
            String instruccion = String.format("%-15s", variable + " " + (tipo.equals("int") ? "DW ?" : "DB ?"));
            String offsetStr = String.format("%16s", Integer.toBinaryString(offset)).replace(' ', '0');
            offsetStr = offsetStr.replaceAll("(.{4})", "$1 "); // Add spaces every 4 bits
            codigoObjeto.append(String.format("%s | %s | %s\n", instruccion, offsetStr, binario));
            offset += tipo.equals("int") ? 2 : 1; // Increment offset based on type size
        }
        codigoObjeto.append("\n");
    }

    private void generarSeccionCodigo(ArrayList<Triple> instrucciones) {
        codigoObjeto.append("////////////////////// Sección de Codigo: //////////////////////\n\n");
        codigoObjeto.append("   Instrucción  |        Offset        |      Contenido\n");

        int offset = 0;
        for (Triple instruccion : instrucciones) {
            String opcode = traducirInstruccion(instruccion, offset);
            if (!opcode.isEmpty()) {
                String instruccionStr = String.format("%-15s", instruccion.operador + " " + instruccion.arg1 + ", " + instruccion.arg2);
                String offsetStr = String.format("%16s", Integer.toBinaryString(offset)).replace(' ', '0');
                offsetStr = offsetStr.replaceAll("(.{4})", "$1 "); // Add spaces every 4 bits
                codigoObjeto.append(String.format("%s | %s | %s\n", instruccionStr, offsetStr, opcode));
                offset += calcularLongitud(opcode);
            }
        }
    }

    private String traducirInstruccion(Triple instruccion, int offset) {
        String operador = instruccion.operador;
        String arg1 = instruccion.arg1;
        String arg2 = instruccion.arg2;

        switch (operador) {
            case "MOV", "=":
                return generarMOV(arg1, arg2);
            case "ADD":
                return generarADD(arg1, arg2);
            case "SUB":
                return generarSUB(arg1, arg2);
            case "CMP":
                return generarCMP(arg1, arg2);
            case "PUSH":
                return generarPUSH(arg1);
            case "POP":
                return generarPOP(arg1);
            case "MUL":
                return generarMUL(arg1);
            case "DIV":
                return generarDIV(arg1);
            case "JGE":
                return generarJGE(arg1, offset);
            case "CALL":
                return generarCALL(arg1, offset);
            case "INT":
                return generarINT(arg1);
            case "JMP":
                return generarJMP(arg1, offset);
            case "XOR":
                return generarXOR(arg1, arg2);
            case "INC":
                return generarINC(arg1);
            case "TEST":
                return generarTEST(arg1, arg2);
            case "JNZ":
                return generarJNZ(arg1, offset);
            case "LOOP":
                return generarLOOP(arg1, offset);
            case "RET":
                return generarRET();
            default:
                return "";
        }
    }

    private String generarMOV(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "100010" + d + w + " | " + modRM;
    }

    private String generarADD(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "000000" + d + w + " | " + modRM;
    }

    private String generarSUB(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "001010" + d + w + " | " + modRM;
    }

    private String generarCMP(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "001110" + d + w + " | " + modRM;
    }

    private String generarPUSH(String registro) {
        String reg = calcularReg(registro, "1");
        return "01010" + reg;
    }

    private String generarPOP(String registro) {
        String reg = calcularReg(registro, "1");
        return "01011" + reg;
    }

    private String generarMUL(String registro) {
        String reg = calcularReg(registro, "1");
        return "1111011" + "1" + " | " + calcularModRM("11", "100", reg);
    }

    private String generarDIV(String registro) {
        String reg = calcularReg(registro, "1");
        return "1111011" + "1" + " | " + calcularModRM("11", "110", reg);
    }

    private String generarJGE(String etiqueta, int offset) {
        int displacement = calcularDesplazamiento(etiqueta, offset);
        return "01111101" + aBinario8(String.valueOf(displacement));
    }

    private String generarCALL(String etiqueta, int offset) {
        int displacement = calcularDesplazamiento(etiqueta, offset);
        return "11101000" + aBinario16(String.valueOf(displacement));
    }

    private String generarINT(String data) {
        return "11001101" + aBinario8(data);
    }

    private String generarJMP(String etiqueta, int offset) {
        int displacement = calcularDesplazamiento(etiqueta, offset);
        return "11101011" + aBinario8(String.valueOf(displacement));
    }

    private String generarXOR(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "001100" + d + w + " | " + modRM;
    }

    private String generarINC(String registro) {
        String reg = calcularReg(registro, "1");
        return "01000" + reg;
    }

    private String generarTEST(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, calcularReg(fuente, w));
        return "100001" + w + " | " + modRM;
    }

    private String generarJNZ(String etiqueta, int offset) {
        int displacement = calcularDesplazamiento(etiqueta, offset);
        return "01110101" + aBinario8(String.valueOf(displacement));
    }

    private String generarLOOP(String etiqueta, int offset) {
        int displacement = calcularDesplazamiento(etiqueta, offset);
        return "11100010" + aBinario8(String.valueOf(displacement));
    }

    private String generarRET() {
        return "11000011";
    }

    private String calcularW(String destino, String fuente) {
        return (esRegistro16(destino) || esRegistro16(fuente)) ? "1" : "0";
    }

    private String calcularD(String destino, String fuente) {
        return esRegistro(destino) ? "1" : "0";
    }

    // Before: Incorrect handling of variables in calcularReg
    private String calcularReg(String registro, String w) {
        if (esRegistro(registro)) {
            switch (registro) {
                case "AL": return "000";
                case "CL": return "001";
                case "DL": return "010";
                case "BL": return "011";
                case "AX": return w.equals("1") ? "000" : "ERROR";
                case "CX": return w.equals("1") ? "001" : "ERROR";
                case "DX": return w.equals("1") ? "010" : "ERROR";
                case "BX": return w.equals("1") ? "011" : "ERROR";
                case "AH": return "100";
                case "CH": return "101";
                case "DH": return "110";
                case "BH": return "111";
            }
        } else if (tablaSimbolos.containsKey(registro)) {
            // Para operandos de memoria, devolvemos un valor genérico (por ejemplo, "110").
            return "110"; // Esto indica que es un operando de memoria.
        }
        return "ERROR";
    }

    // After: Adjusted calcularModRM to handle memory operands
    private String calcularModRM(String mod, String reg, String rm) {
        if (tablaSimbolos.containsKey(rm)) {
            // Memory addressing: mod = 00 (no displacement)
            mod = "00";
            rm = "110"; // Placeholder for memory addressing
        }
        return mod + reg + rm;
    }

    private boolean esRegistro(String registro) {
        return registro.matches("AL|CL|DL|BL|AH|CH|DH|BH|AX|CX|DX|BX");
    }

    private boolean esRegistro16(String registro) {
        return registro.matches("AX|CX|DX|BX");
    }

    private int calcularLongitud(String opcode) {
        return opcode.replace(" ", "").length() / 8;
    }

    private int calcularDesplazamiento(String etiqueta, int offset) {
        if (tablaSimbolos.containsKey(etiqueta)) {
            int etiquetaOffset = Integer.parseInt(tablaSimbolos.get(etiqueta));
            return etiquetaOffset - (offset + 2);
        }
        return 0;
    }

    private String aBinario16(String valor) {
        try {
            int intValue = Integer.parseInt(valor);
            return String.format("%16s", Integer.toBinaryString(intValue & 0xFFFF)).replace(' ', '0');
        } catch (NumberFormatException e) {
            return "0000000000000000";
        }
    }

    private String aBinario8(String valor) {
        try {
            int intValue = Integer.parseInt(valor);
            return String.format("%8s", Integer.toBinaryString(intValue & 0xFF)).replace(' ', '0');
        } catch (NumberFormatException e) {
            return "00000000";
        }
    }

    private String calcularS(String valor, String w) {
        if (w.equals("1")) {
            int intValue = Integer.parseInt(valor);
            return (intValue >= -128 && intValue <= 127) ? "1" : "0";
        }
        return "0";
    }
}