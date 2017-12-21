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
       String login;
       //Tablenames and Column_names used in our custom database - defining constants. Set according to your own database
       static final String USER_TAB="Users",LOGIN_COL="login",PASS_COL="pass",EMAIL_COL="email",ID_COL="user_ID",CODE_COL="verification_code",VER_COL="verified";
       //order in USER_TAB: user_ID,pass,login,verification_code,email,verified
       static final String MES_TAB="Messages",MES_ID_COL="message_id",MES_COL="message",TIME_COL="timestamp_sent",SEND_COL="user";
        //order in Messages: message_id,message,timestamp_sent,user
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
                                    SendToClient("Error","EMAILINUSE","Ktoś już używa tego adresu e-mail.");
                                    break;
                                }
                                else if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1]})){
                                    SendToClient("Error","LOGININUSE","Ten login już jest zajęty. Zmień login.");
                                    break;
                                }
                                
                                int new_id=createNewId(USER_TAB,ID_COL);
                                String encoded=Encrypt(data[2]);
                                String verifyCode=randomVerifyCode();
                                int addUser=InsertTo(USER_TAB, new String[]{""+new_id,encoded,data[1],verifyCode,data[3],"false"});
                                SendToClient("Info","CODE_SENT","Kod weryfikacyjny przesłano na e-mail: "+data[3]);
                            
                                //parent.sender.Send(data[3],verifyCode);
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
                                        int updated=parent.newupdate("Update",new String[]{"\""+VER_COL+"\"=true"},USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1]});
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
                                if(ExistsIn(USER_TAB,new String[]{LOGIN_COL+"..equal.."+data[1],PASS_COL+"..equal.."+encoded})){
                                    if(parent.users.contains(data[1])){
                                        SendToClient("Error","LOGIN_IN_USE","Użytkownik o takim loginie jest już zalogowany");
                                        SendToClient("Break");
                                    }
                                    else{
                                        SendToClient("Login");
                                        login=data[1];
                                        parent.users.add(data[1]);
                                        parent.clientOutputStreams.add(client);
                                        SendFullChat();
                                    }
                                }else{
                                    SendToClient("Error","USER_INVALID","Nieprawidłowe dane logowania");
                                    SendToClient("Break");
                                    
                                }
                            } catch (NoSuchAlgorithmException |SQLException ex) {
                               ex.printStackTrace();
                               parent.ServerTextAppend("Błąd w sekwencji logowania\n");
                            }
                            break;
                        case "Logout": case "Disconnect":
                            SendToClient("Break");
                            parent.users.remove(data[1]);
                            parent.clientOutputStreams.remove(client);
                            break;
                        case "Message":
                            int new_id=createNewId(MES_TAB,MES_ID_COL);
                            parent.newupdate("Insert",new String[]{""+new_id,data[2],curDate,data[1]},MES_TAB,new String[]{});
                            parent.updateChat();
                            break;
                        
                        }
                    
                    
                } 
             } 
             catch (IOException | SQLException ex) 
             {
                ex.printStackTrace();
                parent.ServerTextAppend("Utracono połączenie. \n");
                parent.users.remove(login);
                parent.clientOutputStreams.remove(client);
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
        ResultSet rs=parent.newquery("Check",new String[]{},table,conditions);
        return rs.next();
    }
    private int InsertTo(String table, String[] values) throws SQLException{
        int rs=parent.newupdate("Insert",values,table,new String[]{});
        return rs;
    }
    public void SendToClient(CharSequence... elements){
        String text=String.join(DELIMITER, elements);
        client.println(text);
        client.flush();
    }
    
    private void SendFullChat() throws SQLException{
        ResultSet rs=parent.newquery("Fetch",new String[]{"*"},MES_TAB,new String[]{});
        while(rs.next()){
            SendToClient("Chat",rs.getString(SEND_COL),rs.getTimestamp(TIME_COL).toString(),rs.getString(MES_COL));
            
        }
        SendToClient("Chat","Zalogowano");
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
    private int createNewId(String table,String id_col) throws SQLException{
        ResultSet rs=parent.newquery("Fetch",new String[]{"max(\""+id_col+"\")"},table,new String[]{});
        int new_id=0;
        while(rs.next()) new_id=rs.getInt("max")+1;
        return new_id;
    }
    
}

