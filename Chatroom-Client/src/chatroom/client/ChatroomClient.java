/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.client;

import java.awt.CardLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import javax.swing.JOptionPane;


/**
 *
 * @author kkapa
 */
public class ChatroomClient extends javax.swing.JFrame {
    int user_id;
    String login,pass;
    Socket server;
    BufferedReader reader;
    PrintWriter writer;
    static final String DELIMITER=";end;";
    Thread IncomingReader;
    /**
     * Creates new form ChatroomClient
     */
    public ChatroomClient() {  
        initComponents();
        setToMiddle();
        }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Login = new chatroom.client.LoginPanel();
        Chat = new chatroom.client.ChatPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.CardLayout());
        getContentPane().add(Login, "card2");
        getContentPane().add(Chat, "card3");

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    public void SwitchPanels(String state){
        CardLayout cards = (CardLayout)(getContentPane().getLayout());
        switch(state){
            case "Chat":
                cards.show(getContentPane(), "card3");
                break;
            case "Login":
                cards.show(getContentPane(), "card2");
                break;
        }
    }
    /**
     * Funkcja wyśrodkowuje pozycję okienka. Jest czysto kosmetyczna.
     */
    private void setToMiddle(){
        setLocation((Toolkit.getDefaultToolkit().getScreenSize().width)/2 - getWidth()/2, (Toolkit.getDefaultToolkit().getScreenSize().height)/2 - getHeight()/2);
    }
    /**
     * Funkcja służy do komunikacji klienta z serwerem. Łączy listę ciągów znakowych w jedną wiadomość
     * (z elementami składowymi rozdzielonymi delimiterem ";end;"), wysyła ją do serwera
     * i nasłuchuje odpowiedzi.
     * <p> W razie gdy to jest potrzebne (podczas rejestracji, weryfikacji i logowania) funkcja dodatkowo próbuje połączyć
     * się z serwerem. Gdy to się nie uda, wiadomość po prostu nie zostanie przesłana</p>
     * @param data - rozszerzalna lista ciągów znaków do przesłania do serwera
     */
    public void SendData(String... data){
        String message=String.join(DELIMITER,data);
        
        final String[] ConnectOn={"Login","Register","Verify"};
        if(Arrays.asList(ConnectOn).contains(data[0])){
            LoginAs(data[1]);
            Connect();
        }
        SendAndListen(message);         
    }
    /**
     * Funkcja służy do wysyłania wiadomości i oczekiwania odpowiedzi od serwera
     * @param message - wiadomość do przesłania do serwera
     */
    private void SendAndListen(String message){
        if(server.isConnected()){
            writer.println(message);
            writer.flush();
            Listen();
        }
    }
    /**
     * Funkcja ma za zadanie ustawić login użytkownika wewnątrz aplikacji, oraz wyczyścić czat
     * przed jego ponownym napełnieniem przez serwer
     * @param login - login użytkownika
     */
    private void LoginAs(String login){
        Chat.setUserName(login);
        this.login=login;
        Chat.Clear();
    }
    /**
     * Funkcja najpierw sprawdza, czy serwer jest już aktywny - gdy jest, to nic nie robi.
     * W przeciwnym wypadku ustawia adresy: serwera, strumienia wejścia, odczytu i wyjścia.
     * Jeżeli po drodze napotka błąd, to informuje o tym klienta wyrzucając okno dialogowe.
     */
    public void Connect(){
        if(ServerIsActive())return;
        try{
        server=new Socket("localhost",2222);
        InputStreamReader streamreader = new InputStreamReader(server.getInputStream());
        reader = new BufferedReader(streamreader);
        writer = new PrintWriter(server.getOutputStream());
        } catch (IOException ex){
            ErrorMessage("CONN_ERROR","Nie udało się połączyć");
        }
    }
    /**
     * Test - czy serwer jest aktywny
     * @return (serwer jest zainicjalizowany i nie jest zamknięty)
     */
    public Boolean ServerIsActive(){
        return !(server==null || server.isClosed());
    }
    /**
     * Funkcja wyświetla po stronie klienta okno błędu, z wybranym tytułem i wiadomością
     * @param title - tytuł okna dialogowego
     * @param message - wiadomość w oknie dialogowym
     */
    public void ErrorMessage(String title, String message){
        JOptionPane.showMessageDialog(this,
            message,
            title,
            JOptionPane.ERROR_MESSAGE);
    }
    /**
     * Funkcja uruchamia nowy wątek zawierający klasę IncomingReader - służącą do nasłuchiwania i odbierania wiadomości przez serwer
     */
    private void Listen(){
        IncomingReader = new Thread(new IncomingReader(this));
        IncomingReader.start();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Windows look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ChatroomClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            ChatroomClient client=new ChatroomClient();
            client.setVisible(true);
            client.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    if(client.server!=null && !client.server.isClosed() )client.SendData("Disconnect"+DELIMITER+(client.login==null?"anon":client.login)+DELIMITER);
                }
            });
        });
    }
/**
 * Funkcja jest używana tylko na potrzeby gładkiego powrotu użytkownika do panelu logowania.
 * <p> Gdy serwer stwierdzi w trakcie logowania, że login jest zajęty, to panel rejestracji
 * powinien się zamknąć. W tym celu klasa IncomingReader musi przechwycić odnośnik do prywatnego
 * elementu klasy ChatroomClient
 * @return odnośnik do panelu logowania
 */
public LoginPanel getLoginPanel(){
    return this.Login;
}
/**
 * Funkcja ma za zadanie umożliwić innym klasom wewnątrz aplikacji dostęp do panelu czatu
 * <p> Dzięki temu klasa IncomingReader może wypisywać nowe wiadomości w oknie czatu</p>
 * @param text - tekst do podpięcia do okna czatu
 */
public void ChatTextAppend(String text){
    Chat.TextAppend(text);
}    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private chatroom.client.ChatPanel Chat;
    private chatroom.client.LoginPanel Login;
    // End of variables declaration//GEN-END:variables
}
