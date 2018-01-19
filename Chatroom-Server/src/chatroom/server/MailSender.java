/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

/**
 * Klasa odpowiedzialna za wysylanie maili do klientow
 * @author kkapa
 */
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {

    String from, username, password;

    MailSender() {
        
        from = "chatroom.pwsztar@gmail.com";
        username = "chatroom.pwsztar@gmail.com";
        password = "poszlaoladoprzedszkola";
    }

    /**
     * Funkcja ma za zadanie wyslac email na podany adres. Email zawiera kod,
     * potrzebny do weryfikacji konta.
     *
     * @param to email adresata
     * @param code kod weryfikacyjny
     */
    public void Send(String to, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            
            Message message = new MimeMessage(session);

            
            message.setFrom(new InternetAddress(from));

           
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));

            
            message.setSubject("ChatRoom - Kod weryfikacyjny");

            
            message.setText("Dziękuję za korzystanie z aplikacji ChatRoom v1.0\n"
                    + "Twój kod weryfikacyjny: " + code + "\n\n"
                    + "Ekipa ChatRoom");

            
            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
