package Compilador;

public class Triple {
    public String operador;
    public String arg1;
    public String arg2;

    public Triple(String operador, String arg1, String arg2) {
        this.operador = operador;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return "(" + operador + ", " + (arg1 != null ? arg1 : "") + ", " + (arg2 != null ? arg2 : "") + ")";
    }
}