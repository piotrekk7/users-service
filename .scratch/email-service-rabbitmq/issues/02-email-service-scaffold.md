# 02 — Email-service scaffold

**What to build:** Nowy projekt Spring Boot `email-service` uruchamia się w kontenerze Docker i odpowiada na health check. Minimalna konfiguracja - bez logiki biznesowej, tylko szkielet gotowy do rozbudowy.

**Blocked by:** 01 — Monorepo restructure

**Status:** ready-for-agent

- [ ] Struktura katalogów `email-service/src/main/java/app/emailservice/`
- [ ] `pom.xml` z Java 25, Spring Boot 4.1.0, zależności: web, amqp, mail, actuator
- [ ] `application.yml` z konfiguracją portu 8081
- [ ] Main class `EmailServiceApplication` z `@SpringBootApplication`
- [ ] `Dockerfile` dla email-service
- [ ] Serwis startuje lokalnie (`mvn spring-boot:run`)
- [ ] Endpoint `GET /actuator/health` zwraca `{"status":"UP"}`
