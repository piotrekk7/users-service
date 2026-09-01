# Microservices Monorepo

Monorepo zawierające mikroserwisy komunikujące się przez RabbitMQ.

## Struktura

```
.
├── users-service/       # User management service z JWT authentication
├── email-service/       # Email notification service (RabbitMQ consumer)
└── docker-compose.yml   # Orkiestracja całego ekosystemu
```

## Serwisy

### users-service
REST API dla zarządzania użytkownikami z JWT authentication. Publikuje eventy do RabbitMQ.

**Port:** 8080  
**Technologie:** Java 25, Spring Boot 4.1.0, PostgreSQL, RabbitMQ  
**Dokumentacja:** [users-service/README.md](users-service/README.md)

### email-service
Mikroserwis nasłuchujący eventów z RabbitMQ i wysyłający emaile.

**Port:** 8081  
**Technologie:** Java 25, Spring Boot 4.1.0, RabbitMQ, MailHog  

## Quick Start

```bash
# Uruchom cały ekosystem
docker-compose up --build

# Users API dostępne na http://localhost:8080
# Email service health: http://localhost:8081/actuator/health
# RabbitMQ Management UI: http://localhost:15672 (guest/guest)
# MailHog Web UI: http://localhost:8025
```

## Infrastruktura

- **PostgreSQL** (port 5433) - baza danych dla users-service
- **RabbitMQ** (ports 5672, 15672) - message broker
- **MailHog** (ports 1025, 8025) - testowy SMTP server

## Event-Driven Architecture

```
users-service --[UserRegisteredEvent]--> RabbitMQ --[email.user.registered]--> email-service
                                          (topic: user.events)
```

Po rejestracji użytkownika (`POST /api/v1/auth/register`), users-service publikuje event do RabbitMQ, a email-service automatycznie wysyła powitalny email.

## Development

Każdy serwis to niezależny projekt Maven:

```bash
# users-service
cd users-service
mvn spring-boot:run

# email-service
cd email-service
mvn spring-boot:run
```

## Cel projektu

Projekt edukacyjny do nauki:
- Architektury mikroserwisów
- Message brokerów (RabbitMQ, później Kafka)
- Event-driven communication
- Docker Compose orchestration
