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
 * Klasa odpowiedzialna za odbieranie komunikatow od klienta, specyficzne zapytania do bazy danych
 * @author kkapa
 */
public class ClientHandler implements Runnable {

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
     * Konstruktor klasy ClientHandler, uzywany przez klase ServerStart przy
     * probie logowania, rejestracji,weryfikacji przez klienta
     *
     * @param clientSocket socket klienta
     * @param user kanal zapisu do klienta
     * @param par odnosnik do glownej aplikacji
     */
    public ClientHandler(Socket clientSocket, PrintWriter user, ChatroomServer par) {
        parent = par;
        client = user;

        try {
            sock = clientSocket;
            InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(isReader);
        } catch (IOException ex) {
            parent.ServerTextAppend("Błąd podczas łączenia z klientem\n");
        }
    }

    @Override
    public void run() {
        String message;
        String[] data;
        try {
            while ((message = reader.readLine()) != null) {
                parent.ServerTextAppend("Otrzymano: " + message + "\n");
                String curDate = getDate();

                data = message.split(DELIMITER);
                switch (data[0]) {
                    case "Register":
                        RegisterUser(data);
                        break;
                    case "Verify":
                        VerifyUser(data);
                        break;
                    case "Login":
                        try {
                            if (CheckInUser(data[1], Encrypt(data[2]))) {
                                if (parent.users.contains(new User(data[1], client))) {
                                    SendToClient("Error", "LOGIN_IN_USE", "Użytkownik o takim loginie jest już zalogowany");
                                    SendToClient("Break");
                                } else if (!CheckInUser(data[1], "", "", "", "true")) {
                                    SendToClient("Error", "USER_INVALID", "Klient o takich danych jeszcze nie jest zweryfikowany.");
                                    SendToClient("Break");
                                } else {
                                    SendToClient("Login", curDate);
                                    login = data[1];
                                    parent.users.add(new User(data[1], client));
                                    SendChat(curDate);
                                }
                            } else {
                                SendToClient("Error", "USER_INVALID", "Nieprawidłowe dane logowania");
                                SendToClient("Break");

                            }
                        } catch (NoSuchAlgorithmException | SQLException ex) {
                            ex.printStackTrace();
                            parent.ServerTextAppend("Błąd w sekwencji logowania\n");
                        }
                        break;
                    case "Logout":
                    case "Disconnect":
                        SendToClient("Break");
                        parent.users.remove(new User(data[1], client));
                        break;
                    case "Message":
                        if (data.length >= 3) {
                            if (HasMore("/mute", data[2])) {
                                Mute(data[2].substring(6));
                            } else if (HasMore("/unmute", data[2])) {
                                Unmute(data[2].substring(8));
                            } else if ("/show history".equals(data[2])) {
                                SendFullChat(data[3]);
                            } else if ("/hide history".equals(data[2])) {
                                SendChat(data[3]);
                            } else if (data[2].substring(0, 1).equals("/")) {
                                SendToClient("Chat", "Niepoprawna komenda");
                            } else {
                                int new_id = createNewId("" + parent.messages, parent.messages.Get(0));
                                parent.Insert("" + parent.messages, "" + new_id, "'" + data[1] + "'", "'" + curDate + "'", "'" + data[2] + "'");
                                parent.updateChat();
                            }
                        }
                        break;
                }
            }
        } catch (IOException | SQLException ex) {
            parent.ServerTextAppend("Utracono połączenie. \n");
            parent.users.remove(new User(login, client));
        }

    }

