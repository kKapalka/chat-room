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
    String name;
    public ArrayList<Column> columns;
    Table(String name){
        this.name=name;
        columns=new ArrayList<>();
    }
    public void addColumn(String name, String type){
        columns.add(new Column(name,type));
    }
    @Override
    public String toString(){
        return this.name;
    }
    public String Typeof(String columnname){
        return columns.get(columns.indexOf(columnname)).type;
    }
    public String Get(int index){
        return columns.get(index).name;
    }
}
