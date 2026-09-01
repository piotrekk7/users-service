# Spec: Email Service with RabbitMQ Integration

## Problem Statement

The users-service application currently registers users but does not notify them via email upon successful registration. There is no mechanism for asynchronous communication between services, which limits the ability to add additional event-driven functionality in the future. As a learning project, there is also a need to explore message broker technologies (RabbitMQ and Kafka) in a microservices architecture.

## Solution

Create a new microservice called `email-service` that listens to user registration events published by `users-service` via RabbitMQ. When a user registers successfully, `users-service` publishes a `UserRegisteredEvent` to a RabbitMQ topic exchange. The `email-service` consumes this event and sends a welcome email to the newly registered user using a local MailHog SMTP server for testing purposes.

The entire ecosystem (users-service, email-service, PostgreSQL, RabbitMQ, MailHog) will be orchestrated through a single `docker-compose.yml` file in a monorepo structure.

## User Stories

1. As a new user, I want to receive a welcome email after registration, so that I know my account was created successfully
2. As a developer, I want users-service to publish events asynchronously, so that registration response time is not impacted by email delivery
3. As a developer, I want to use RabbitMQ topic exchange, so that multiple services can subscribe to user events in the future
4. As a developer, I want email-service to be decoupled from users-service, so that either service can be developed and deployed independently
5. As a developer, I want to test email delivery locally without sending real emails, so that I can verify the integration works
6. As a developer, I want to see sent emails in a web UI (MailHog), so that I can visually confirm email content and delivery
7. As a system operator, I want failed email deliveries to retry automatically, so that transient failures don't result in lost notifications
8. As a system operator, I want failed messages to go to a Dead Letter Queue after retry limit, so that I can investigate persistent failures
9. As a developer, I want both services in the same git repository, so that I can manage the monorepo easily
10. As a developer, I want a single docker-compose command to start the entire ecosystem, so that setup is simple
11. As a developer, I want email-service to expose health endpoints, so that I can monitor service status
12. As a developer, I want users-service to continue operating if RabbitMQ is temporarily unavailable, so that the system is resilient
13. As a developer, I want the event payload to contain only necessary data, so that we don't leak sensitive information like password hashes
14. As a future developer, I want consistent Java and Spring Boot versions across services, so that the tech stack is uniform
15. As a developer, I want email-service to be stateless without a database, so that it remains simple and focused on message processing
16. As a developer, I want to learn RabbitMQ patterns first, so that I can compare it with Kafka in a future microservice
17. As a system administrator, I want RabbitMQ to persist messages, so that events aren't lost if the broker restarts
18. As a developer, I want clear separation between production-ready patterns (retry, DLQ) and learning simplicity, so that the project is educational but realistic

## Implementation Decisions

### Monorepo Structure
- Restructure current repository root to contain two service directories: `users-service/` and `email-service/`
- Move all existing code from root into `users-service/` subdirectory
- Place shared `docker-compose.yml` at repository root level alongside both service directories
- Each service maintains its own `pom.xml`, `Dockerfile`, and `src/` directory - they are independent Maven projects, not a multi-module build
- Update `.gitignore` to remain at root level and cover both projects

### Technology Stack Alignment
- email-service uses Java 25 (matching users-service upgrade)
- email-service uses Spring Boot 4.1.0 (matching users-service)
- email-service uses minimal dependencies: `spring-boot-starter-web`, `spring-boot-starter-amqp`, `spring-boot-starter-mail`
- No database, JWT, security, or Liquibase dependencies in email-service

### RabbitMQ Topology
- Topic exchange named `user.events`
- Routing key pattern: `user.registered` for registration events
- Queue for email-service: `email.user.registered`
- Dead Letter Exchange (DLX): `user.events.dlx`
- Dead Letter Queue (DLQ): `email.user.registered.dlq`
- Messages have TTL and retry configuration with exponential backoff (3 retries: 1s, 5s, 15s delays)

### Event Contract
- Create `UserRegisteredEvent` DTO in users-service containing:
  - `email` (String, required)
  - `username` (String, derived from firstName + lastName or email prefix)
  - `registeredAt` (ISO-8601 timestamp)
- Do NOT include: password, password hash, user ID, role information, or any other sensitive/unnecessary data
- Serialize as JSON using Jackson

### users-service Modifications
- Add `spring-boot-starter-amqp` dependency to pom.xml
- Create RabbitMQ configuration class defining topic exchange and connection settings
- Create `UserRegisteredEvent` DTO
- Create `UserEventPublisher` service to publish events to RabbitMQ
- Modify `AuthService.register()` to publish `UserRegisteredEvent` after successful user save
- Publishing is fire-and-forget (asynchronous) - registration completes immediately, does not wait for email
- Add RabbitMQ connection properties to application.yml/properties
- Update docker-compose context path from `.` to `./users-service`

