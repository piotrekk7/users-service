package app.emailservice.listener;

import app.emailservice.event.UserRegisteredEvent;
import app.emailservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "email.user.registered")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for email: {}", event.getEmail());

        emailService.sendWelcomeEmail(event);
        log.info("Successfully processed UserRegisteredEvent for: {}", event.getEmail());
    }
}
