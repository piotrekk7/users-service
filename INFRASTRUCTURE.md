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

4. **Kafka 7.6.0 (KRaft mode)** - Event streaming
   - PLAINTEXT port: `9092`
   - Topic: `audit.events` (3 partitions), `audit.events.dlq`

5. **Kafka UI** - Kafka management console
   - Port: `9090`
   - Web interface: http://localhost:9090

6. **MongoDB 7** - Database for audit-service
   - Port: `27017`
   - Database: `auditdb`

7. **Mongo Express** - MongoDB admin UI
   - Port: `8083`
   - Web interface: http://localhost:8083

8. **PostGIS 16 (geoserver-db)** - Spatial database for GeoServer
   - Host port: `5434`
   - Container port: `5432`
   - Database: `geodata`
   - User: `geouser`
   - Password: `geopassword`

9. **GeoServer 2.25.2** - OGC map server (WMS/WFS)
   - Host port: `8085`
   - Web UI: http://localhost:8085/geoserver/web
   - Credentials: `admin` / `geoserver`
   - Data directory persisted via named volume `geoserver-data`

### Application Services
10. **users-service** - User management API
    - Port: `8080`
    - Build context: `./users-service`
    - Depends on: postgres, rabbitmq

11. **email-service** - Email notification service
    - Port: `8081`
    - Build context: `./email-service`
    - Depends on: rabbitmq, mailhog

12. **audit-service** - Audit log service (Kafka consumer)
    - Port: `8082`
    - Build context: `./audit-service`
    - Depends on: kafka, mongodb

13. **balance-service** - Balance / SSE streaming service
    - Port: `8084`
    - Build context: `./balance-service`

14. **account-app** - Angular frontend
    - Host port: `4200`
    - Build context: `./account-app`
    - Depends on: balance-service

## Network

All services are connected via a Docker bridge network named `microservices-network`.

## Data Persistence

Named volumes:
- `postgres-data` - PostgreSQL database files (usersdb)
- `rabbitmq-data` - RabbitMQ message store
- `kafka-data` - Kafka log segments
- `mongodb-data` - MongoDB audit data
- `geoserver-db-data` - PostGIS geodata database files
- `geoserver-data` - GeoServer configuration and layer definitions

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

### geoserver-db (PostGIS)
- Command: `pg_isready -U geouser -d geodata`
- Interval: 10s
- Timeout: 5s
- Retries: 5

### GeoServer
- Command: HTTP 200 on `http://localhost:8080/geoserver/web/`
- Interval: 30s
- Timeout: 10s
- Retries: 5
- Start period: 60s

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

## Natural Earth Data Import (GeoServer layers)

After starting `geoserver-db` for the first time, run the one-time import script to load
the three Natural Earth 1:110m datasets into the `geodata` PostGIS database:

```bash
# 1. Start the spatial database (if not already running)
docker compose up -d geoserver-db

# 2. Wait until the container is healthy
docker compose ps geoserver-db   # Status column should show "(healthy)"

# 3. Run the import (downloads ~3 MB, takes ~30 s)
./scripts/import-geodata.sh
```

The script:
- Downloads `ne_110m_admin_0_countries`, `ne_110m_rivers_lake_centerlines`, and
  `ne_110m_populated_places_simple` from Natural Earth CDN.
- Installs `gdal-tools` into the container if not already present (needed for `ogr2ogr`).
- Copies each shapefile into the `users-geoserver-db` container.
- Uses `ogr2ogr -f PGDUMP | psql` (inside the container) to create and populate tables
  `countries`, `rivers`, and `cities` in the `public` schema with geometries in EPSG:4326.
- Prints row counts for each table as a quick sanity check.

### Verify the import

```bash
docker exec users-geoserver-db psql -U geouser -d geodata -c "
  SELECT 'countries' AS tbl, count(*) FROM countries
  UNION ALL
  SELECT 'rivers',            count(*) FROM rivers
  UNION ALL
  SELECT 'cities',            count(*) FROM cities;
"
```

Expected output:

```
   layer   | count
-----------+-------
 countries |   177
 rivers    |    13
 cities    |   243
```

> **Note:** The `ERROR: table "X" does not exist` messages on first run are harmless — they come
> from the `DROP_TABLE=ON` option trying to drop tables that don't exist yet. Subsequent runs
> are fully clean (drop + recreate).
>
> Rivers: 1:110m scale includes only the 13 largest rivers worldwide (Amazon, Nile, Congo…).
> This is correct for the coarse resolution chosen for this demo.

### Re-running the import

The `shp2pgsql` default mode (`-c`) drops and recreates each table, so the script is
idempotent — re-running it replaces the data cleanly.

### Connection details (host-side, for psql / pgAdmin / DBeaver)

| Setting  | Value       |
|----------|-------------|
| Host     | `localhost` |
| Port     | `5434`      |
| Database | `geodata`   |
| User     | `geouser`   |
| Password | `geopassword` |

---

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
