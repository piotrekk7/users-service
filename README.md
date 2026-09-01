# Microservices Monorepo

Monorepo containing microservices communicating via RabbitMQ.

## Structure

```
.
├── users-service/       # User management service with JWT authentication
├── email-service/       # Email notification service (RabbitMQ consumer)
└── docker-compose.yml   # Orchestration of the entire ecosystem
```

## Services

### users-service
REST API for user management with JWT authentication. Publishes events to RabbitMQ.

**Port:** 8080  
**Technologies:** Java 25, Spring Boot 4.1.0, PostgreSQL, RabbitMQ  
**Documentation:** [users-service/README.md](users-service/README.md)

### email-service
Microservice listening to RabbitMQ events and sending emails.

**Port:** 8081  
**Technologies:** Java 25, Spring Boot 4.1.0, RabbitMQ, MailHog  

## Quick Start

```bash
# Start the entire ecosystem
docker-compose up --build

# Users API available at http://localhost:8080
# Email service health: http://localhost:8081/actuator/health
# RabbitMQ Management UI: http://localhost:15672 (guest/guest)
# MailHog Web UI: http://localhost:8025
```

## Infrastructure

- **PostgreSQL** (port 5433) - database for users-service
- **RabbitMQ** (ports 5672, 15672) - message broker
- **MailHog** (ports 1025, 8025) - test SMTP server

## Event-Driven Architecture

```
users-service --[UserRegisteredEvent]--> RabbitMQ --[email.user.registered]--> email-service
                                          (topic: user.events)
```

After user registration (`POST /api/v1/auth/register`), users-service publishes an event to RabbitMQ, and email-service automatically sends a welcome email.

## Development

Each service is an independent Maven project:

```bash
# users-service
cd users-service
mvn spring-boot:run

# email-service
cd email-service
mvn spring-boot:run
```
