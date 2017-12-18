/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author kkapa
 */
public class ServerStart implements Runnable 
    {
        ChatroomServer parent;
        ServerStart(ChatroomServer par){
            parent=par;
        }
        @Override
        public void run() 
        {
            //clientOutputStreams = new ArrayList();
            //users = new ArrayList();  

            try 
            {
                
                ServerSocket serversocket = new ServerSocket(2222);
                while (true) 
                {
				Socket clientsocket=serversocket.accept();
				PrintWriter out = new PrintWriter(clientsocket.getOutputStream(), true);
                                BufferedReader in = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));
                                Thread listener = new Thread(new ClientHandler(clientsocket, out, parent));
				listener.start();
                }
            }
            catch (IOException ex)
            {
                parent.ServerTextAppend("Error making a connection. \n");
            }
        }
    }