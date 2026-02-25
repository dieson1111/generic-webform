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

---

## Library Features

### 1. Schema Management (CRUD)

The `FormSchemaManager` interface provides full **runtime** schema lifecycle management:

| Operation | Description |
|-----------|-------------|
| **Create** | Create a new form schema with components |
| **Read**   | Retrieve a schema by `form_id` |
| **List**   | List all available form schemas |
| **Update** | Update an existing schema |
| **Delete** | Delete a schema by `form_id` |

### 2. Schema Loading Strategies

The library supports multiple ways to load schemas into the internal registry:

| Strategy | Description |
|----------|-------------|
| **Classpath** | Load schemas bundled inside the JAR (`ClasspathSchemaProvider`) |
| **Filesystem** | Load schemas from the local filesystem (`FilesystemSchemaProvider`) |
| **Repository** | Load schemas from a database/persistence layer (`RepositorySchemaProvider`) |
| **In-Memory** | Built-in default store (`InMemorySchemaRepository`) — auto-applied when no custom bean is provided |

### 3. Form Submission & Validation

- Validates submitted flat key-value data against a Form.io-compatible schema component tree.
- Returns a structured response:
  - ✅ **Success**: `{ "valid": true, "errors": {} }`
  - ❌ **Failure**: `{ "valid": false, "errors": { "fieldKey": "error message" } }`

### 4. Recursive Schema Traversal

- Supports deeply nested, tree-structured schemas (layout components wrapping input components).
- Submitted data is always a **flat key-value map**; the engine recursively traverses the schema tree to find all input fields.

**Layout types** (traversed, never validated directly):
`well`, `fieldset`, `columns`, `flexbox`, `panel`, `tabs`, `table`, `container`

**Static types** (skipped entirely — no data, no children):
`content`, `htmlelement`, `button`

### 5. Pluggable Persistence (SPI)

| SPI Interface | Purpose |
|---------------|---------|
| `FormRepository` | Persist and retrieve form submissions |
| `SchemaRepository` | Persist and retrieve form schemas |

Both use Spring's `@ConditionalOnMissingBean` — provide your own JPA/DB implementation or fall back to the built-in in-memory defaults.

### 6. Spring Boot Auto-Configuration

Drop in the dependency and the library automatically registers `FormEngine` and `FormSchemaManager` — no boilerplate required.

---

## Validation Rules

### Schema-Level Validation (on Create / Update)

Enforced before a schema is persisted:

| Rule | Error Message |
|------|---------------|
| `form_id` must not be blank | `"Schema formId must not be blank"` |
| Must have at least one component | `"Schema must have at least one component"` |
| Component `type` must not be blank | `"Component type must not be blank (key: ...)"` |
| Input components must have a non-blank `key` | `"Input component must have a non-blank key (type: ...)"` |

### Field-Level Validation (on Form Submission)

Applied per input component at submission time:

| Validation | Applies To | Error Message |
|------------|------------|---------------|
| **Required** | All input types | `"Field is required"` (or custom message) |
| **Min Length** | `textfield`, `textarea`, `password`, `phonenumber` | `"Value must be at least N characters"` |
| **Max Length** | `textfield`, `textarea`, `password`, `phonenumber` | `"Value must be at most N characters"` |
| **Regex Pattern** | `textfield`, `textarea`, `password`, `phonenumber` | `"Value does not match pattern: <regex>"` |
| **Email Format** | `email` | `"Invalid email format"` |
| **Numeric Format** | `number`, `currency` | `"Expected a numeric value"` |
| **Numeric Min** | `number`, `currency` | `"Value must be >= N"` |
| **Numeric Max** | `number`, `currency` | `"Value must be <= N"` |
| **Select / Radio Options** | `select`, `radio` | `"Value is not one of the allowed options"` |
| **Date / Time Presence** | `datetime`, `day`, `time` | `"A valid date/time is required"` |
| **Signature Presence** | `signature` | `"Signature is required"` |
| **Custom Message** | Any field with `customMessage` set | Overrides the default error message |

### Supported Component Types

| Type | Category |
|------|----------|
| `textfield`, `textarea`, `password` | Text input |
| `email` | Email input |
| `url` | URL input |
| `phonenumber` | Phone input |
| `number`, `currency` | Numeric input |
| `select`, `radio` | Choice input |
| `datetime`, `day`, `time` | Date / time input |
| `signature` | Signature input |
| `panel`, `well`, `fieldset`, `columns`, `flexbox`, `tabs`, `table`, `container` | Layout (traversed, not validated) |
| `content`, `htmlelement`, `button` | Static (skipped entirely) |