    /**
     * Funkcja uzywana do implementacji '/mute' i '/unmute', gdzie po spacji wprowadza sie nick uzytkownika
     * @param word - sprawdzany ciag znakow
     * @param text - ciag znakow, do ktorego 'word' jest porownywany
     * @return true jezeli sprawdzany ciag znakow ma ten sam poczatek co 'text' i ma co najmniej dwa znaki wiecej
     */
    private Boolean HasMore(String word, String text) {
        if (text.length() < word.length() + 2) {
            return false;
        }
        return (text.substring(0, word.length()).equals(word));
    }
    /**
     * <p>Funkcja umozliwia blokowanie wiadomosci od niektorych uzytkownikow komenda '/mute nazwa_uzytkownika'.</p>
     * <p>Dodaje do bazy danych, do tabeli 'mutes' nowy rekord zawierajacy osobe blokujaca i blokowana.</p>
     * <p>Technicznie jest mozliwe wielokrotne zablokowanie tego samego uzytkownika, gdyz przy probie odblokowania
     * i tak usuwane są wszystkie rekordy zawierajace ta osobe blokowana i blokująca.</p>
     * @param username - nazwa użytkownika do zablokowania
     */
    private void Mute(String username) {
        if (!CheckInUser(username)) {
            SendToClient("Chat", "Użytkownik " + username + " nie istnieje");
        }
        else if (username == null ? login == null : username.equals(login)){
            SendToClient("Chat", "Nie możesz zablokować siebie");
        }else {
            try {
                parent.Insert("" + parent.mutes, "" + createNewId("" + parent.mutes, parent.mutes.Get(0)), "'" + this.login + "'", "'" + username + "'");
                SendToClient("Chat", "Wiadomości od użytkownika: " + username + " są dla ciebie niewidoczne.");
            } catch (SQLException ex) {
                SendToClient("Error", "Błąd połączenia z bazą danych");
            }
        }
    }
    /**
     * <p>Funkcja umozliwia odblokowanie wiadomosci od uprzednio zablokowanych uzytkownikow komenda '/unmute nazwa_uzytkownika'.</p>
     * <p> Usuwa z bazy danych rekordy zawierajace dana osobe blokowana i blokująca.</p>
     * @param username - nazwa użytkownika do odblokowania
     */
    private void Unmute(String username) {
        if (!CheckInUser(username)) {
            SendToClient("Chat", "Użytkownik " + username + " nie istnieje");
        }
        else if (username == null ? login == null : username.equals(login)){
            SendToClient("Chat", "I tak nie możesz zablokować siebie");
        } else {
            parent.Delete("" + parent.mutes, parent.mutes.Get(1) + " LIKE '" + login + "' AND " + parent.mutes.Get(2) + " LIKE '" + username + "'");
            SendToClient("Chat", "Wiadomości od użytkownika: " + username + " są dla ciebie znów widoczne.");

        }
    }
    /**
     * <p> Funkcja ma za zadanie zarejestrowac uzytkownika w bazie danych</p>
     * <p> Najpierw sprawdza, czy email jest w uzyciu. Potem, czy login. Jesli te dwa sa wolne, to generuje kod weryfikacyjny,
     * tworzy rekord w bazie danych i wysyla kod na podany wczesniej email.</p>
     * @param data - ciag informacji o uzytkowniku do rejestracji: login, haslo, email
     */
    private void RegisterUser(String[] data) {
        try {
            if (CheckInUser("", "", data[3])) {
                SendToClient("Error", "EMAILINUSE", "Ktoś już używa tego adresu e-mail.");
            } else if (CheckInUser(data[1])) {
                SendToClient("Error", "LOGININUSE", "Ten login już jest zajęty. Zmień login.");
            } else {
                String verifyCode = randomVerifyCode();
                parent.Insert("" + parent.tab_users, "" + createNewId("" + parent.tab_users, parent.tab_users.Get(0)), "'" + data[1] + "'", "'" + Encrypt(data[2]) + "'", "'" + data[3] + "'", "'" + verifyCode + "'", "false");
                SendToClient("Info", "CODE_SENT", "Kod weryfikacyjny przesłano na e-mail: " + data[3]);
                parent.sender.Send(data[3], verifyCode);
            }
        } catch (NoSuchAlgorithmException | SQLException ex) {
            parent.ServerTextAppend("Błąd w sekwencji rejestracji\n");
        }
    }
    /**
     * <p> Funkcja ma za zadanie zweryfikowac, ze dany email naprawde nalezy do danego uzytkownika</p>
     * <p> Najpierw sprawdza czy istnieje dany klient w bazie. Potem, czy klient juz byl zweryfikowany.</p>
     * <p> Nastepnie sprawdza czy do danego loginu jest przypisany wprowadzony przez uzytkownika kod. Jesli tak, to
     * klient jest zweryfikowany pomyslnie. Jesli nie, to wprowadzil bledny kod.</p>
     * @param data - ciag informacji o uzytkowniku, po kolei: login, haslo, email, kod weryfikacyjny
     */
    private void VerifyUser(String[] data) {
        try {
            if (!CheckInUser(data[1], Encrypt(data[2]))) {
                SendToClient("Error", "CRED_INVALID", "Nie istnieje klient z takimi danymi");
            } else {
                if (CheckInUser(data[1], Encrypt(data[2]), data[3], "", "true")) {
                    SendToClient("Info", "ALREADY_DONE", "Klient o takich danych był już zarejestrowany.");
                } else if (CheckInUser(data[1], "", "", data[4])) {
                    parent.Update("" + parent.tab_users, new String[]{parent.tab_users.Get(1) + " LIKE '" + data[1] + "'"}, parent.tab_users.Get(5) + "=true");
                    SendToClient("Info", "VER_SUCCESS", "Weryfikacja zakończona sukcesem. Można się zalogować.");
                } else {
                    SendToClient("Error", "CODE_INVALID", "Błędny kod weryfikacyjny. Sprawdź swoją skrzynkę pocztową.");
                }

            }
        } catch (NoSuchAlgorithmException ex) {
            parent.ServerTextAppend("Błąd w sekwencji weryfikacji\n");
        }
    }
    /**
     * <p> Funkcja sluzy do szyfrowania tekstu algorytmem SHA-256</p>
     * @param text - tekst do zaszyfrowania
     * @return zaszyfrowany tekst
     * @throws NoSuchAlgorithmException
     */
    private String Encrypt(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
        
    }
    /**
     * Funkcja pomocnicza do funkcji Encrypt() - ma za zadanie przekonwertowac ciag bajtow na ciag znakow hexadecymalnych</p>
     * @param bytes - tabela bajtow wchodzacych
     * @return ciag znakow
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * Funkcja komponuje i przesyla wiadomosc do uzytkownika.
     * <p>
     * Jest wyroznionych 5 typow wiadomosci:</p>
     * <p>
     * Error - sygnal bledu</p>
     * <p>
     * Info - sygnal informacji</p>
     * <p>
     * Login - sygnal logowania do czatu</p>
     * <p>
     * Message - sygnal przesylania nowej wiadomosci do czatu</p>
     * <p>
     * Break - sygnal przerwania polaczenia z serwerem</p>
     *
     * @param elements fragmenty wiadomosci do polaczenia i przeslania klientowi
     */
    public void SendToClient(CharSequence... elements) {
        String text = String.join(DELIMITER, elements);
        client.println(text);
        client.flush();
    }

