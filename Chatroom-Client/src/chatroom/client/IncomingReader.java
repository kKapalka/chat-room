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
                    System.out.println(stream);
                     data = stream.split(DELIMITER);

                     if (data[0].equals("Error")) 
                     {
                        JOptionPane.showMessageDialog(parent,
                        data[1],
                        "DC_ERROR",
                        JOptionPane.ERROR_MESSAGE);
                     } 
                     
                }
           }catch(HeadlessException | IOException ex) { }
        }
    }
