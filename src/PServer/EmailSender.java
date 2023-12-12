package PServer;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    public static void sendEmail(String toEmail, String otp) {
        final String username = "hocvieniqltm";
        final String password = "tjtwhrjbxwiykczg";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Xác nhận đăng ký");
            message.setText("Mã OTP của bạn là: " + otp);

            Transport.send(message);


        } catch (MessagingException e) {
            // Thêm xử lý ngoại lệ hoặc logging tại đây
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}