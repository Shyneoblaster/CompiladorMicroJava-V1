package Compilador;

public class Simbolo {
    String token;
    String lexema;
    String tipo;
    String valor;
    int direccion;

    public Simbolo(String token, String lexema, String tipo, String valor, int direccion) {
        this.token = token;
        this.lexema = lexema;
        this.tipo = tipo;
        this.valor = valor;
        this.direccion = direccion;
    }
}

