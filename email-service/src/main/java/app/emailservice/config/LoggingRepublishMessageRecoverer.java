package app.emailservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

@Slf4j
public class LoggingRepublishMessageRecoverer extends RepublishMessageRecoverer {

    public LoggingRepublishMessageRecoverer(RabbitTemplate rabbitTemplate, String deadLetterExchange, String deadLetterRoutingKey) {
        super(rabbitTemplate, deadLetterExchange, deadLetterRoutingKey);
    }

    @Override
    public void recover(Message message, Throwable cause) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.error("All retry attempts exhausted for message with routing key: {}. Sending to Dead Letter Queue. Error: {}",
                routingKey, cause.getMessage());
        super.recover(message, cause);
        log.error("Message successfully sent to Dead Letter Queue. Routing key: {}", routingKey);
    }
}
