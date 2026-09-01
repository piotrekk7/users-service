# Infrastructure Setup and Verification

This document describes the RabbitMQ and supporting infrastructure for the microservices ecosystem.

## Services Overview

The `docker-compose.yml` orchestrates the following services:

### Infrastructure Services
1. **PostgreSQL 16** - Database for users-service
   - Host port: `5433`
   - Container port: `5432`
   - Database: `usersdb`
   - User: `appuser`
   - Password: `apppassword`

2. **RabbitMQ 3.13 (with management)** - Message broker
   - AMQP port: `5672`
   - Management UI: `15672`
   - Credentials: `guest/guest`
   - Web interface: http://localhost:15672

3. **MailHog** - SMTP testing server
   - SMTP port: `1025`
   - Web UI: `8025`
   - Web interface: http://localhost:8025

### Application Services
4. **users-service** - User management API
   - Port: `8080`
   - Build context: `./users-service`
   - Depends on: postgres, rabbitmq

5. **email-service** - Email notification service
   - Port: `8081`
   - Build context: `./email-service`
   - Depends on: rabbitmq, mailhog

## Network

All services are connected via a Docker bridge network named `microservices-network`.

## Data Persistence

Two named volumes ensure data persistence:
- `postgres-data` - PostgreSQL database files
- `rabbitmq-data` - RabbitMQ message store

## Health Checks

### PostgreSQL
- Command: `pg_isready -U appuser -d usersdb`
- Interval: 10s
- Timeout: 5s
- Retries: 5

### RabbitMQ
- Command: `rabbitmq-diagnostics -q ping`
- Interval: 10s
- Timeout: 5s
- Retries: 5

## Quick Start

### Start the entire stack
```bash
docker-compose up --build
```

### Start infrastructure only
```bash
docker-compose up postgres rabbitmq mailhog
```

### Stop all services
```bash
docker-compose down
```

### Stop and remove volumes (clean slate)
```bash
docker-compose down -v
```

## Verification Steps

### 1. Verify all services are running
```bash
docker-compose ps
```

Expected output: All services should be in "Up" state.

### 2. Check service health
```bash
# PostgreSQL
docker-compose exec postgres pg_isready -U appuser -d usersdb

# RabbitMQ
docker-compose exec rabbitmq rabbitmq-diagnostics ping
```

### 3. Access Web Interfaces

**RabbitMQ Management UI:**
- URL: http://localhost:15672
- Username: `guest`
- Password: `guest`
- Verify: You should see the RabbitMQ dashboard with exchanges, queues, and connections

**MailHog Web UI:**
- URL: http://localhost:8025
- Verify: You should see the MailHog inbox interface (initially empty)

**users-service API:**
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

**email-service:**
- Health: http://localhost:8081/actuator/health

### 4. Verify RabbitMQ Topology

Once services are running, check RabbitMQ Management UI for:
- Exchange: `user.events` (topic exchange)
- Queue: `email.user.registered`
- Dead Letter Exchange: `user.events.dlx`
- Dead Letter Queue: `email.user.registered.dlq`

### 5. Test Email Flow

1. Register a new user via users-service:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

2. Check RabbitMQ Management UI:
   - Navigate to "Queues" tab
   - Verify message was published to `email.user.registered` queue
   - Verify message was consumed (queue should be empty after processing)

3. Check MailHog:
   - Open http://localhost:8025
   - Verify welcome email was received
   - Check email content, recipient, and subject

## Troubleshooting

### Services fail to start
```bash
# Check logs for specific service
docker-compose logs users-service
docker-compose logs email-service
docker-compose logs rabbitmq
```

### RabbitMQ connection refused
- Ensure RabbitMQ health check passes before application services start
- Check `docker-compose logs rabbitmq` for errors
- Verify port 5672 is not blocked by firewall

### Database connection errors
- Verify PostgreSQL health check passes
- Check `docker-compose logs postgres`
- Confirm credentials in environment variables match

### Email not sent
- Check email-service logs: `docker-compose logs email-service`
- Verify MailHog is running: `docker-compose ps mailhog`
- Check RabbitMQ queue has messages: http://localhost:15672

### Clean rebuild
```bash
# Stop and remove everything including volumes
docker-compose down -v

# Remove Docker images
docker-compose rm -f

# Rebuild from scratch
docker-compose up --build --force-recreate
```

## Configuration

Environment variables can be customized in `docker-compose.yml` or via a `.env` file.
See `.env.example` for available configuration options.

## Development Tips

### Running services locally (outside Docker)

If you want to run application services locally while using Dockerized infrastructure:

```bash
# Start infrastructure only
docker-compose up postgres rabbitmq mailhog

# In separate terminals, run services with local config
cd users-service
mvn spring-boot:run

cd email-service
mvn spring-boot:run
```

When running locally, update connection strings to use `localhost` instead of Docker service names.
