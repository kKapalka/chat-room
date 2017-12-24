/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

import java.io.PrintWriter;

/**
 *
 * @author kkapa
 */
public class User {
    /**
     * nazwa uzytkownika
     */
    String name;
    /**
     * Kanal wysylania informacji
     */
    PrintWriter OS;
    User(String name, PrintWriter OS){
        this.name=name;
        this.OS=OS;
    }
}
