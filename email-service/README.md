# Email Service

Microservice for handling email sending.

## Description

The service listens to RabbitMQ queue for user registration messages and sends welcome emails.

## Technologies

- Java 25
- Spring Boot 4.1.0
- Spring AMQP (RabbitMQ)
- Spring Mail
- Maven

## Configuration

### Environment Variables

```env
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
```

## Running

### Locally (requires RabbitMQ and SMTP server)

```bash
mvn spring-boot:run
```

### Docker

```bash
docker build -t email-service .
docker run -p 8081:8081 email-service
```

### Docker Compose

```bash
cd ..
docker-compose up email-service
```

## Endpoints

- **Health Check**: `http://localhost:8081/actuator/health`
- **Info**: `http://localhost:8081/actuator/info`

## RabbitMQ

### Queue

- **Name**: `user.registration`
- **Exchange**: `user.events`
- **Routing Key**: `user.registered`

### Message Format

```json
{
  "userId": 123,
  "username": "john.doe",
  "email": "john.doe@example.com"
}
```

## Testing

The project uses MailHog for email testing:

- **MailHog UI**: http://localhost:8025
- **SMTP**: localhost:1025

All sent emails can be viewed in the MailHog interface.
