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
       static final String USER_TAB="Users",LOGIN_COL="login",PASS_COL="pass",EMAIL_COL="email",ID_COL="user_ID";
       //order in USER_TAB: user_ID,pass,login,verified,verification_code,email
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
                                    SendToClient("Error"+DELIMITER+"EmailInUse"+DELIMITER);
                                    break;
                                }
                                int new_id=createNewId();

                                String encoded=Encrypt(data[2]);
                                //Test kodowania hasła według algorytmu SHA-256
                                SendToClient("Error"+DELIMITER+encoded+DELIMITER);
                                int addUser=0;
                                String verifyCode=randomVerifyCode();
                                //VerifyMail(data[3],verifyCode);
                                addUser=update("INSERT INTO \""+USER_TAB+"\" VALUES("+new_id+",'"+encoded+"','"+data[1]+"','");
                            }
                            catch (NullPointerException|NoSuchAlgorithmException ex)
                            {
                                ex.printStackTrace();
                            }
                            break;
                        
                        }
                    
                } 
             } 
             catch (IOException | SQLException ex) 
             {
                 ex.printStackTrace();
                /*parent.ServerTextAppend("Lost a connection. \n");
                parent.clientOutputStreams.remove(client*/
             } 
            
	} 
       private Boolean isEmailInUse(String email) throws SQLException{
               ResultSet rs=query("SELECT * FROM \""+USER_TAB+"\" WHERE \""+EMAIL_COL+"\" LIKE '"+email+"';");
               return rs.next();
       }
       
       private String Encrypt(String text) throws NoSuchAlgorithmException{

           MessageDigest digest = MessageDigest.getInstance("SHA-256");
           byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
           String encoded = bytesToHex(hash);
           
           return encoded;
           
       }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
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

