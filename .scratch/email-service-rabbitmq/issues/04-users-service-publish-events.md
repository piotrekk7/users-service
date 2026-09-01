# 04 — users-service publishes registration events

**What to build:** Po rejestracji nowego użytkownika (POST /api/v1/auth/register), users-service publikuje event `UserRegisteredEvent` do RabbitMQ topic exchange `user.events` z routing key `user.registered`. Event widoczny w RabbitMQ management UI. Rejestracja działa asynchronicznie - użytkownik otrzymuje odpowiedź natychmiast, nie czeka na publikację.

**Blocked by:** 01 — Monorepo restructure, 03 — Add RabbitMQ to infrastructure

**Status:** ready-for-agent

- [ ] Zależność `spring-boot-starter-amqp` w users-service/pom.xml
- [ ] RabbitMQ config class definiujący topic exchange `user.events`
- [ ] DTO `UserRegisteredEvent` z polami: email, username, registeredAt (ISO-8601)
- [ ] Serwis `UserEventPublisher` publikujący eventy do RabbitMQ
- [ ] `AuthService.register()` wywołuje publisher po zapisaniu użytkownika
- [ ] Konfiguracja RabbitMQ w application.yml (host: rabbitmq dla Docker)
- [ ] Po rejestracji nowego użytkownika event pojawia się w RabbitMQ management UI
- [ ] Username składa się z firstName + " " + lastName
- [ ] Event NIE zawiera: password, password hash, user ID, role
- [ ] users-service nadal startuje poprawnie i działa (backward compatibility)
