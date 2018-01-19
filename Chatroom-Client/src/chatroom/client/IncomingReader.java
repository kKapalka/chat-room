/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.client;

import java.awt.HeadlessException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Klasa odpowiedzialna za odbieranie informacji od serwera, manipulacje panelami na podstawie odebranych informacji
 * @author kkapa
 */
public class IncomingReader implements Runnable {

    static final String DELIMITER = ";end;";
    ChatroomClient parent;

    IncomingReader(ChatroomClient par) {
        parent = par;
    }

    @Override
    public void run() {
        String[] data;
        String stream;
        int i = 0;
        try {
            while ((stream = parent.reader.readLine()) != null) {
                data = stream.split(DELIMITER);

                switch (data[0]) {
                    case "Error":
                    case "Info":
                        parent.Message(data[0], data[1], data[2]);
                        String[] ToDispose = {"LOGININUSE", "VER_SUCCESS"};
                        if (Arrays.asList(ToDispose).contains(data[1])) {
                            parent.getLoginPanel().dialog.dispose();
                        }
                        break;
                    case "Login":
                        parent.SwitchPanels("Chat");
                        parent.logintime = data[1];
                        break;
                    case "Chat":
                        if (data.length > 3) {
                            data[3] = data[3].replace(";apos;", "'");
                        }
                        parent.ChatTextAppend(data);
                        System.out.println(i++);
                        break;
                    case "Break":
                        parent.IncomingReader.interrupt();
                        parent.server.close();
                        parent.IncomingReader = null;
                        parent.SwitchPanels("Login");
                        break;
                }
            }
        } catch (HeadlessException | IOException ex) {
        }
    }
}
