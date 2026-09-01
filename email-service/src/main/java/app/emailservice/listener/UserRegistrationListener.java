package app.emailservice.listener;

import app.emailservice.event.UserRegisteredEvent;
import app.emailservice.service.EmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "email.user.registered")
    public void handleUserRegistered(
            UserRegisteredEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Received UserRegisteredEvent for email: {}", event.getEmail());

        try {
            emailService.sendWelcomeEmail(event);
            channel.basicAck(deliveryTag, false);
            log.info("Successfully processed UserRegisteredEvent for: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to process UserRegisteredEvent for: {}", event.getEmail(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to NACK message", ioException);
            }
        }
    }
}
