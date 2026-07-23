# Vantage: Environment Variables

The following environment variables are required to run the Vantage platform. They are pre-configured for local development in `application.yml` and `docker-compose.yml`, but must be set as GitHub Secrets for the CI/CD pipeline and production deployment.

## Database (PostgreSQL / Neon.tech)
- `SPRING_DATASOURCE_URL`: The JDBC connection string.
- `SPRING_DATASOURCE_USERNAME`: The database username.
- `SPRING_DATASOURCE_PASSWORD`: The database password.

## Messaging (RabbitMQ / CloudAMQP)
- `SPRING_RABBITMQ_HOST`: The RabbitMQ host.
- `SPRING_RABBITMQ_PORT`: The RabbitMQ port (usually 5672).
- `SPRING_RABBITMQ_USERNAME`: The RabbitMQ username.
- `SPRING_RABBITMQ_PASSWORD`: The RabbitMQ password.
- `SPRING_RABBITMQ_VIRTUAL_HOST`: The virtual host (required for CloudAMQP).

## Security
- `JWT_SECRET`: A secure, base64-encoded secret string used for signing JWTs.

## Observability (OpenTelemetry / Grafana Cloud)
- `OTEL_EXPORTER_OTLP_ENDPOINT`: The OTLP endpoint for exporting traces and metrics.
- `OTEL_EXPORTER_OTLP_HEADERS`: Authorization headers for Grafana Cloud (e.g., `Authorization=Basic <base64_token>`).
