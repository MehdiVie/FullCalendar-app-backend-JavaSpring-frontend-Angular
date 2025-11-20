package com.example.reminder.service;

import com.example.reminder.model.Event;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    /*
    @Async
    public void sendPlainEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Plain email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send plain email to {}: {}", to, e.getMessage(), e);
        }
    }*/

    @Async // each call executes in separated Thread
    public void sendReminderHtml(String to , String subject , String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true,"UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("HTML reminder email sent to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage(), e);
        }
    }

    public String buildReminderHtml(Event e) {
        String desc = (e.getDescription() == null || e.getDescription().isBlank())
                ? "—"
                : e.getDescription();

        return """
        <div style="font-family: Arial, sans-serif; background:#f4f4f5; padding:20px;">
          <div style="max-width:520px; margin:0 auto; background:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #e5e7eb;">
            <div style="background:#2563eb; color:#fff; padding:16px 20px; font-size:18px; font-weight:600;">
              🔔 Reminder: %s
            </div>
            <div style="padding:20px;">
              <p style="margin:0 0 12px 0; color:#374151;">Hi,</p>
              <p style="margin:0 0 16px 0; color:#374151;">This is a friendly reminder for your event.</p>

              <table style="width:100%%; border-collapse:collapse; margin-top:10px;">
                <tr>
                  <td style="padding:8px 0; color:#6b7280; width:110px;">Title:</td>
                  <td style="padding:8px 0; color:#111827; font-weight:500;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0; color:#6b7280;">Event date:</td>
                  <td style="padding:8px 0; color:#111827;">%s</td>
                </tr>
                <tr>
                  <td style="padding:8px 0; color:#6b7280; vertical-align:top;">Description:</td>
                  <td style="padding:8px 0; color:#111827;">%s</td>
                </tr>
              </table>

              <p style="margin-top:20px; font-size:12px; color:#9ca3af;">
                You received this email because you created a reminder in ReminderApp.
              </p>
            </div>
          </div>
        </div>
        """.formatted(
                e.getTitle(),
                e.getTitle(),
                e.getEventDate(),
                desc
        );
    }

    public String changeEmailHtml() {


        return """
        <div style="font-family: Arial, sans-serif; background:#f4f4f5; padding:20px;">
          <div style="max-width:520px; margin:0 auto; background:#ffffff; border-radius:12px; overflow:hidden; border:1px solid #e5e7eb;">

            <div style="padding:20px;">
              <p style="margin:0 0 12px 0; color:#374151;">Hi,</p>
              <p style="margin:0 0 16px 0; color:#374151;">This is a friendly reminder,
              because you have changed your Email(Username).</p>

              <table style="text-align:center; width:100%%; border-collapse:collapse; margin-top:10px;">
                <tr>
                  <td style="text-align:center; padding:8px 0; color:#6b7280; width:450px;">
                    <a href="http://localhost:4200/login" target="_blank"
                      style="
                        display:inline-block;
                        background:#3b82f6;
                        color:#ffffff;
                        text-decoration:none;
                        padding:12px 20px;
                        border-radius:8px;
                        font-size:14px;
                        font-weight:600;
                        font-family:Arial, sans-serif;
                        text-align:center;
                      ">
                      Login to Reminder App with your new Email
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin-top:20px; font-size:12px; color:#9ca3af;">
                You received this email because you created a reminder in ReminderApp.
              </p>
            </div>
          </div>
        </div>
        """;
    }

    public String buildVerificationEmailHtml(String link, String newEmail) {
        return """
        <h2>Confirm your new email</h2>
        <p>Please click the link below to verify: <b>%s</b></p>
        <a href="%s" style="padding:10px 18px; background:#2563eb; color:white; text-decoration:none; border-radius:6px;">
            Verify Email
        </a>
    """.formatted(newEmail, link);
    }
}
