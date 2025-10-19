package Compilador;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class ModeloTablaSimbolo extends AbstractTableModel {

    private final String[] columnas = {"Token", "Lexema", "Tipo", "Valor", "Dirección"};
    private ArrayList<Simbolo> simbolos;

    public ModeloTablaSimbolo(ArrayList<Simbolo> simbolos) {
        this.simbolos = simbolos;
    }

    @Override
    public int getRowCount() {
        return simbolos.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnas[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Simbolo s = simbolos.get(rowIndex);
        switch (columnIndex) {
            case 0: return s.token;
            case 1: return s.lexema;
            case 2: return s.tipo;
            case 3: return s.valor;
            case 4: return s.direccion;
        }
        return null;
    }
}

