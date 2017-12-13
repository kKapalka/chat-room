/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

import static chatroom.server.ChatroomServer.DELIMITER;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.time.LocalDateTime;

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
            catch (Exception ex) 
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
                        parent.sql="SELECT max(\"user_ID\") FROM \"Users\"";
                        ResultSet rs=parent.statement.executeQuery(parent.sql);
                        int new_id=0;
                        while(rs.next()) new_id=rs.getInt("max")+1;
                        System.out.println(new_id);
                        
                        
                        
                        }catch (NullPointerException npex){
                            npex.printStackTrace();
                        }
                    } 
                    /*
                    else if (data[2].equals(disconnect)) 
                    {
                        tellEveryone((data[0] + ":has disconnected." + ":" + chat));
                        userRemove(data[0]);
                    } 
                    else if (data[2].equals(chat)) 
                    {
                        tellEveryone(message);
                    } 
                    else 
                    {
                        ta_chat.append("No Conditions were met. \n");
                    }*/
                } 
             } 
             catch (Exception ex) 
             {
                /*parent.ServerTextAppend("Lost a connection. \n");
                parent.clientOutputStreams.remove(client*/
                 ex.printStackTrace();
             } 
            
	} 
       private Boolean isEmailInUse(String email){
           try{
               parent.statement=parent.conn.createStatement();
               parent.sql="SELECT * FROM \"Users\" WHERE \"email\" LIKE '"+email+"';";
               System.out.println(parent.sql);
               ResultSet rs=parent.statement.executeQuery(parent.sql);
               if(rs.next()) return true;
               else return false;
           }catch (Exception ex){
               parent.ServerTextAppend("Nieoczekiwany błąd.\n");
               ex.printStackTrace();
               return null;
           }
           
       }
     }

