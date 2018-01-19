/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

/**
 *
 * @author kkapa
 */
public class Column {

    /**
     * nazwa kolumny w bazie danych
     */
    String name;
    /**
     * typ zmiennej kolumny
     */
    String type;

    Column(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
