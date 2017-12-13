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
       //Tablenames and Column_names used in our custom database - defining constants
       static final String USER_TAB="Users",LOGIN_COL="login",PASS_COL="pass",EMAIL_COL="email",ID_COL="user_ID";
       //End defining
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
                    String date=LocalDateTime.now().toString().replace("T"," ");
                    date=date.substring(0,date.lastIndexOf("."));
                    parent.ServerTextAppend("Otrzymano: " + message + "\n");
                    parent.ServerTextAppend("O: "+date+"\n");
                    data = message.split(DELIMITER);
                    for(String temp:data) System.out.println(temp);
                    if (data[0].equals("Register")) 
                    {
                        try{
                            if(isEmailInUse(data[3])){
                                client.println("Error"+DELIMITER+"EmailInUse"+DELIMITER);
                                client.flush();
                                break;
                            }
                        parent.statement=parent.conn.createStatement();
                        parent.sql="SELECT max(\""+ID_COL+"\") FROM \""+USER_TAB+"\"";
                        ResultSet rs=parent.statement.executeQuery(parent.sql);
                        int new_id=0;
                        while(rs.next()) new_id=rs.getInt("max")+1;
                        System.out.println(new_id);
                        String encoded=Encrypt(data[2]);
                        //Test kodowania hasła według algorytmu SHA-256
                        client.println("Error"+DELIMITER+encoded+DELIMITER);
                        client.flush();
                        /*int addUser=0;
                        try{
                            parent.statement=parent.conn.createStatement();
                            addUser=parent.statement.executeUpdate("INSERT INTO \""+USER_TAB+"\" VALUES("+new_id+",'"+data[1]+"','"+encoded)
                        }*/
                        
                        
                        }catch (NullPointerException npex){
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
       private Boolean isEmailInUse(String email){
           try{
               parent.statement=parent.conn.createStatement();
               parent.sql="SELECT * FROM \""+USER_TAB+"\" WHERE \""+EMAIL_COL+"\" LIKE '"+email+"';";
               System.out.println(parent.sql);
               ResultSet rs=parent.statement.executeQuery(parent.sql);
               return rs.next();
           }catch (SQLException ex){
               parent.ServerTextAppend("Nieoczekiwany błąd.\n");
               return null;
           }
           
       }
       private String Encrypt(String text){
           String encoded="";
           try{
           MessageDigest digest = MessageDigest.getInstance("SHA-256");
           byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
           encoded = bytesToHex(hash);
           }catch (NoSuchAlgorithmException naex){
               parent.ServerTextAppend("Błąd w trakcie kodowania");
           }
           return encoded;
           
       }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
       
     }

