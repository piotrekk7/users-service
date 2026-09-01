# User Service API

REST API for user management with JWT authentication, built with Spring Boot 4.1.0 and Java 25.

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
| Java | 25 | Programming language |
| Spring Boot | 4.1.0 | Application framework |
| Spring Data JPA | 4.1.0 | Data access layer |
| Spring Security | 7.0.0 | Authorization and authentication |
| PostgreSQL | 16 | Database |
| Liquibase | - | Database migrations |
| jjwt | 0.12.6 | JWT token generation and validation |
| SpringDoc OpenAPI | 2.6.0 | API documentation (Swagger UI) |
| Lombok | - | Boilerplate code reduction |
| JaCoCo | 0.8.12 | Code coverage |
| H2 Database | - | In-memory database for tests |
| Maven | 3.9.x | Build tool |
| Docker | - | Containerization |
