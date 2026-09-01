# 03 — Add RabbitMQ to infrastructure

**What to build:** RabbitMQ i MailHog działają w docker-compose. Wspólny `docker-compose.yml` w root uruchamia całą infrastrukturę (postgres, rabbitmq, mailhog) i oba serwisy. RabbitMQ management UI dostępny w przeglądarce.

**Blocked by:** 01 — Monorepo restructure

**Status:** ready-for-agent

- [ ] `docker-compose.yml` w root z serwisami: postgres, rabbitmq, mailhog, users-service, email-service
- [ ] RabbitMQ z management plugin na portach 5672 (AMQP), 15672 (management UI)
- [ ] MailHog na portach 1025 (SMTP), 8025 (web UI)
- [ ] Wspólna sieć Docker `microservices-network`
- [ ] Build context dla users-service: `./users-service`, dla email-service: `./email-service`
- [ ] Health checks dla postgres i rabbitmq
- [ ] `docker-compose up` uruchamia cały stack
- [ ] RabbitMQ management UI dostępny na http://localhost:15672 (guest/guest)
- [ ] MailHog web UI dostępny na http://localhost:8025
