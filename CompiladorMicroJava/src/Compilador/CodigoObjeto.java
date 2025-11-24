package Compilador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodigoObjeto {
    private StringBuilder codigoObjeto;
    private LinkedHashMap<String, String> tablaSimbolos; // variable -> tipo (int/boolean)
    private Map<String, Integer> datosOffset; // variable -> offset en bytes

    public CodigoObjeto(LinkedHashMap<String, String> tablaSimbolos) {
        this.codigoObjeto = new StringBuilder();
        this.tablaSimbolos = tablaSimbolos;
        this.datosOffset = new HashMap<>();
    }

    public String generarCodigoObjeto(ArrayList<Triple> instrucciones) {
        generarSeccionDatos();             // llena datosOffset
        generarSeccionCodigo(instrucciones);
        return codigoObjeto.toString();
    }

    private void generarSeccionDatos() {
        codigoObjeto.append("////////////////////// Sección de Datos: //////////////////////\n\n");
        codigoObjeto.append("   Instrucción  |        Offset        |      Contenido\n");

        int offset = 0;
        datosOffset.clear();
        for (String variable : tablaSimbolos.keySet()) {
            String tipo = tablaSimbolos.get(variable);
            String binario = "int".equalsIgnoreCase(tipo) ? "0000 0000 0000 0000" : "0000 0000";
            String instruccion = String.format("%-15s", variable + " " + (tipo.equals("int") ? "DW ?" : "DB ?"));
            String offsetStr = String.format("%16s", Integer.toBinaryString(offset)).replace(' ', '0');
            offsetStr = offsetStr.replaceAll("(.{4})", "$1 "); // Add spaces every 4 bits
            codigoObjeto.append(String.format("%s | %s | %s\n", instruccion, offsetStr, binario));
            datosOffset.put(variable, offset);
            offset += "int".equalsIgnoreCase(tipo) ? 2 : 1;
        }
        codigoObjeto.append("\n");
    }

    // Representa una entrada (etiqueta o instrucción con plantilla)
    private static class Entry {
        boolean isLabel;
        String labelName;       // si isLabel == true
        String template;        // plantilla binaria con separadores " | " y placeholders {D8:LBL} / {D16:LBL}
        int offsetBytes;        // calculado en la primera pasada
        int sizeBytes;          // calculado
        String mnemonic;        // texto legible de la instrucción
        Entry(String labelName) {
            this.isLabel = true;
            this.labelName = labelName;
            this.template = null;
            this.mnemonic = null;
        }
        Entry(String template, String mnemonic) {
            this.isLabel = false;
            this.labelName = null;
            this.template = template;
            this.mnemonic = mnemonic;
        }
    }

    private void generarSeccionCodigo(ArrayList<Triple> instrucciones) {
        codigoObjeto.append("////////////////////// Sección de Codigo: //////////////////////\n\n");
        codigoObjeto.append("   Instrucción       |        Offset        |      Contenido\n");

        // DEBUG: imprimir Triples recibidos
        System.out.println("DEBUG: Triples recibidos:");
        for (int i = 0; i < instrucciones.size(); i++) {
            Triple t = instrucciones.get(i);
            System.out.println(i + ": " + t.operador + " " + t.arg1 + " " + t.arg2);
        }

        ArrayList<Entry> entries = new ArrayList<>();
        int whileCounter = 0;
        boolean necesitaPrintRoutine = false;

        // 1) Construir lista de entradas (plantillas), generando etiquetas internas
        for (Triple instr : instrucciones) {
            String op = instr.operador;
            String a1 = instr.arg1;
            String a2 = instr.arg2;

            switch (op) {
                case "ASSIGN":
                    // Emitir según tipo de la variable destino: boolean -> AL, int -> AX
                    String tipoDest = tablaSimbolos.get(a1);
                    if ("boolean".equalsIgnoreCase(tipoDest)) {
                        entries.add(new Entry(generarMOV("AL", a2), "MOV AL, " + a2));
                        entries.add(new Entry(generarMOV(a1, "AL"), "MOV " + a1 + ", AL"));
                    } else {
                        entries.add(new Entry(generarMOV("AX", a2), "MOV AX, " + a2));
                        entries.add(new Entry(generarMOV(a1, "AX"), "MOV " + a1 + ", AX"));
                    }
                    break;

                case "MOV":
                case "=":
                    entries.add(new Entry(generarMOV(a1, a2), "MOV " + a1 + ", " + a2));
                    break;

                case "ADD":
                    // Mantener la semántica de CodigoIntermedio: cargar, operar, guardar
                    entries.add(new Entry(generarMOV("AX", a1), "MOV AX, " + a1));
                    entries.add(new Entry(generarADD("AX", a2), "ADD AX, " + a2));
                    entries.add(new Entry(generarMOV(a1, "AX"), "MOV " + a1 + ", AX"));
                    break;

                case "SUB":
                case "MINUS":
                    entries.add(new Entry(generarSUB(a1, a2), "SUB " + a1 + ", " + a2));
                    break;

                case "CMP":
                    entries.add(new Entry(generarCMP(a1, a2), "CMP " + a1 + ", " + a2));
                    break;

                case "PRINT": {
                    String tipoArg = tablaSimbolos.get(a1);
                    if ("int".equalsIgnoreCase(tipoArg)) {
                        entries.add(new Entry(generarMOV("AX", a1), "MOV AX, " + a1));
                        entries.add(new Entry("11101000" + " | " + "{D16:PrintNumber}", "CALL PrintNumber"));
                    } else if ("boolean".equalsIgnoreCase(tipoArg)) {
                        entries.add(new Entry(generarMOV("AL", a1), "MOV AL, " + a1));
                        entries.add(new Entry(generarMOV("AH", "0"), "MOV AH, 0"));
                        entries.add(new Entry("11101000" + " | " + "{D16:PrintNumber}", "CALL PrintNumber"));
                    }
                    necesitaPrintRoutine = true;
                    // Después del CALL: CR y LF
                    entries.add(new Entry(generarMOV("DL", "13"), "MOV DL, 13"));
                    entries.add(new Entry(generarMOV("AH", "2"), "MOV AH, 02h"));
                    entries.add(new Entry(generarINT("33"), "INT 21h"));
                    entries.add(new Entry(generarMOV("DL", "10"), "MOV DL, 10"));
                    entries.add(new Entry(generarMOV("AH", "2"), "MOV AH, 02h"));
                    entries.add(new Entry(generarINT("33"), "INT 21h"));
                    break;
                }

                case "WHILE":
                    String startLbl = "WHILE_" + whileCounter + "_START";
                    String endLbl = "WHILE_" + whileCounter + "_END";
                    entries.add(new Entry(startLbl));
                    entries.add(new Entry(generarMOV("AX", a1), "MOV AX, " + a1));
                    entries.add(new Entry(generarCMP("AX", a2), "CMP AX, " + a2));
                    entries.add(new Entry("01111101" + " | " + "{D8:" + endLbl + "}", "JGE " + endLbl));
                    whileCounter++;
                    break;

                case "WHILE_BOOL":
                    String sLbl = "WHILE_" + whileCounter + "_START";
                    String eLbl = "WHILE_" + whileCounter + "_END";
                    entries.add(new Entry(sLbl));
                    entries.add(new Entry(generarMOV("AL", a1), "MOV AL, " + a1));
                    entries.add(new Entry(generarCMP("AL", "1"), "CMP AL, 1"));
                    entries.add(new Entry("01110101" + " | " + "{D8:" + eLbl + "}", "JNE " + eLbl));
                    whileCounter++;
                    break;

                case "END_WHILE":
                    int idx = whileCounter - 1;
                    if (idx < 0) idx = 0;
                    String sLbl2 = "WHILE_" + idx + "_START";
                    String eLbl2 = "WHILE_" + idx + "_END";
                    entries.add(new Entry("11101011" + " | " + "{D8:" + sLbl2 + "}", "JMP " + sLbl2));
                    entries.add(new Entry(eLbl2));
                    break;

                case "MUL":
                    entries.add(new Entry(generarMUL(a1), "MUL " + a1));
                    break;

                case "DIV":
                    entries.add(new Entry(generarDIV(a1), "DIV " + a1));
                    break;

                case "JGE":
                    entries.add(new Entry("01111101" + " | " + "{D8:" + a1 + "}", "JGE " + a1));
                    break;

                case "JMP":
                    entries.add(new Entry("11101011" + " | " + "{D8:" + a1 + "}", "JMP " + a1));
                    break;

                case "CALL":
                    entries.add(new Entry("11101000" + " | " + "{D16:" + a1 + "}", "CALL " + a1));
                    break;

                case "INT":
                    entries.add(new Entry(generarINT(a1), "INT " + a1));
                    break;

                default:
                    // ignorar instrucciones no mapeadas
                    break;
            }
        }

        // Si hubo PRINTs, añadimos rutina PrintNumber completa (etiquetas y cuerpo)
        if (necesitaPrintRoutine) {
            entries.add(new Entry("PrintNumber"));
            // Push registers
            entries.add(new Entry(generarPUSH("AX"), "PUSH AX"));
            entries.add(new Entry(generarPUSH("BX"), "PUSH BX"));
            entries.add(new Entry(generarPUSH("CX"), "PUSH CX"));
            entries.add(new Entry(generarPUSH("DX"), "PUSH DX"));
            // MOV CX,0 ; MOV BX,10
            entries.add(new Entry(generarMOV("CX", "0"), "MOV CX, 0"));
            entries.add(new Entry(generarMOV("BX", "10"), "MOV BX, 10"));
            // CONVERT:
            entries.add(new Entry("CONVERT"));
            entries.add(new Entry(generarXOR("DX", "DX"), "XOR DX, DX"));
            entries.add(new Entry(generarDIV("BX"), "DIV BX"));
            entries.add(new Entry(generarPUSH("DX"), "PUSH DX"));
            entries.add(new Entry(generarINC("CX"), "INC CX"));
            entries.add(new Entry(generarTEST("AX", "AX"), "TEST AX, AX"));
            entries.add(new Entry("01110101" + " | " + "{D8:CONVERT}", "JNZ CONVERT"));
            // PRINT_LOOP:
            entries.add(new Entry("PRINT_LOOP"));
            entries.add(new Entry(generarPOP("DX"), "POP DX"));
            entries.add(new Entry(generarADD("DL", String.valueOf(48)), "ADD DL, '0'"));
            entries.add(new Entry(generarMOV("AH", "2"), "MOV AH, 02h"));
            entries.add(new Entry(generarINT("33"), "INT 21h"));
            entries.add(new Entry("11100010" + " | " + "{D8:PRINT_LOOP}", "LOOP PRINT_LOOP"));
            // Restore and RET
            entries.add(new Entry(generarPOP("DX"), "POP DX"));
            entries.add(new Entry(generarPOP("CX"), "POP CX"));
            entries.add(new Entry(generarPOP("BX"), "POP BX"));
            entries.add(new Entry(generarPOP("AX"), "POP AX"));
            entries.add(new Entry(generarRET(), "RET"));
        }

        // 2) Primera pasada: calcular offsets y tamaños
        Map<String, Integer> labelOffsets = new HashMap<>();
        int currentOffsetBytes = 0;
        for (Entry e : entries) {
            if (e.isLabel && e.template == null && e.labelName != null) {
                e.offsetBytes = currentOffsetBytes;
                labelOffsets.put(e.labelName, currentOffsetBytes);
                e.sizeBytes = 0;
            } else {
                int bits = countBitsInTemplate(e.template);
                e.sizeBytes = bits / 8;
                e.offsetBytes = currentOffsetBytes;
                currentOffsetBytes += e.sizeBytes;
            }
        }

        // 3) Segunda pasada: resolver placeholders y emitir
        for (Entry e : entries) {
            if (e.isLabel && e.template == null && e.labelName != null) {
                String labelLine = String.format("%-20s", e.labelName + ":");
                String offsetStr = String.format("%16s", Integer.toBinaryString(e.offsetBytes)).replace(' ', '0');
                offsetStr = offsetStr.replaceAll("(.{4})", "$1 ");
                codigoObjeto.append(String.format("%s | %s | %s\n", labelLine, offsetStr, "<Etiqueta>"));
                continue;
            }
            String finalOpcode = resolvePlaceholders(e.template, e.offsetBytes, e.sizeBytes, labelOffsets);
            String instruccionText = (e.mnemonic != null) ? e.mnemonic : "DATA";
            String instruccionStr = String.format("%-20s", instruccionText);
            String offsetStr = String.format("%16s", Integer.toBinaryString(e.offsetBytes)).replace(' ', '0');
            offsetStr = offsetStr.replaceAll("(.{4})", "$1 ");
            codigoObjeto.append(String.format("%s | %s | %s\n", instruccionStr, offsetStr, finalOpcode));
        }
    }


    // Cuenta bits en una plantilla: reemplaza placeholders por ceros y cuenta bits (sin separadores)
    private int countBitsInTemplate(String template) {
        if (template == null) return 0;
        String t = template;
        // reemplazar {D8:xxx} por 8 ceros, {D16:xxx} por 16 ceros
        t = t.replaceAll("\\{D8:[^}]+\\}", repeatChar('0', 8));
        t = t.replaceAll("\\{D16:[^}]+\\}", repeatChar('0', 16));
        // quitar separadores y espacios
        t = t.replace(" ", "").replace("|", "");
        return t.length();
    }

    // Resolver placeholders: usar little-endian para {D16:...}
    private String resolvePlaceholders(String template, int entryOffsetBytes, int entrySizeBytes, Map<String, Integer> labelOffsets) {
        if (template == null) return "";
        String result = template;

        // D8 placeholders (igual)
        Pattern p8 = Pattern.compile("\\{D8:([^}]+)\\}");
        Matcher m8 = p8.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (m8.find()) {
            String label = m8.group(1);
            int target = labelOffsets.getOrDefault(label, 0);
            int displacement = target - (entryOffsetBytes + entrySizeBytes);
            String bin = aBinario8(String.valueOf(displacement));
            m8.appendReplacement(sb, bin);
        }
        m8.appendTail(sb);
        result = sb.toString();

        // D16 placeholders -> little-endian (low | high)
        Pattern p16 = Pattern.compile("\\{D16:([^}]+)\\}");
        Matcher m16 = p16.matcher(result);
        sb = new StringBuffer();
        while (m16.find()) {
            String label = m16.group(1);
            int target = labelOffsets.getOrDefault(label, 0);
            int displacement = target - (entryOffsetBytes + entrySizeBytes);
            String bin = formatWordLittleEndian(String.valueOf(displacement));
            m16.appendReplacement(sb, bin);
        }
        m16.appendTail(sb);
        result = sb.toString();

        return result;
    }

    private static String repeatChar(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    // Traducción de instrucciones (devuelve plantilla binaria)
    // generarMOV: usar little-endian para immediatos de 16-bit
    private String generarMOV(String destino, String fuente) {
        // Normalizar literales booleanas
        if (fuente != null) {
            if (fuente.equalsIgnoreCase("true")) fuente = "1";
            else if (fuente.equalsIgnoreCase("false")) fuente = "0";
        }

        String w = calcularW(destino, fuente);
        if (tablaSimbolos.containsKey(destino)) {
            String tipo = tablaSimbolos.get(destino);
            if ("int".equalsIgnoreCase(tipo)) w = "1"; else w = "0";
        }

        // Caso inmediato
        if (isNumeric(fuente)) {
            if (esRegistro(destino)) {
                String reg = calcularReg(destino, w);
                String opcode = "1011" + w + reg;
                String imm = w.equals("1") ? formatWordLittleEndian(fuente) : aBinario8(fuente).replaceAll("(.{4})", "$1 ").trim();
                return opcode + " | " + imm;
            }
            String regField = "000";
            String opcodeBase = w.equals("1") ? "11000111" : "11000110"; // C7 / C6
            String modrm = calcularModRM("00", regField, destino, w);
            String imm = w.equals("1") ? formatWordLittleEndian(fuente) : aBinario8(fuente).replaceAll("(.{4})", "$1 ").trim();
            return opcodeBase + " | " + modrm + " | " + imm;
        }

        // Caso register <-> memoria / register
        String d = calcularD(destino, fuente);

        // El campo 'reg' del ModR/M debe contener siempre el registro (si hay),
        // es decir: si hay un registro entre los operandos, usar ese registro.
        String regField;
        if (esRegistro(fuente)) regField = calcularReg(fuente, w); // fuente es registro -> reg = fuente
        else regField = calcularReg(destino, w); // fuente no-reg -> reg = destino (caso inusual)

        // Determinar qué operando se codifica como r/m y el valor de 'mod'
        String mod;
        String rmOperand;
        if (esRegistro(destino)) {
            // r/m = fuente
            mod = esRegistro(fuente) ? "11" : "00";
            rmOperand = fuente;
        } else {
            // r/m = destino (memoria directa) -> mod = 00 y calcularModRM añadirá displacement
            mod = "00";
            rmOperand = destino;
        }

        String modRM = calcularModRM(mod, regField, rmOperand, w);
        return "100010" + d + w + " | " + modRM;
    }


    // generarADD: immediato 16-bit en little-endian
    private String generarADD(String destino, String fuente) {
        if (isNumeric(fuente)) {
            String w = calcularW(destino, fuente);
            String s = calcularS(fuente, w);           // usar s cuando el inmediato cabe en signed 8-bit
            String regField = "000"; // /0 para ADD
            // Si el destino es registro, mod = 11 (registro); si es memoria, mod = 00 (r/m)
            String mod = esRegistro(destino) ? "11" : "00";
            String modrm = calcularModRM(mod, regField, destino, w);
            String immBin = w.equals("1")
                    ? formatWordLittleEndian(fuente)
                    : aBinario8(fuente).replaceAll("(.{4})", "$1 ").trim();
            return "100000" + s + w + " | " + modrm + " | " + immBin;
        } else {
            String w = calcularW(destino, fuente);
            String d = calcularD(destino, fuente);
            String reg = calcularReg(destino, w);
            String modRM = calcularModRM("11", reg, fuente, w);
            return "000000" + d + w + " | " + modRM;
        }
    }

    private String generarSUB(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, fuente, w);
        return "001010" + d + w + " | " + modRM;
    }


    // generarCMP: immediato 16-bit en little-endian
    private String generarCMP(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        if (isNumeric(fuente)) {
            String regField = "111"; // /7 para CMP r/m, imm
            String mod = esRegistro(destino) ? "11" : "00";
            String modrm = calcularModRM(mod, regField, destino, w);
            String imm = w.equals("1") ? formatWordLittleEndian(fuente) : aBinario8(fuente).replaceAll("(.{4})", "$1 ").trim();
            return "100000" + " | " + modrm + " | " + imm;
        } else {
            String d = calcularD(destino, fuente);
            String reg = calcularReg(destino, w);
            String modRM = calcularModRM("11", reg, fuente, w);
            return "001110" + d + w + " | " + modRM;
        }
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
        return "1111011" + "1" + " | " + calcularModRM("11", "100", registro, "1");
    }

    private String generarDIV(String registro) {
        return "1111011" + "1" + " | " + calcularModRM("11", "110", registro, "1");
    }

    private String generarINT(String data) {
        return "11001101" + " | " + aBinario8(data);
    }

    private String generarXOR(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente);
        String reg = calcularReg(destino, w);
        String modRM = calcularModRM("11", reg, fuente, w);
        return "001100" + d + w + " | " + modRM;
    }

    private String generarINC(String registro) {
        String reg = calcularReg(registro, "1");
        return "01000" + reg;
    }

    private String generarTEST(String destino, String fuente) {
        String w = calcularW(destino, fuente);
        String d = calcularD(destino, fuente); // incluir bit d según plantilla solicitada

        // el campo 'reg' debe contener el registro de la segunda operando (fuente)
        String reg = calcularReg(fuente, w);

        // determinar mod y operando r/m según si destino es registro o memoria
        String mod;
        String rmOperand;
        if (esRegistro(destino)) {
            mod = "11";
            rmOperand = destino;
        } else {
            mod = "00";
            rmOperand = destino;
        }

        String modRM = calcularModRM(mod, reg, rmOperand, w);
        return "000100" + d + w + " | " + modRM;
    }

    private String generarJNZ(String etiqueta, int offset) {
        int displacement = 0;
        return "01110101" + aBinario8(String.valueOf(displacement));
    }

    private String generarLOOP(String etiqueta, int offset) {
        int displacement = 0;
        return "11100010" + aBinario8(String.valueOf(displacement));
    }

    private String generarRET() {
        return "11000011";
    }

    private String calcularW(String destino, String fuente) {
        // Si destino es una variable conocida, usar su tipo
        if (destino != null && tablaSimbolos.containsKey(destino)) {
            return "int".equalsIgnoreCase(tablaSimbolos.get(destino)) ? "1" : "0";
        }
        // Si fuente es una variable conocida, usar su tipo
        if (fuente != null && tablaSimbolos.containsKey(fuente)) {
            return "int".equalsIgnoreCase(tablaSimbolos.get(fuente)) ? "1" : "0";
        }
        // fallback: registros 16-bit determinan w
        return (esRegistro16(destino) || esRegistro16(fuente)) ? "1" : "0";
    }

    private String calcularD(String destino, String fuente) {
        return esRegistro(destino) ? "1" : "0";
    }

    private String calcularReg(String registro, String w) {
        if (registro == null) return "ERROR";
        // Registros 8-bit
        switch (registro) {
            case "AL": return "000";
            case "CL": return "001";
            case "DL": return "010";
            case "BL": return "011";
            case "AH": return "100";
            case "CH": return "101";
            case "DH": return "110";
            case "BH": return "111";
        }
        // Registros 16-bit
        switch (registro) {
            case "AX": return "000";
            case "CX": return "001";
            case "DX": return "010";
            case "BX": return "011";
        }
        // Variable / memoria: usar código rm = 110 para direccion directa
        if (tablaSimbolos.containsKey(registro)) {
            return "110";
        }
        // No es registro ni variable conocida
        return "ERROR";
    }

    // calcularModRM ahora usa little-endian para el desplazamiento de 16-bit en memoria directa
    private String calcularModRM(String mod, String reg, String operando, String w) {
        if (esRegistro(operando)) {
            String rm = calcularReg(operando, w);
            return mod + reg + rm;
        }
        if (tablaSimbolos.containsKey(operando)) {
            String rm = "110"; // direccion directa
            String base = mod + reg + rm;
            if ("00".equals(mod)) {
                String displacement = "0000000000000000";
                try {
                    int off = datosOffset.getOrDefault(operando, 0);
                    // usar little-endian en la representación de la plantilla
                    displacement = formatWordLittleEndian(String.valueOf(off));
                } catch (Exception e) {
                    // mantener cero si falla
                }
                return base + " | " + displacement;
            }
            return base;
        }
        return "ERROR";
    }

    private boolean esRegistro(String registro) {
        return registro != null && registro.matches("AL|CL|DL|BL|AH|CH|DH|BH|AX|CX|DX|BX");
    }

    private boolean esRegistro16(String registro) {
        return registro != null && registro.matches("AX|CX|DX|BX");
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

    private boolean isNumeric(String s) {
        if (s == null) return false;
        return s.matches("-?\\d+");
    }

    // Helper: formatea un word (16-bit) en little-endian como "LLLLLLLL | HHHHHHHH"
    private String formatWordLittleEndian(String valor) {
        try {
            int v = Integer.parseInt(valor);
            int low = v & 0xFF;
            int high = (v >> 8) & 0xFF;
            String lowBin = aBinario8(String.valueOf(low)).replaceAll("(.{4})", "$1 ").trim();
            String highBin = aBinario8(String.valueOf(high)).replaceAll("(.{4})", "$1 ").trim();
            return lowBin + " | " + highBin;
        } catch (NumberFormatException e) {
            String zero = aBinario8("0").replaceAll("(.{4})", "$1 ").trim();
            return zero + " | " + zero;
        }
    }


}
