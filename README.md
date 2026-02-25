# Generic Web Form Library — Demo

A demonstration of the **generic-web-form-library**, a headless Spring Boot starter for schema-driven form validation and persistence. The demo app provides a REST API and JPA-backed persistence using PostgreSQL.

## Project Structure

- **generic-web-form-library** — Reusable headless library (schema engine, validation, SPI interfaces)
- **demo-app** — Example Spring Boot application that wires the library with Postgres persistence
- **docker/** — Docker Compose setup (Postgres + demo-app)

## Prerequisites

- Java 17+
- Maven 3.x
- Docker & Docker Compose (for containerised setup), **or** a local PostgreSQL 12+

## Quick Start (Docker)

```bash
docker-compose up --build
```

The demo-app will be available at `http://localhost:8080`.

## Quick Start (Local)

1. **Start PostgreSQL** and create the database:
   ```sql
   CREATE DATABASE formdb;
   \c formdb
   CREATE SCHEMA form_system;
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the demo app**:
   ```bash
   cd demo-app
   mvn spring-boot:run
   ```

## API Reference

### Schema Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/forms` | List all form schemas |
| `GET` | `/api/forms/{formId}` | Get a form schema by ID |
| `POST` | `/api/forms` | Create a new form schema |
| `PUT` | `/api/forms/{formId}` | Update an existing schema |
| `DELETE` | `/api/forms/{formId}` | Delete a form schema |

### Form Submission

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/forms/{formId}/submit` | Validate and submit form data |
| `GET`  | `/api/forms/{formId}/submissions` | List all submitted data for a form |

### Example — Submit a Contact Form

```bash
# List seeded schemas
curl http://localhost:8080/api/forms

# Get a specific schema
curl http://localhost:8080/api/forms/contact-us

# Submit valid data
curl -X POST http://localhost:8080/api/forms/contact-us/submit \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Smith",
    "email": "jane@example.com",
    "subject": "Inquiry",
    "message": "Hello!"
  }'

# Submit invalid data (missing required field)
curl -X POST http://localhost:8080/api/forms/contact-us/submit \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Smith",
    "subject": "Inquiry"
  }'
```

### Example — Create a Schema via API

```bash
curl -X POST http://localhost:8080/api/forms \
  -H "Content-Type: application/json" \
  -d '{
    "form_id": "feedback",
    "form_name": "Feedback Form",
    "form_version": "1.0",
    "components": [
      { "key": "rating", "type": "textfield", "label": "Rating", "input": true,
        "validate": { "required": true } },
      { "key": "comment", "type": "textfield", "label": "Comment", "input": true,
        "validate": { "required": false } }
    ]
  }'
```

## Library Integration

To use the library in your own Spring Boot project:

```xml
<dependency>
    <groupId>com.genericform</groupId>
    <artifactId>generic-web-form-library</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The library auto-configures `FormEngine` and `FormSchemaManager` beans. Provide your own `FormRepository` and `SchemaRepository` beans for real persistence (see `demo-app/src/main/java/com/ksl/demo/persistence/` for examples).
