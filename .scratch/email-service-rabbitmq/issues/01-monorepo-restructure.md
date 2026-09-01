# 01 — Monorepo restructure

**What to build:** Przekształcić obecne repozytorium w monorepo z dwoma serwisami. Cały obecny kod users-service przeniesiony do podfolderu `users-service/`. Struktura katalogów gotowa do dodania drugiego serwisu `email-service/` na tym samym poziomie. Wspólny `.gitignore` w root obsługuje oba projekty.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] Cała zawartość root (src/, pom.xml, Dockerfile, etc.) przeniesiona do `users-service/`
- [ ] `.gitignore` pozostał w root i pokrywa oba projekty
- [ ] `README.md` w root opisuje strukturę monorepo
- [ ] Istniejący `docker-compose.yml` usunięty (zostanie zastąpiony nowym wspólnym)
- [ ] Historia git zachowana
- [ ] Struktura: `users-service/` i miejsce na `email-service/` obok siebie
