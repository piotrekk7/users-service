# User Service API

REST API for user management with JWT authentication, built with Spring Boot 3.3.2 and Java 21.

## About

Warm-up project implementing a user management system with JWT authentication.

### Features

- User registration and login with JWT
- User CRUD operations (ADMIN) + `/users/me` endpoint (authenticated)
- Pagination and filtering
- Role system (ADMIN, USER)
- Database migrations (Liquibase)
- Swagger UI
- Unit and integration tests
- Docker Compose

## Technology Stack

| Technology | Version | Description |
|------------|--------|-------------|
| Java | 21 (LTS) | Programming language |
| Spring Boot | 3.3.2 | Application framework |
| Spring Data JPA | 3.3.2 | Data access layer |
| Spring Security | 6.3.1 | Authorization and authentication |
| PostgreSQL | 16 | Database |
| Liquibase | - | Database migrations |
| jjwt | 0.12.6 | JWT token generation and validation |
| SpringDoc OpenAPI | 2.6.0 | API documentation (Swagger UI) |
| Lombok | - | Boilerplate code reduction |
| JaCoCo | 0.8.12 | Code coverage |
| H2 Database | - | In-memory database for tests |
| Maven | 3.9.x | Build tool |
| Docker | - | Containerization |
