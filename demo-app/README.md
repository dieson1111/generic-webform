# Demo App

A sample Spring Boot application that demonstrates how to integrate the **generic-web-form-library** — a headless, schema-driven form validation and persistence engine. The demo app wires the library with **JPA-backed persistence** using **PostgreSQL**, exposing a full REST API for schema management and form submission.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Option A — Docker Compose (recommended)](#option-a--docker-compose-recommended)
  - [Option B — Local Development](#option-b--local-development)
- [Configuration](#configuration)
- [API Reference](#api-reference)
  - [Schema Management](#schema-management)
  - [Form Submission](#form-submission)
- [Usage Examples](#usage-examples)
- [Database Schema](#database-schema)
- [Seeded Sample Data](#seeded-sample-data)
- [Key Concepts](#key-concepts)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                       demo-app                           │
│                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌────────────┐  │
│  │ FormController│───▶│  FormEngine  │───▶│  JpaForm   │  │
│  │   (REST API) │    │  (library)   │    │ Repository │  │
│  │              │───▶│              │    └──────┬─────┘  │
│  │              │    │ FormSchema   │           │        │
│  │              │───▶│   Manager    │    ┌──────▼─────┐  │
│  │              │    │  (library)   │───▶│ JpaSchema  │  │
│  └──────────────┘    └──────────────┘    │ Repository │  │
│                                          └──────┬─────┘  │
│                                                 │        │
└─────────────────────────────────────────────────┼────────┘
                                                  │
                                           ┌──────▼──────┐
                                           │ PostgreSQL  │
                                           │  (formdb)   │
                                           └─────────────┘
```

- **`FormController`** — REST endpoints created by the demo app (the library is *headless* and does not provide its own controllers).
- **`FormEngine`** & **`FormSchemaManager`** — Auto-configured beans from the `generic-web-form-library`.
- **`JpaFormRepository`** & **`JpaSchemaRepository`** — Demo app's JPA implementations of the library's SPI interfaces (`FormRepository`, `SchemaRepository`), persisting data to PostgreSQL.

---

## Prerequisites

| Requirement       | Version  |
|-------------------|----------|
| Java (JDK)        | 17+      |
| Maven             | 3.x      |
| Docker & Compose  | Latest (for Docker setup) |
| PostgreSQL        | 12+ (for local setup)     |

---

## Project Structure

```
demo-app/
├── Dockerfile                          # Multi-stage Docker build
├── pom.xml                             # Maven module descriptor
└── src/main/
    ├── java/com/ksl/demo/
    │   ├── DemoApplication.java        # Spring Boot entry point
    │   ├── config/
    │   │   └── DataSeeder.java         # Seeds sample schemas & submissions on startup
    │   ├── controller/
    │   │   └── FormController.java     # REST API endpoints
    │   └── persistence/
    │       ├── FormSchemaEntity.java        # JPA entity for form schemas
    │       ├── FormSchemaSpringRepo.java    # Spring Data JPA repository
    │       ├── FormSubmissionEntity.java    # JPA entity for submissions
    │       ├── FormSubmissionSpringRepo.java# Spring Data JPA repository
    │       ├── JpaFormRepository.java       # Implements library's FormRepository SPI
    │       └── JpaSchemaRepository.java     # Implements library's SchemaRepository SPI
    └── resources/
        └── application.yml             # App & datasource configuration
```

---

## Getting Started

### Option A — Docker Compose (recommended)

From the **project root** (`webform/`):

```bash
docker-compose up --build
```

This starts:
- **PostgreSQL 15** on port `5432` (with auto-created `formdb` database and `form_system` schema)
- **demo-app** on port `8080`

> The `DataSeeder` automatically creates sample form schemas on first launch.

### Option B — Local Development

1. **Start PostgreSQL** and create the database / schema:

   ```sql
   CREATE DATABASE formdb;
   \c formdb
   CREATE SCHEMA IF NOT EXISTS form_system;
   ```

2. **Build the entire project** (library + demo-app) from the project root:

   ```bash
   mvn clean install
   ```

3. **Run the demo app**:

   ```bash
   cd demo-app
   mvn spring-boot:run
   ```

   The app will be available at **`http://localhost:8080`**.

---

## Configuration

All configuration lives in `src/main/resources/application.yml`:

| Property                                 | Default Value                                                | Description                          |
|------------------------------------------|--------------------------------------------------------------|--------------------------------------|
| `spring.datasource.url`                  | `jdbc:postgresql://localhost:5432/formdb?currentSchema=form_system` | JDBC connection URL           |
| `spring.datasource.username`             | `postgres`                                                   | Database user                        |
| `spring.datasource.password`             | `password`                                                   | Database password                    |
| `spring.jpa.hibernate.ddl-auto`          | `update`                                                     | Hibernate auto-DDL strategy          |
| `genericform.schema.source`              | `classpath`                                                  | Library schema source mode (`classpath`, `filesystem`, or `repository`) |
| `genericform.schema.classpath-prefix`    | `forms/`                                                     | Classpath prefix for schema files    |

> **Docker override:** When running via Docker Compose, environment variables in `docker-compose.yml` override the datasource URL, username, and password to point to the containerised PostgreSQL.

---

## API Reference

### Schema Management

| Method   | Endpoint              | Description                    |
|----------|-----------------------|--------------------------------|
| `GET`    | `/api/forms`          | List all form schemas          |
| `GET`    | `/api/forms/{formId}` | Get a form schema by ID        |
| `POST`   | `/api/forms`          | Create a new form schema       |
| `PUT`    | `/api/forms/{formId}` | Update an existing form schema |
| `DELETE` | `/api/forms/{formId}` | Delete a form schema           |

### Form Submission

| Method | Endpoint                       | Description                                     |
|--------|--------------------------------|-------------------------------------------------|
| `POST` | `/api/forms/{formId}/submit`   | Validate and submit form data against a schema  |
| `GET`  | `/api/forms/{formId}/submissions` | Retrieve all submitted data for a specific form |

**Response behaviour:**
- `200 OK` — Submission is valid; data has been persisted.
- `400 Bad Request` — Validation failed; response body contains error details.
- `404 Not Found` — The requested `formId` does not exist.

---

## Usage Examples

### List all schemas

```bash
curl http://localhost:8080/api/forms
```

### Get a specific schema

```bash
curl http://localhost:8080/api/forms/contact-us
```

### Create a new schema

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

### Update a schema

```bash
curl -X PUT http://localhost:8080/api/forms/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "form_id": "feedback",
    "form_name": "Feedback Form",
    "form_version": "2.0",
    "components": [
      { "key": "rating", "type": "textfield", "label": "Rating", "input": true,
        "validate": { "required": true } },
      { "key": "comment", "type": "textfield", "label": "Comment", "input": true,
        "validate": { "required": true } }
    ]
  }'
```

### Delete a schema

```bash
curl -X DELETE http://localhost:8080/api/forms/feedback
```

### Submit data (valid)

```bash
curl -X POST http://localhost:8080/api/forms/contact-us/submit \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Smith",
    "email": "jane@example.com",
    "subject": "Inquiry",
    "message": "Hello!"
  }'
```

### Submit data (invalid — missing required fields)

```bash
curl -X POST http://localhost:8080/api/forms/contact-us/submit \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Smith",
    "subject": "Inquiry"
  }'
```

> Will return `400 Bad Request` with validation errors for the missing `email` field.

### Retrieve submissions

```bash
curl http://localhost:8080/api/forms/contact-us/submissions
```

---

## Database Schema

Hibernate auto-generates the following tables in the `form_system` schema:

### `form_schemas`

| Column           | Type    | Description                                   |
|------------------|---------|-----------------------------------------------|
| `form_id`        | VARCHAR | **PK.** Unique schema identifier              |
| `form_name`      | VARCHAR | Human-readable form name                      |
| `version`        | VARCHAR | Schema version label (e.g. `1.0`)             |
| `components_json`| TEXT    | Component tree definitions serialised as JSON |

### `form_submissions`

| Column         | Type      | Description                         |
|----------------|-----------|-------------------------------------|
| `id`           | UUID      | **PK.** Auto-generated UUID         |
| `form_id`      | VARCHAR   | Reference to the schema's `formId`  |
| `data_json`    | TEXT      | Submitted data serialised as JSON   |
| `submitted_at` | TIMESTAMP | Automatically set on insert         |

---

## Seeded Sample Data

On startup, `DataSeeder` creates two sample schemas (if they do not already exist):

### 1. `contact-us`

| Key        | Type       | Required |
|------------|------------|----------|
| `fullName` | textfield  | ✅       |
| `email`    | email      | ✅       |
| `subject`  | textfield  | ✅       |
| `message`  | textfield  | ❌       |

### 2. `employee-onboarding`

Wrapped in a well → fieldset layout (Form.io style):

| Key            | Type       | Required | Constraints                        |
|----------------|------------|----------|------------------------------------||
| `firstName`    | textfield  | ✅       |                                    |
| `lastName`     | textfield  | ✅       |                                    |
| `email`        | email      | ✅       |                                    |
| ↳ `street`     | textfield  | ✅       | Inside `addressSection` fieldset   |
| ↳ `city`       | textfield  | ✅       | Inside `addressSection` fieldset   |
| ↳ `postalCode` | textfield  | ✅       | pattern: `^[A-Za-z0-9\- ]{3,10}$` |

A sample submission against `contact-us` is also persisted automatically.

---

## Key Concepts

| Concept              | Description                                                                                       |
|----------------------|---------------------------------------------------------------------------------------------------|
| **Headless library** | The `generic-web-form-library` provides engines and SPI interfaces but **no controllers**. The host app (this demo) is responsible for defining its own REST endpoints. |
| **SPI pattern**      | The library defines `FormRepository` and `SchemaRepository` interfaces. This demo provides JPA implementations (`JpaFormRepository`, `JpaSchemaRepository`) that are auto-detected by Spring. |
| **Auto-configuration** | Adding the library as a dependency automatically configures `FormEngine` and `FormSchemaManager` beans via Spring Boot auto-configuration. |
| **`@ConditionalOnMissingBean`** | The library registers in-memory default repositories only if the host app does not provide its own. Because this demo provides JPA beans, the defaults are skipped. |
