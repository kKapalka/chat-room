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
        /**
         * Kanal odczytu informacji od klienta
         */
       BufferedReader reader;
       /**
        * Adres socketu klienta
        */
       Socket sock;
       /**
        * Kanal zapisu informacji do klienta
        */
       PrintWriter client;
       /**
        * Odnosnik do glownej aplikacji serwera
        */
       ChatroomServer parent;
       /**
        * Login uzytkownika
        */
       String login;

    /**
     * Konstruktor klasy ClientHandler, uzywany przez klase ServerStart przy probie logowania, rejestracji,weryfikacji przez klienta
     * @param clientSocket socket klienta
     * @param user kanal zapisu do klienta
     * @param par odnosnik do glownej aplikacji
     */
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
                                    if(parent.users.contains(new User(data[1],client))){
                                        SendToClient("Error","LOGIN_IN_USE","Użytkownik o takim loginie jest już zalogowany");
                                        SendToClient("Break");
                                    }
                                    else{
                                        SendToClient("Login");
                                        login=data[1];
                                        parent.users.add(new User(data[1],client));
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
                            parent.users.remove(new User(data[1],client));
                            break;
                        case "Message":
                            if(data.length==3){
                                if(HasMore("/mute",data[2]))  Mute(data[2].substring(6));
                                else if (HasMore("/unmute",data[2])) Unmute(data[2].substring(8));
                                else if(data[2].substring(0,1).equals("/")) SendToClient("Chat","Niepoprawna komenda");
                                else{
                                    int new_id=createNewId(""+parent.messages,parent.messages.Get(0));
                                    parent.Insert(""+parent.messages,""+new_id,"'"+data[1]+"'","'"+curDate+"'",data.length<3?"''":"'"+data[2]+"'");
                                    parent.updateChat();
                                }
                            }
                            break;
                    }
                }
             } 
             catch (IOException | SQLException ex) 
             {
                parent.ServerTextAppend("Utracono połączenie. \n");
                parent.users.remove(new User(login,client));
             } 
            
        }
    private Boolean HasMore(String word, String text){
        if(text.length()<word.length()+2) return false;
        return(text.substring(0,word.length()).equals(word));
    }
    
    private void Mute(String username){
        if(!CheckInUser(username)) SendToClient("Chat","Użytkownik "+username+" nie istnieje");
        else{
           try{
               parent.Insert(""+parent.mutes, ""+createNewId(""+parent.mutes,parent.mutes.Get(0)),"'"+this.login+"'","'"+username+"'");
               SendToClient("Chat","Wiadomości od użytkownika: "+username +" są dla ciebie niewidoczne.");
           } catch(SQLException ex){ SendToClient("Error","Błąd połączenia z bazą danych");}
        }
    }
    private void Unmute(String username){
        if(!CheckInUser(username)) SendToClient("Chat","Użytkownik "+username+" nie istnieje");
        else{
               parent.Delete(""+parent.mutes,parent.mutes.Get(1)+" LIKE '"+login+"' AND "+parent.mutes.Get(2)+" LIKE '"+username+"'");
               SendToClient("Chat","Wiadomości od użytkownika: "+username +" są dla ciebie znów widoczne.");
           
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
                parent.sender.Send(data[3],verifyCode);
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
    
    /**
     * Funkcja komponuje i przesyla wiadomosc do uzytkownika.
     * <p>Jest wyroznionych 5 typow wiadomosci:</p>
     * <p>Error - sygnal bledu</p>
     * <p>Info - sygnal informacji</p>
     * <p>Login - sygnal logowania do czatu</p>
     * <p>Message - sygnal przesylania nowej wiadomosci do czatu</p>
     * <p>Break - sygnal przerwania polaczenia z serwerem</p>
     * @param elements fragmenty wiadomosci do polaczenia i przeslania klientowi
     */
    public void SendToClient(CharSequence... elements){
        String text=String.join(DELIMITER, elements);
        client.println(text);
        client.flush();
    }
    
    /**
     * Specyficzny przypadek funkcji Check z klasy ChatroomServer - ma za zadanie sprawdzac wystepowanie klienta w bazie
     * <p> Moze przyjac od 1 do 5 zmiennych typu String. Na ich podstawie komponuje kwerende CheckIn(), gdzie 
     * kazda kolejna kolumne testuje na wystepowanie kazdej kolejnej wartosci.</p>
     * <p> Funkcja korzysta ze stalej kompozycji tabeli uzytkownikow w bazie danych.</p>
     * @param data informacje do sprawdzenia w tabeli użytkowników, w kolejności: login, hasło,email,kod weryfikacji, czy jest zweryfikowane
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
        ResultSet rs=parent.SelectFromChat(new String[]{},login);
        try{
        while(rs.next()){
            SendToClient("Chat",rs.getTimestamp(parent.messages.Get(2)).toString(),rs.getString(parent.messages.Get(1)),"  "+rs.getString(parent.messages.Get(3)));    
        }
        }catch (NullPointerException ex){}
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

