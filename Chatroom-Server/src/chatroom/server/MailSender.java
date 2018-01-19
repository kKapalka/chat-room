/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatroom.server;

/**
 *
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
        // Sender's email ID needs to be mentioned
        from = "chatroom.pwsztar@gmail.com"; //change accordingly
        username = "chatroom.pwsztar@gmail.com";//change accordingly
        password = "poszlaoladoprzedszkola";//change accordingly
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
            // Create a default MimeMessage object.
            Message message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));

            // Set Subject: header field
            message.setSubject("ChatRoom - Kod weryfikacyjny");

            // Now set the actual message
            message.setText("Dziękuję za korzystanie z aplikacji ChatRoom v1.0\n"
                    + "Twój kod weryfikacyjny: " + code + "\n\n"
                    + "Ekipa ChatRoom");

            // Send message
            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
