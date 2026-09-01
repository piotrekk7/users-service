# 05 — email-service sends welcome email

**What to build:** End-to-end flow działający: użytkownik rejestruje się w users-service → email-service konsumuje `UserRegisteredEvent` z RabbitMQ → wysyła powitalny email → email pojawia się w MailHog web UI. Prosty tekstowy email z username i datą rejestracji.

**Blocked by:** 02 — Email-service scaffold, 03 — Add RabbitMQ to infrastructure, 04 — users-service publishes registration events

**Status:** ready-for-agent

- [ ] RabbitMQ config class w email-service definiujący binding queue `email.user.registered` → exchange `user.events` (routing key: `user.registered`)
- [ ] `UserRegistrationListener` z `@RabbitListener` konsumujący z kolejki `email.user.registered`
- [ ] `EmailService` używający `JavaMailSender` do wysyłki emaili
- [ ] Konfiguracja JavaMailSender w application.yml (host: mailhog, port: 1025)
- [ ] Prosty tekstowy email template z: "Welcome {username}! You registered at {registeredAt}"
- [ ] Manual acknowledgment po udanej wysyłce
- [ ] Po rejestracji użytkownika przez POST /api/v1/auth/register:
  - [ ] Event konsumowany przez email-service
  - [ ] Email widoczny w MailHog UI (http://localhost:8025)
  - [ ] Email zawiera poprawny username i timestamp
- [ ] Logi w email-service pokazują przetwarzanie eventu
