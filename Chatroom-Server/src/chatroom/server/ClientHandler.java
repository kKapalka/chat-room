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
       static final String USER_TAB="users",LOGIN_COL="login",PASS_COL="pass",EMAIL_COL="email",ID_COL="id",CODE_COL="code",VER_COL="verified";
       //order in USER_TAB: id,login,pass,email,code,verified
       static final String MES_TAB="messages",MES_ID_COL="id",MES_COL="message",TIME_COL="sendtime",SEND_COL="username";
        //order in messages: message_id,user,sendtime,message
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
                            RegisterUser(data);
                            break;
                        case "Verify":
                            VerifyUser(data);
                            break;
                        case "Login":
                            try{
                                if(CheckInUser(data[1],Encrypt(data[2]))){
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
                            int new_id=createNewId(""+parent.messages,parent.messages.Get(0));
                            parent.Insert(""+parent.messages,""+new_id,"'"+data[1]+"'","'"+curDate+"'","'"+data[2]+"'");
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
    private void RegisterUser(String[] data){
        try
        {
            if(CheckInUser("","",data[3])) SendToClient("Error","EMAILINUSE","Ktoś już używa tego adresu e-mail.");
            else if(CheckInUser(data[1])) SendToClient("Error","LOGININUSE","Ten login już jest zajęty. Zmień login.");
            else{
                String verifyCode=randomVerifyCode();
                parent.Insert(""+parent.tab_users,""+createNewId(""+parent.tab_users,parent.tab_users.Get(0)),"'"+data[1]+"'","'"+Encrypt(data[2])+"'","'"+data[3]+"'","'"+verifyCode+"'","false");
                SendToClient("Info","CODE_SENT","Kod weryfikacyjny przesłano na e-mail: "+data[3]);
                //parent.sender.Send(data[3],verifyCode);
            }
        }
        catch (NoSuchAlgorithmException|SQLException ex)
        {
            parent.ServerTextAppend("Błąd w sekwencji rejestracji\n");
        }
    }
    private void VerifyUser(String[] data){
        try{
            if(!CheckInUser(data[1],Encrypt(data[2]))) SendToClient("Error","CRED_INVALID","Nie istnieje klient z takimi danymi");
            else{
                if(CheckInUser(data[1],Encrypt(data[2]),data[3],"","true")) SendToClient("Info","ALREADY_DONE","Klient o takich danych był już zarejestrowany.");

                else if(CheckInUser(data[1],"","",data[4])){
                    parent.Update(""+parent.tab_users,new String[]{parent.tab_users.Get(1)+" LIKE '"+data[1]+"'"},parent.tab_users.Get(5)+"=true");
                    SendToClient("Info","VER_SUCCESS","Weryfikacja zakończona sukcesem. Można się zalogować.");
                } else SendToClient("Error","CODE_INVALID","Błędny kod weryfikacyjny. Sprawdź swoją skrzynkę pocztową.");

            }   
        } catch(NoSuchAlgorithmException ex){
            parent.ServerTextAppend("Błąd w sekwencji weryfikacji\n");
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
    
    public void SendToClient(CharSequence... elements){
        String text=String.join(DELIMITER, elements);
        client.println(text);
        client.flush();
    }
    
    /**
     * 
     * @param data - informacje do sprawdzenia w tabeli użytkowników, w kolejności: login, hasło,email,kod weryfikacji, czy jest zweryfikowane
     * @return true jeżeli dany rekord istnieje w tabeli
     */
    public Boolean CheckInUser(String... data){
        if (data.length==0 || data.length>5) return null;
        int size=0;
        for(String temp:data) if(!"".equals(temp)) size++;
        String[] conditions=new String[size];
        int j=0;
        for(int i=0;i<data.length;i++){
            if(!"".equals(data[i]) && i<4) conditions[j++]=parent.tab_users.Get(i+1)+" LIKE '"+data[i]+"'";
            else if(i==4) conditions[j++]=parent.tab_users.Get(i+1)+"=true";
        }
        return parent.CheckIn(""+parent.tab_users, conditions);
    }
    private void SendFullChat() throws SQLException{
        ResultSet rs=parent.Select(MES_TAB,new String[]{},"*");
        while(rs.next()){
            SendToClient("Chat",rs.getTimestamp(parent.messages.Get(2)).toString(),rs.getString(parent.messages.Get(1)),"  "+rs.getString(parent.messages.Get(3)));
            
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
        ResultSet rs=parent.Select(table,new String[]{},"max("+id_col+")");
        int new_id=0;
        while(rs.next()) new_id=rs.getInt("max")+1;
        return new_id;
    }
    
}

