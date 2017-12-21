/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.client;

import java.awt.HeadlessException;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 *
 * @author kkapa
 */
 public class IncomingReader implements Runnable
    {
        static final String DELIMITER=";end;";
        ChatroomClient parent;
        IncomingReader(ChatroomClient par){
            parent=par;
        }
        @Override
        public void run() 
        {
            String[] data;
            String stream;

            try 
            {
                while ((stream = parent.reader.readLine()) != null) 
                {
                    
                     data = stream.split(DELIMITER);

                     switch (data[0]) 
                     {
                         case "Error":
                            JOptionPane.showMessageDialog(parent,
                            data[2],
                            data[1],
                            JOptionPane.ERROR_MESSAGE);
                            if("LOGININUSE".equals(data[1])){
                                parent.getLoginPanel().dialog.dispose();
                            }
                            break;
                         case "Info":
                            JOptionPane.showMessageDialog(parent,
                            data[2],
                            data[1],
                            JOptionPane.INFORMATION_MESSAGE);
                            if("VER_SUCCESS".equals(data[1])){
                                parent.getLoginPanel().dialog.dispose();
                            } 
                         case "Login":
                             if (data.length==1)
                             parent.SwitchPanels("Chat");
                             break;
                         case "Chat":
                             String text="";
                             for(String temp:data){
                                 if (!data[0].equals(temp)){
                                     if(!data[1].equals(temp))text+=":";
                                     text+=""+temp;
                                 }
                             }
                             parent.ChatTextAppend(text);
                             break;
                         case "Break":
                             parent.IncomingReader.interrupt();
                             parent.server.close();
                             parent.SwitchPanels("Login");
                             break;
                     } 
                     
                }
           }catch(HeadlessException | IOException ex) { }
        }
    }
