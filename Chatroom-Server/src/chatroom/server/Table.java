/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

import java.util.ArrayList;

/**
 *
 * @author kkapa
 */
public class Table {

    /**
     * nazwa tabeli
     */
    String name;

    /**
     * lista kolumn
     */
    public ArrayList<Column> columns;

    Table(String name) {
        this.name = name;
        columns = new ArrayList<>();
    }

    /**
     * Funkcja dodaje nowa kolumne do listy na podstawie jej nazwy i typu
     * zmiennej
     *
     * @param name nazwa kolumny
     * @param type typ zmiennej kolumny
     */
    public void addColumn(String name, String type) {
        columns.add(new Column(name, type));
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Funkcja zwraca typ zmiennej kolumny na podstawie jej nazwy
     *
     * @param columnname nazwa kolumny w liscie
     * @return typ zmiennej kolumny
     */
    public String Typeof(String columnname) {
        return columns.get(columns.indexOf(columnname)).type;
    }

    /**
     * Funkcja zwraca nazwe kolumny z listy na podstawie jej indeksu
     *
     * @param index indeks kolumny w liscie
     * @return nazwa kolumny
     */
    public String Get(int index) {
        return columns.get(index).name;
    }
}
