/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

import static chatroom.server.ChatroomServer.DELIMITER;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author kkapa
 */
public class ClientHandler implements Runnable	
   {
       BufferedReader reader;
       Socket sock;
       PrintWriter client;
       ChatroomServer parent;
       //Tablenames and Column_names used in our custom database - defining constants. Set according to your own database
       static final String USER_TAB="Users",LOGIN_COL="login",PASS_COL="pass",EMAIL_COL="email",ID_COL="user_ID",CODE_COL="verification_code",VER_COL="verified";
       //order in USER_TAB: user_ID,pass,login,verification_code,email,verified
       //order in Messages: message_id,user_id,message,timestamp_sent
    public ClientHandler(Socket clientSocket, PrintWriter user, ChatroomServer par) 
       {
            parent=par;
            client = user;
            try 
            {
                sock = clientSocket;
                InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(isReader);
            }
            catch (IOException ex) 
            {
                ex.printStackTrace();
                parent.ServerTextAppend("Unexpected error... \n");
            }
       }

       @Override
       public void run() 
       {
            String message;
            String[] data;
            try 
            {
                while ((message = reader.readLine()) != null) 
                {
                    parent.ServerTextAppend("Otrzymano: " + message + "\n");
                    String curDate=getDate();
                    data = message.split(DELIMITER);

                    switch(data[0]){
                        case "Register":
                            try
                            {
                                if(isEmailInUse(data[3]))
                                {
                                    SendToClient("Error"+DELIMITER+"EMAILINUSE"+DELIMITER+"Ktoś już używa tego adresu e-mail."+DELIMITER);
                                    break;
                                }
                                else if(isLoginInUse(data[1])){
                                    SendToClient("Error"+DELIMITER+"LOGININUSE"+DELIMITER+"Ten login już jest zajęty. Zmień login."+DELIMITER);
                                    break;
                                }
                                int new_id=createNewId();
                                String encoded=Encrypt(data[2]);
                                String verifyCode=randomVerifyCode();
                                int addUser=update("INSERT INTO \""+USER_TAB+"\" VALUES("+new_id+",'"+encoded+"','"+data[1]+"','"+verifyCode+"','"+data[3]+"',false);");
                                parent.sender.Send(data[3],verifyCode);
                            }
                            catch (NullPointerException|NoSuchAlgorithmException ex)
                            {
                                ex.printStackTrace();
                            }
                            break;
                        case "Verify":
                            try{
                                String encoded=Encrypt(data[2]);
                                if(CredentialsCorrect(data[1],encoded,data[3])){
                                    if(AlreadyVerified(data[1])){
                                        SendToClient("Info"+DELIMITER+"ALREADY_DONE"+DELIMITER+"Klient o takich danych był już zarejestrowany."+DELIMITER);  
                                    }
                                    else if(CheckVerifyCode(data[1],data[4])){
                                        int updated=update("UPDATE \""+USER_TAB+"\" set \""+VER_COL+"\"=true where \""+LOGIN_COL+"\" LIKE '"+data[1]+"';");
                                        if(updated==1) SendToClient("Info"+DELIMITER+"VER_SUCCESS"+DELIMITER+"Weryfikacja zakończona sukcesem. Można się zalogować."+DELIMITER);
                                    } else{
                                        SendToClient("Error"+DELIMITER+"CODE_INVALID"+DELIMITER+"Błędny kod weryfikacyjny. Sprawdź swoją skrzynkę pocztową.");
                                    }
                                }
                                else SendToClient("Error"+DELIMITER+"CRED_INVALID"+DELIMITER+"Nie istnieje klient z takimi danymi");
                            } catch(NoSuchAlgorithmException |SQLException ex){
                                ex.printStackTrace();
                            }
                            
                        }
                    
                } 
             } 
             catch (IOException | SQLException ex) 
             {
                /*parent.ServerTextAppend("Lost a connection. \n");
                parent.clientOutputStreams.remove(client*/
             } 
            
        } 
       /**
        * Funkcja wysyła zapytanie do bazy:
        * <p> Wybierz z tablicy gdzie email jest zgodny z emailem podanym w funkcji</p>
        * @param email - email do sprawdzenia, czy istnieje już w bazie danych
        * @return true, jeżeli zestaw wyników zapytania SQL-owego nie jest pusty, false - w przeciwnym wypadku
        * @throws SQLException - jeżeli nastąpi błąd połączenia z bazą danych
        */
       private Boolean isEmailInUse(String email) throws SQLException{
               ResultSet rs=query("SELECT * FROM \""+USER_TAB+"\" WHERE \""+EMAIL_COL+"\" LIKE '"+email+"';");
               return rs.next();
       }
       /**
        * Funkcja wysyła zapytanie do bazy:
        * <p> Wybierz z tablicy gdzie login jest zgodny z loginem podanym w funkcji</p>
        * @param login - login do sprawdzenia, czy istnieje już w bazie danych
        * @return true, jeżeli zestaw wyników zapytania SQL-owego nie jest pusty, false - w przeciwnym wypadku
        * @throws SQLException - jeżeli nastąpi błąd połączenia z bazą danych
        */
       private Boolean isLoginInUse(String login) throws SQLException{
           ResultSet rs=query("SELECT * from \""+USER_TAB+"\" WHERE \""+LOGIN_COL+"\" LIKE '"+login+"';");
           return rs.next();
       }
       /**
        * Funkcja szyfruje podany do niej tekst algorytmem SHA256
        * @param text - tekst do zaszyfrowania
        * @return 32-bajtowy hash-code zastępujący tekst
        * @throws NoSuchAlgorithmException - gdy program stwierdzi, że taki algorytm nie istnieje
        */
       private String Encrypt(String text) throws NoSuchAlgorithmException{
           MessageDigest digest = MessageDigest.getInstance("SHA-256");
           byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
           String encoded = bytesToHex(hash);
           return encoded.substring(0,32);
       }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
    private Boolean CredentialsCorrect( String login, String pass, String email) throws SQLException{
        ResultSet rs=query("SELECT * from \""+USER_TAB+"\" WHERE \""+LOGIN_COL+"\" LIKE '"+login+"'"
                + " and \""+PASS_COL+"\" LIKE '"+pass+"'and \""+EMAIL_COL+"\" LIKE '"+email+"';");
        return rs.next();
    }
    private Boolean CheckVerifyCode(String login, String code) throws SQLException{
        ResultSet rs=query("Select * from \""+USER_TAB+"\" WHERE\""+LOGIN_COL+"\"Like '"+login+"' and \""+CODE_COL+"\" LIKE '"+code+"';");
        return rs.next();
    }
    private Boolean AlreadyVerified(String login) throws SQLException{
        ResultSet rs=query("Select * from \""+USER_TAB+"\" WHERE\""+LOGIN_COL+"\"Like '"+login+"';");
        String state="";
        while (rs.next()){
            System.out.println(rs.getString(6));
            state=rs.getString(6);
        }
        return ("t".equals(state));
    }
    public void SendToClient(String text){
        client.println(text);
        client.flush();
    }
    private ResultSet query(String sql) throws SQLException{
        parent.statement=parent.conn.createStatement();
        ResultSet rs=parent.statement.executeQuery(sql);
        return rs;
    }
    private int update(String sql) throws SQLException{
        parent.statement=parent.conn.createStatement();
        int updated=parent.statement.executeUpdate(sql);
        return updated;
    }
    private String randomVerifyCode(){
        return Long.toHexString(Double.doubleToLongBits(Math.random())).substring(4,14);
    }
    private String getDate(){
        String datetime=LocalDateTime.now().toString().replace("T"," ");
        datetime=datetime.substring(0,datetime.lastIndexOf("."));
        parent.ServerTextAppend("O: "+datetime+"\n");
        return datetime;
    }
    private int createNewId() throws SQLException{
        ResultSet rs=query("SELECT max(\""+ID_COL+"\") FROM \""+USER_TAB+"\"");
        int new_id=0;
        while(rs.next()) new_id=rs.getInt("max")+1;
        return new_id;
    }
    
}

