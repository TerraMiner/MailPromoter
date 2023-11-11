package ua.terra;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class EmailSender {

    private final String userName;

    private Session session;

    private void loginSession(String userName, String password) {
        Properties properties = System.getProperties();

        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });
    }

    public EmailSender(String userName, String password) {
        loginSession(userName, password);
        this.userName = userName;
    }

    public void send(
            String target,
            String theme,
            File file
    ) throws MessagingException, IOException {
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(userName));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(target));
        message.setSubject(theme, "UTF-8");

        Multipart multipart = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        String htmlContent = new String(Files.readAllBytes(file.toPath()));
        htmlPart.setContent(htmlContent, "text/html; charset=utf-8");

        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        message.saveChanges();

        Transport.send(message);
    }
}