### email-service Implementation
- Package structure: `app.emailservice.*`
- Maven groupId: `app.emailservice`, artifactId: `email-service`
- Create `UserRegistrationListener` with `@RabbitListener` annotation consuming from `email.user.registered` queue
- Create `EmailService` that uses Spring's `JavaMailSender` to send emails
- Email template: Simple text-based welcome email with username and registration timestamp
- Configure retry policy with exponential backoff and DLQ routing after 3 failed attempts
- Expose REST endpoint at port 8081 with `/actuator/health` for monitoring
- Optional: simple GET `/api/email/status` endpoint showing service status
- MailHog SMTP configuration: host=mailhog, port=1025 (SMTP), UI on port 8025

### Docker Compose Services
The shared docker-compose.yml orchestrates:
1. **postgres** - PostgreSQL 16 for users-service (port 5433 on host)
2. **rabbitmq** - RabbitMQ with management plugin (ports 5672 for AMQP, 15672 for management UI)
3. **mailhog** - MailHog SMTP server (port 1025 SMTP, 8025 web UI)
4. **users-service** - Spring Boot app (port 8080, build context `./users-service`)
5. **email-service** - Spring Boot app (port 8081, build context `./email-service`)

All services connected via Docker network named `microservices-network`.

Service dependencies:
- users-service depends on postgres (healthcheck) and rabbitmq
- email-service depends on rabbitmq and mailhog

### Configuration Management
- RabbitMQ host for both services: `rabbitmq` (Docker service name)
- MailHog host for email-service: `mailhog` (Docker service name)
- Environment variables for sensitive config passed via docker-compose environment section
- Both services use application.yml with sensible defaults, overridable via environment variables

### Error Handling
- RabbitMQ message acknowledgment: manual ACK after successful email send
- On email send failure: throw exception to trigger retry
- After 3 retries: message automatically routed to DLQ via DLX configuration
- Log all retry attempts and DLQ routing events

## Testing Decisions

### What Makes a Good Test
- Tests should verify external behavior, not implementation details
- Integration tests should use testcontainers for RabbitMQ to match production environment
- Mock external dependencies (JavaMailSender) in unit tests, but use real message broker in integration tests
- Tests should be independent and repeatable

### Testing Scope (For Future Implementation)
Since tests are out of scope for initial implementation, when added later:

**users-service:**
- Integration test for `AuthService.register()` verifying that successful registration publishes `UserRegisteredEvent` to RabbitMQ
- Use testcontainers for RabbitMQ
- Prior art: `AuthControllerIntegrationTest.java` for integration test patterns

**email-service:**
- Unit test for `UserRegistrationListener` mocking JavaMailSender, verifying email parameters
- Integration test publishing event to testcontainers RabbitMQ and verifying email send was attempted
- Prior art: Similar test structure to users-service integration tests

## Out of Scope

The following are explicitly excluded from this spec:

1. **Tests** - No unit or integration tests will be written in the initial implementation
2. **HTML email templates** - Simple text-based emails only
3. **Email template engine** (Thymeleaf, Freemarker) - Plain text with string interpolation
4. **Database for email-service** - No audit log or email history persistence
5. **Authentication/authorization for email-service** - No JWT, no security configuration
6. **Kafka integration** - Will be explored in a future third microservice
7. **Multiple email types** - Only welcome email on registration; no password reset, verification, etc.
8. **Email attachments** - Not supported
9. **Production SMTP provider** (SendGrid, AWS SES, Gmail) - MailHog only
10. **Monitoring/observability** beyond basic health endpoints - No metrics, distributed tracing, or structured logging
11. **CI/CD pipeline** - No GitHub Actions or automated deployment
12. **API documentation (Swagger)** for email-service - users-service keeps its existing Swagger, email-service has minimal API surface
13. **Internationalization** - English-only email content
14. **User email preferences** - No unsubscribe, no opt-out
15. **Rate limiting** on email sending
16. **Message deduplication** - RabbitMQ at-least-once delivery is acceptable

## Further Notes

### Educational Goals
This project serves as a learning exercise for:
- Message-driven microservices architecture
- RabbitMQ topic exchanges and routing patterns
- Retry mechanisms and Dead Letter Queues
- Docker Compose orchestration of multiple services
- Event-driven decoupling between services

### Future Enhancements
After completing this spec, potential next steps include:
1. Adding comprehensive tests using testcontainers
2. Creating a third microservice using Kafka to compare broker technologies
3. Implementing additional event types (password reset, profile updated)
4. Adding email templates with HTML/CSS
5. Implementing outbox pattern for guaranteed event delivery
6. Adding observability stack (Prometheus, Grafana, Jaeger)

### Migration Path
The restructuring of the repository (moving users-service into a subdirectory) should:
- Preserve git history
- Update all documentation references to new paths
- Update README.md at root to describe monorepo structure
- Maintain backward compatibility for existing users-service functionality