    /**
     * Specyficzny przypadek funkcji Check z klasy ChatroomServer - ma za
     * zadanie sprawdzac wystepowanie klienta w bazie
     * <p>
     * Moze przyjac od 1 do 5 zmiennych typu String. Na ich podstawie komponuje
     * kwerende CheckIn(), gdzie kazda kolejna kolumne testuje na wystepowanie
     * kazdej kolejnej wartosci.</p>
     * <p>
     * Funkcja korzysta ze stalej kompozycji tabeli uzytkownikow w bazie
     * danych.</p>
     *
     * @param data informacje do sprawdzenia w tabeli użytkowników, w
     * kolejności: login, hasło,email,kod weryfikacji, czy jest zweryfikowane
     * @return true jeżeli dany rekord istnieje w tabeli
     */
    public Boolean CheckInUser(String... data) {
        if (data.length == 0 || data.length > 5) {
            return null;
        }
        int size = 0;
        for (String temp : data) {
            if (!"".equals(temp)) {
                size++;
            }
        }
        String[] conditions = new String[size];
        int j = 0;
        for (int i = 0; i < data.length; i++) {
            if (!"".equals(data[i]) && i < 4) {
                conditions[j++] = parent.tab_users.Get(i + 1) + " LIKE '" + data[i] + "'";
            } else if (i == 4) {
                conditions[j++] = parent.tab_users.Get(i + 1) + "=true";
            }
        }
        return parent.CheckIn("" + parent.tab_users, conditions);
    }
    /**
     * <p> Funkcja jest wywolywana podczas aktywacji wyswietlania historii czatu.</p>
     * <p> Najpierw wybiera z bazy danych te wiadomosci ktore byly wyslane przed logowaniem uzytkownika, i wysyla je do czatu</p>
     * <p> Potem wywoluje funkcje SendChat(time)
     * @param time - czas logowania uzytkownika
     * @throws SQLException 
     */
    private void SendFullChat(String time) throws SQLException {
        ResultSet rs = parent.SelectFromChat(new String[]{"sendtime < '" + time + "'"}, login);
        try {
            while (rs.next()) {
                SendToClient("Chat", rs.getTimestamp(parent.messages.Get(2)).toString(), rs.getString(parent.messages.Get(1)), "  " + rs.getString(parent.messages.Get(3)));
            }
        } catch (NullPointerException ex) {
        }
        SendChat(time);
    }
    /**
     * Funkcja najpierw wysyla do uzytkownika komunikat o pomyslnym logowaniu. Potem wybiera z bazy danych aktualne wiadomosci, i je wysyla do czatu.
     * <p> Funkcja jest wywolywana przy logowaniu oraz podczas chowania historii czatu</p>
     * @param time czas logowania uzytkownika
     * @throws SQLException 
     */
    private void SendChat(String time) throws SQLException {
        SendToClient("Chat", "Zalogowano. Aby poznać komendy czatu wpisz /help");
        ResultSet rs=parent.Select(""+parent.mutes, new String[]{parent.mutes.Get(1)+" LIKE '"+login+"'"},parent.mutes.Get(2));
        try{
            Boolean ismuted=false;
            String mutedUsers="";
            while (rs.next()){
                ismuted=true;
                mutedUsers+=rs.getString(parent.mutes.Get(2))+" ";
            }
            if(ismuted) SendToClient("Chat", "Lista zablokowanych uzytkownikow: "+mutedUsers);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        
        rs = parent.SelectFromChat(new String[]{"sendtime >= '" + time + "'"}, login);
        try {
            while (rs.next()) {
                SendToClient("Chat", rs.getTimestamp(parent.messages.Get(2)).toString(), rs.getString(parent.messages.Get(1)), "  " + rs.getString(parent.messages.Get(3)));
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    private String randomVerifyCode() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())).substring(4, 14);
    }

    private String getDate() {
        String datetime = LocalDateTime.now().toString().replace("T", " ");
        datetime = datetime.substring(0, datetime.lastIndexOf("."));
        parent.ServerTextAppend("O: " + datetime + "\n");
        return datetime;
    }

    private int createNewId(String table, String id_col) throws SQLException {
        ResultSet rs = parent.Select(table, new String[]{}, "max(" + id_col + ")");
        int new_id = 0;
        while (rs.next()) {
            new_id = rs.getInt("max") + 1;
        }
        return new_id;
    }

}
