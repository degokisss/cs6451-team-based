# Docker Setup Guide

This guide explains how to run the Hotel Reservation System using Docker and Docker Compose for local development.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (version 20.10 or higher)
- [Docker Compose](https://docs.docker.com/compose/install/) (version 2.0 or higher)

Verify installation:
```bash
docker --version
docker-compose --version
```

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd hotel-reservation-system
```

### 2. Set Up Environment Variables

Copy the example environment file:
```bash
cp .env.example .env
```

The `.env` file contains all configuration for database credentials, Redis, JWT secrets, and OAuth settings.

**Important**: The `.env` file is git-ignored to prevent committing sensitive credentials. Review and update values as needed:
- Database credentials: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
- Redis password: `REDIS_PASSWORD`
- JWT secret: `JWT_SECRET`
- Google OAuth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`

For local development, the default values work out of the box. For production, always use strong, unique credentials.

### 3. Start the Application

Start all services (PostgreSQL, Redis, and the application):
```bash
docker-compose up
```

Or run in detached mode (background):
```bash
docker-compose up -d
```

The application will be available at:
- **Application**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### 4. Stop the Application

```bash
docker-compose down
```

To also remove volumes (database data):
```bash
docker-compose down -v
```

## Building the Docker Image

### Build Locally

Build the Docker image:
```bash
docker build -t hotel-reservation-system:latest .
```

### Multi-stage Build Explained

The Dockerfile uses a multi-stage build:

1. **Builder Stage** (gradle:9.1.0-jdk25):
   - Compiles the Java application with Gradle 9.1.0
   - Runs Gradle build
   - Creates the JAR file

2. **Runtime Stage** (eclipse-temurin:25-jre-alpine):
   - Minimal Alpine Linux with JRE 25
   - Copies only the JAR file (not source code)
   - Runs as non-root user for security
   - Includes health check

This approach produces a much smaller final image (~200MB vs ~1GB).

## Docker Compose Services

### Application Service

```yaml
app:
  build: .
  ports:
    - "8080:8080"
  depends_on:
    - postgres
    - redis
```

- Builds from the Dockerfile in the current directory
- Exposes port 8080 for HTTP traffic
- Waits for PostgreSQL and Redis to be healthy before starting

### PostgreSQL Service

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: ${POSTGRES_DB:-hotel_reservation}
    POSTGRES_USER: ${POSTGRES_USER:-hoteluser}
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-hotelpass}
  ports:
    - "5432:5432"
  volumes:
    - postgres-data:/var/lib/postgresql/data
```

- Uses PostgreSQL 16 Alpine image
- Data persists in a Docker volume (`postgres-data`)
- Credentials configured via `.env` file (defaults: `hoteluser` / `hotelpass`)

### Redis Service

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --requirepass ${REDIS_PASSWORD:-redispass}
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
```

- Uses Redis 7 Alpine image
- Data persists in a Docker volume (`redis-data`)
- Password configured via `.env` file (default: `redispass`)

## Common Docker Commands

### View Running Containers

```bash
docker-compose ps
```

### View Logs

View all logs:
```bash
docker-compose logs
```

View logs for specific service:
```bash
docker-compose logs app
docker-compose logs postgres
docker-compose logs redis
```

Follow logs (real-time):
```bash
docker-compose logs -f app
```

### Execute Commands Inside Containers

Access application container:
```bash
docker-compose exec app sh
```

Access PostgreSQL:
```bash
docker-compose exec postgres psql -U hoteluser -d hotel_reservation
```

Access Redis CLI:
```bash
docker-compose exec redis redis-cli -a redispass
```

### Rebuild After Code Changes

Rebuild and restart:
```bash
docker-compose up --build
```

Force rebuild (no cache):
```bash
docker-compose build --no-cache
docker-compose up
```

### Clean Up Everything

Stop containers and remove volumes:
```bash
docker-compose down -v
```

Remove all unused Docker resources:
```bash
docker system prune -a --volumes
```

## Development Workflow

### Hot Reload with Spring Boot DevTools

The application includes Spring Boot DevTools for automatic restarts:

1. Make code changes in your IDE
2. Save the file
3. The application will automatically restart inside the container

Note: This works with the `docker-compose.override.yml` configuration which mounts the source code.

### Running Tests

Run tests inside Docker:
```bash
docker-compose exec app ./gradlew test
```

Run tests with your local Gradle:
```bash
./gradlew test
```

### Accessing the Database

**Using Docker:**
```bash
docker-compose exec postgres psql -U hoteluser -d hotel_reservation
```

**Using local PostgreSQL client:**
```bash
psql -h localhost -p 5432 -U hoteluser -d hotel_reservation
```

Common queries:
```sql
-- List all tables
\dt

-- Describe a table
\d customer

-- Query data
SELECT * FROM customer LIMIT 10;
```

### Accessing Redis

**Using Docker:**
```bash
docker-compose exec redis redis-cli -a redispass
```

**Using local Redis CLI:**
```bash
redis-cli -h localhost -p 6379 -a redispass
```

Common commands:
```bash
# List all keys
KEYS *

# Get a value
GET key_name

# View all active connections
CLIENT LIST
```

## Troubleshooting

### Port Already in Use

If ports 5432, 6379, or 8080 are already in use, you can change them in `docker-compose.yml`:

```yaml
ports:
  - "5433:5432"  # Change host port (left side)
```

### Database Connection Issues

Check if PostgreSQL is ready:
```bash
docker-compose logs postgres
```

Verify health check:
```bash
docker-compose ps
```

Look for "healthy" status in the State column.

### Application Won't Start

Check application logs:
```bash
docker-compose logs app
```

Common issues:
- Database not ready: Wait a few seconds and check logs
- Environment variables missing: Check `.env` file
- Port conflicts: Change port mappings

### Out of Disk Space

Clean up unused Docker resources:
```bash
docker system df  # Check disk usage
docker system prune -a --volumes  # Clean up
```

### Rebuild from Scratch

Complete reset:
```bash
docker-compose down -v
docker system prune -a --volumes
docker-compose up --build
```

## Environment Variables Reference

See `.env.example` for a complete list of configurable environment variables.

### Critical Variables

- `SPRING_DATASOURCE_URL`: PostgreSQL connection string
- `SPRING_DATA_REDIS_HOST`: Redis hostname
- `JWT_SECRET`: Secret key for JWT tokens (change in production!)
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret

### Environment Profiles

- **Development**: `SPRING_PROFILES_ACTIVE=dev`
- **Production**: `SPRING_PROFILES_ACTIVE=prod`

Use different `.env` files for different environments:
```bash
# Load dev environment
docker-compose --env-file .env.dev up

# Load production environment
docker-compose --env-file .env.production up
```

## Production Considerations

### DO NOT use Docker Compose in production!

Docker Compose is designed for development. For production, use:
- **Kubernetes** (recommended for large-scale deployments)
- **Docker Swarm** (simpler alternative to Kubernetes)
- **Managed container services** (AWS ECS, Azure Container Instances, Google Cloud Run)

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Redis Docker Image](https://hub.docker.com/_/redis)

