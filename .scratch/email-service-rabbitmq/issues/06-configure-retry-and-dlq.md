# 06 — Configure retry and Dead Letter Queue

**What to build:** Gdy wysyłka emaila failuje (np. MailHog wyłączony), email-service automatycznie retryuje 3 razy z wykładniczym backoff (1s, 5s, 15s). Po wyczerpaniu prób wiadomość trafia do Dead Letter Queue widocznej w RabbitMQ management UI. Wszystkie retry attempts i routing do DLQ logowane.

**Blocked by:** 05 — email-service sends welcome email

**Status:** ready-for-agent

- [ ] Dead Letter Exchange (DLX) `user.events.dlx` skonfigurowany w RabbitMQ config
- [ ] Dead Letter Queue `email.user.registered.dlq` zbindowana do DLX
- [ ] Główna kolejka `email.user.registered` ma DLX i TTL skonfigurowane
- [ ] Retry policy z 3 próbami i exponential backoff (1s, 5s, 15s)
- [ ] Test scenario: zatrzymaj MailHog (`docker-compose stop mailhog`), zarejestruj użytkownika
  - [ ] Logi pokazują 3 próby retry z rosnącym delay
  - [ ] Po 3 nieudanych próbach wiadomość w DLQ (widoczna w RabbitMQ management UI)
  - [ ] Event NIE wraca na główną kolejkę
- [ ] Test scenario: uruchom ponownie MailHog, wiadomość z DLQ może być ręcznie przerobiona (shovel/manual)
- [ ] Wszystkie retry attempts logowane z poziomem WARN
- [ ] Routing do DLQ logowane z poziomem ERROR
