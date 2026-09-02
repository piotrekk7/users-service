package app.emailservice.service;

import app.emailservice.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@emailservice.app}")
    private String fromEmail;

    public void sendWelcomeEmail(UserRegisteredEvent event) {
        log.info("Sending welcome email to: {}", event.getEmail());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(event.getEmail());
            message.setSubject("Welcome to our platform!");
            message.setText(buildWelcomeEmailText(event));

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to: {}. Error: {}. Message will be retried.",
                    event.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildWelcomeEmailText(UserRegisteredEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = event.getRegisteredAt().format(formatter);

        return String.format("""
                Welcome %s!

                Thank you for registering with us.
                You registered at %s.

                We're glad to have you on board!

                Best regards,
                The Email Service Team
                """, event.getUsername(), formattedDate);
    }
}
