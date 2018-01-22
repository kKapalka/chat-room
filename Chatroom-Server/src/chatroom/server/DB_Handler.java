/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

/**
 * Klasa odpowiedzialna za generowanie bazy komunikacje i zbieranie informacji
 * @author kkapa
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DB_Handler {

    String server = "jdbc:postgresql://localhost:5432/";
    String dbname = "";
    String user, pass;
    Statement statement;
    Connection conn;
    ResultSet rs;
    ChatroomServer parent;

    DB_Handler(String server, String user, String pass, ChatroomServer parent) throws SQLException {
        this.dbname = server;
        this.user = user;
        this.pass = pass;
        this.parent = parent;

        conn = DriverManager.getConnection(this.server, user, pass);

        CreateDB();
        CreateTables();

        conn = DriverManager.getConnection(this.server + this.dbname, user, pass);
        Init();
    }

    private void Init() throws SQLException {
        ArrayList<Table> tables = GetAllTables();

        tables.forEach((temp) -> {
            try {
                GetAllColumns(temp);
            } catch (SQLException ex) {
                Logger.getLogger(ChatroomServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (null != "" + temp) {
                switch ("" + temp) {
                    case "users":
                        parent.tab_users = temp;
                        break;
                    case "messages":
                        parent.messages = temp;
                        break;
                    case "mutes":
                        parent.mutes = temp;
                        break;
                    default:
                        break;
                }
            }
        });

    }

    private ArrayList<Table> GetAllTables() throws SQLException {
        ArrayList<Table> tables = new ArrayList<>();
        ResultSet rs = conn.getMetaData().getTables("Chatroom", null, "%", null);
        while (rs.next()) {
            if (rs.getString(4) != null && rs.getString(4).equalsIgnoreCase("TABLE")) {
                tables.add(new Table(rs.getString(3)));
            }
        }
        return tables;
    }

    private void GetAllColumns(Table table) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("select * from " + table);
        ResultSetMetaData rsmd = rs.getMetaData();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            table.addColumn(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
        }
    }
    /**
     * Funkcja generujaca baze danych
     */
    private void CreateDB() {
        try {

            statement = conn.createStatement();
            String namecheck = dbname.replaceAll("[^a-z0-9_]", "");
            if (dbname == null ? namecheck != null : !dbname.equals(namecheck)) {
                statement.executeUpdate("CREATE DATABASE \"" + dbname + "\";");
            } else {
                statement.executeUpdate("CREATE DATABASE " + dbname + ";");
            }
        } catch (SQLException ex) {

        }
    }
    /**
     * Funkcja generujaca tabele wewnatrz bazy danych
     */
    private void CreateTables() {
        try {
            conn = DriverManager.getConnection(server+dbname, user, pass);
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet tables = dbm.getTables(null, null, "messages", null);
            if (!tables.next()) {
                statement = conn.createStatement();
                statement.executeUpdate("CREATE TABLE messages (id int not null primary key, username character varying(30), sendtime timestamp without time zone, message text);");
            }
            tables = dbm.getTables(null, null, "users", null);
            if (!tables.next()) {
                statement = conn.createStatement();
                statement.executeUpdate("CREATE TABLE users (id int not null primary key, login character varying(30), pass character(64), email character varying(80), code character(10), verified boolean);");
            }
            tables = dbm.getTables(null, null, "mutes", null);
            if (!tables.next()) {
                statement = conn.createStatement();
                statement.executeUpdate("CREATE TABLE mutes (id int not null primary key, muter character varying(30), muted character varying(30));");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    /**
     * <p> Niskopoziomowa funkcja obslugujaca aktualizacje bazy danych: wstawianie, modyfikacje, usuwanie wartosci</p>
     * @param sql tresc zapytania aktualizujacego
     */
    public void updateDatabase(String sql) {
        int res;
        try {
            System.out.println(sql);
            statement = conn.createStatement();
            res = statement.executeUpdate(sql);
        } catch (SQLException ex) {
            parent.ServerTextAppend("Blad w trakcie aktualizacji tabeli.");
        }
    }
    /**
     * <p> Niskopoziomowa funkcja obslugujaca zapytania do bazy danych</p>
     * @param sql tresc zapytania
     * @return zestaw wynikowy zapytania
     */
    public ResultSet queryDatabase(String sql) {
        try {
            statement = conn.createStatement();
            ResultSet res = statement.executeQuery(sql);
            return res;
        } catch (SQLException ex) {
            parent.ServerTextAppend("Blad podczas wybierania wartosci z bazy.");
            ex.printStackTrace();
        }
        return null;
    }

}
