package app.userservice.service;

import app.userservice.config.RabbitMQConfig;
import app.userservice.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TOPIC_EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
                    event
            );
            log.info("Published UserRegisteredEvent for email: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent for email: {}", event.getEmail(), e);
        }
    }
}
