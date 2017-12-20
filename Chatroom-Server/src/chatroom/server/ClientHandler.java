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
import java.util.Arrays;
import java.util.regex.Pattern;
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
                
                parent.ServerTextAppend("Błąd podczas łączenia z klientem\n");
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
                                if(ExistsIn(USER_TAB,new String[]{EMAIL_COL+"..equal.."+data[3]}))
                                {
                                    SendToClient(String.join(DELIMITER,"Error","EMAILINUSE","Ktoś już używa tego adresu e-mail."));
                                    break;
                                }
                                else if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1]})){
                                    SendToClient(String.join(DELIMITER,"Error","LOGININUSE","Ten login już jest zajęty. Zmień login."));
                                    break;
                                }
                                
                                int new_id=createNewId();
                                String encoded=Encrypt(data[2]);
                                String verifyCode=randomVerifyCode();
                                int addUser=InsertTo(USER_TAB, new String[]{""+new_id,encoded,data[1],verifyCode,data[3],"false"});
                                SendToClient("Info","CODE_SENT","Kod weryfikacyjny przesłano na e-mail: "+data[3]);
                            
                                parent.sender.Send(data[3],verifyCode);
                            }
                            catch (NullPointerException|NoSuchAlgorithmException ex)
                            {
                                parent.ServerTextAppend("Błąd w sekwencji rejestracji\n");
                            }
                            break;
                        case "Verify":
                            try{
                                String encoded=Encrypt(data[2]);
                                if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1],PASS_COL+"..equal.."+encoded,EMAIL_COL+"..equal.."+data[3]})){
                                    if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1],VER_COL+"..bool.."+"true"})){
                                        SendToClient("Info","ALREADY_DONE","Klient o takich danych był już zarejestrowany.");  
                                    }
                                    else if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1],CODE_COL+"..equal.."+data[4]})){
                                        int updated=newupdate("Update",new String[]{"\""+VER_COL+"\"=true"},USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1]});
                                        if(updated==1) SendToClient("Info","VER_SUCCESS","Weryfikacja zakończona sukcesem. Można się zalogować.");
                                    } else{
                                        SendToClient("Error","CODE_INVALID","Błędny kod weryfikacyjny. Sprawdź swoją skrzynkę pocztową.");
                                    }
                                }
                                else SendToClient("Error","CRED_INVALID","Nie istnieje klient z takimi danymi");
                            } catch(NoSuchAlgorithmException |SQLException ex){
                                parent.ServerTextAppend("Błąd w sekwencji weryfikacji\n");
                            }
                            break;
                        case "Login":
                            try{
                                String encoded=Encrypt(data[2]);
                                if(!ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1],PASS_COL+"..equal.."+encoded})){
                                    SendToClient("Login");
                                    
                                }else{
                                    SendToClient("Error","USER_INVALID","Nieprawidłowe dane logowania");
                                }
                            } catch (NoSuchAlgorithmException |SQLException ex) {
                               parent.ServerTextAppend("Błąd w sekwencji logowania\n");
                            }
                        }
                    
                    
                } 
             } 
             catch (IOException | SQLException ex) 
             {
                
                parent.ServerTextAppend("Utracono połączenie. \n");
                //parent.clientOutputStreams.remove(client*/
             } 
            
        } 
      
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
    
    private Boolean ExistsIn(String table, String[] conditions) throws SQLException{
        ResultSet rs=newquery("Check",new String[]{},table,conditions);
        return rs.next();
    }
    private int InsertTo(String table, String[] values) throws SQLException{
        int rs=newupdate("Insert",values,table,new String[]{});
        return rs;
    }
    public void SendToClient(CharSequence... elements){
        String text=String.join(DELIMITER, elements);
        client.println(text);
        client.flush();
    }
    
    /**
     * 
     * @param type przyjmuje "Insert" - wstaw, "Delete"-usuń, "Update"-zaktualizuj
     * @param values - tabela wartości do wstawienia lub zmienienia
     * @param table - tablica bazy danych do zmodyfikowania
     * @param conditions - warunki, zapisane formatem "Kolumna..equal..Wartość"
     * @return ilość zmodyfikowanych wierszy
     * @throws SQLException jeżeli wystąpi błąd z połączeniem z bazą danych
     */
    private int newupdate(String type, String[] values, String table, String[] conditions) throws SQLException{
        parent.statement=parent.conn.createStatement();
        String sql="";
        switch(type){
            case "Insert":
                sql+="INSERT INTO \""+table+"\" VALUES (";
                for(String temp:values){
                    if(!temp.equals(values[0])) sql+=", ";
                    try{
                        sql+=""+Integer.parseInt(temp);
                    }catch (NumberFormatException ex){
                        if ("true".equals(temp) ||"false".equals(temp)) sql+=""+temp;
                        else sql+="'"+temp+"'";
                    }
                }
                sql+=");";
                int rs=parent.statement.executeUpdate(sql);
                return rs;
                
            case "Delete":
                sql+="DELETE FROM \""+table+"\" WHERE ";
                break;
            case "Update":
                sql+="UPDATE \""+table+"\" SET ";
                for(String temp:values){
                    if(!temp.equals(values[0])) sql+=", ";
                    sql+=temp;
                }
                sql+=" WHERE ";
                break;
        }
        for(String temp:conditions){
            if(!temp.equals(conditions[0])) sql+=" AND ";  
                String[] data=temp.split(Pattern.quote(".."));
                sql+="\""+data[0]+"\"";
                switch(data[1]){
                    case "equal":
                        sql+=" LIKE ";
                        break;
                }
                sql+="'"+data[2]+"'";
            }
            sql+=";";
            int rs=parent.statement.executeUpdate(sql);
            return rs;
    }
    private ResultSet newquery(String type, String[] values, String table, String[] conditions) throws SQLException{
        String sql="";
        switch(type){
            case "Check":
                sql+="SELECT *";
                break;
            case "Fetch":
                sql+="SELECT ";
                for(String temp:values){
                    if(!temp.equals(values[0])) sql+=", ";
                    sql+=temp;
                }
                break;
        }
        sql+=" FROM \""+table+"\"";
        if(conditions.length>0){
            sql+=" WHERE ";
            for(String temp:conditions){
                if(!temp.equals(conditions[0])) sql+=" AND ";  
                String[] data=temp.split(Pattern.quote(".."));
                sql+="\""+data[0]+"\"";
                switch(data[1]){
                    case "equal":
                        sql+=" LIKE ";
                        sql+="'"+data[2]+"'";
                        break;
                    case "bool":
                        sql+="="+data[2];
                }
                
            }
        }
        sql+=";";
        parent.statement=parent.conn.createStatement();
        ResultSet rs=parent.statement.executeQuery(sql);
        return rs;
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
        ResultSet rs=newquery("Fetch",new String[]{"max(\""+ID_COL+"\")"},USER_TAB,new String[]{});
        int new_id=0;
        while(rs.next()) new_id=rs.getInt("max")+1;
        return new_id;
    }
    
}

