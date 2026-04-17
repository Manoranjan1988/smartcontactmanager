package com.smartcontact.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.smartcontact.exception.EmailSendException;
import com.smartcontact.exception.OtpSendException;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String senderEmail;

    

    public boolean sendVerificationEmail(String toEmail, String token, String baseUrl) {
        try {
            System.out.println("Email service Started.....");
            String subject = "Verify your account - Smart Contact Manager";
            String verificationUrl = baseUrl + "/public/verify?token=" + token;
            log.info("The URL is: {}",verificationUrl);

            String message = "Dear User,\n\n"
                    + "Thank you for registering. Please click the link below to activate your account:\n"
                    + verificationUrl + "\n\n"
                    + "Please use the latest verification email. Older links will not work.\n\n"
                    + "Regards,\nSmart Contact Manager Team";

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(senderEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email Sent Successfully to {} ", toEmail);
            return true;

        } catch (MailSendException e) {
            log.error("ERROR: Invalid Email or SMTP issue: ", e.getMessage());
            return false;
        }

        catch (Exception e) {
            throw new EmailSendException("Mail Server is currently down. Please try again later!");

        }
    }

    public void otpEmailVerification(String toEmail, String otp) {

        try {
            System.out.println("Sending OTP to: " + toEmail);
            String subject = "OTP for Password Reset - SCM";
            String message = "Dear User,\n\n"
                    + "TYour 6-digit OTP for password reset is: " + otp + "\n\n"
                    + "This OTP is valid for 5 minutes. Do not share it with anyone.\n\n"
                    + "Regards,\nSmart Contact Manager Team";

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailMessage.setFrom(senderEmail);

            mailSender.send(mailMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new OtpSendException("Mail Server is currently down. Send OTP Later");
        }

    }

    @Async
    public void sendEmailWithAttachment(String to, String subject, String message, byte[] fileData, String fileName) {

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message, true);
            helper.setFrom(senderEmail);

            if (fileData != null && fileData.length > 0) {
                helper.addAttachment(fileName, new ByteArrayResource(fileData));
            }
            mailSender.send(mimeMessage);
            Thread.sleep(2000);
        } catch (Exception e) {

            e.printStackTrace();
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void sendEmail(String name, String email, String subject, String message) {

        try {
            String emailBody = "New Inquiry from SCM Website:\n\n" +
                    "Name: " + name + "\n" +
                    "Email: " + email + "\n" +
                    "Subject: " + subject + "\n" +
                    "Message: " + message;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(senderEmail);
            msg.setFrom(senderEmail);
            msg.setReplyTo(email);
            msg.setSubject("SCM Contact: " + subject);
            msg.setText(emailBody);

            mailSender.send(msg);
            log.info("DEBUG: Brevo Cannon Fired! Email Sent to {}", senderEmail);
        } catch (Exception e) {
            e.printStackTrace();
            throw new EmailSendException("Mail Server is currently down");
        }

    }

    @Async
    public void sendPaymentSuccessEmail(String to, String name, String orderId, String amount) {

        try {
            String body = "<div style='max-width:600px; margin:auto; padding:20px; font-family:sans-serif;'>" +

                    "<div style='text-align:center; margin-bottom:20px;'>" +
                    "<img src='https://res.cloudinary.com/dqr8hqui5/image/upload/v1774898074/logo_nlkbez.png' width='100'/>" +
                    "</div>" +

                    "<h2>Hello " + name + ",</h2>" +
                    "<p>Thank you for your payment 🙏</p>" +

                    "<hr/>" +

                    "<p><b>Order ID:</b> " + orderId + "</p>" +
                    "<p><b>Amount Paid:</b> <span style='color:#16a34a;'>₹" + amount + "</span></p>" +

                    "<p style='color:green; font-weight:bold;'>Payment Successful ✅</p>" +

                    "<hr/>" +

                    "<p style='font-size:14px; color:gray;'>If you did not make this payment, contact support immediately.</p>"
                    +

                    "<br><p>Best Regards,<br><b>Team SCM</b></p>" +

                    "<br><small>This is an automated message. Please do not reply.</small>" +

                    "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setFrom(senderEmail);
            helper.setSubject("✅ Payment Confirmed – Smart Contact Manager");
            helper.setText(body, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new EmailSendException("Mail Server is currently down");
        }
    }
}