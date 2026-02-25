# Frontend Integration Guide — Generic Web Form Engine

> **Audience**: Frontend developers integrating with the Generic Web Form Engine backend.

---

## 1. Form Schema Management (CRUD)

The backend supports **runtime creation and management** of form schemas using a Form.io compatible structure. The host app exposes these via REST endpoints (examples below based on `demo-app`).

### Create a Schema

```http
POST /api/forms
Content-Type: application/json
```

```json
{
  "form_id": "job_application",
  "form_name": "Job Application",
  "form_version": "v1",
  "components": [
    { 
      "key": "fullName", 
      "type": "textfield", 
      "label": "Full Name", 
      "input": true, 
      "validate": { "required": true } 
    },
    { 
      "key": "email", 
      "type": "email", 
      "label": "Email Address", 
      "input": true, 
      "validate": { "required": true } 
    },
    {
      "type": "columns",
      "input": false,
      "columns": [
        {
          "components": [
            { "key": "city", "type": "textfield", "label": "City", "input": true }
          ]
        },
        {
          "components": [
            { "key": "postalCode", "type": "textfield", "label": "Postal Code", "input": true }
          ]
        }
      ]
    }
  ]
}
```

**Response** (`201 Created`): returns the saved schema.

### Update a Schema

```http
PUT /api/forms/{form_id}
Content-Type: application/json
```

Body: same structure as Create. The `form_id` in the body must match the URL.

**Response** (`200 OK`): returns the updated schema.

### Get a Schema

```http
GET /api/forms/{form_id}
```

**Response** (`200 OK`): returns the schema JSON. Use this to dynamically render the form.

### List All Schemas

```http
GET /api/forms
```

**Response** (`200 OK`): returns an array of all schemas.

### Delete a Schema

```http
DELETE /api/forms/{form_id}
```

**Response** (`204 No Content`).

### Validation Rules for Schema Creation

The backend validates the schema structure before saving:

| Rule | Error if violated |
|---|---|
| `form_id` must not be blank | `"Schema formId must not be blank"` |
| Must have at least one component | `"Schema must have at least one component"` |
| Component `type` must not be blank | `"Component type must not be blank (key: ...)"` |
| Input components (except `button`) must have a `key` | `"Input component must have a non-blank key (type: ...)"` |

---

## 2. Form Schema Structure

The backend manages form schemas as JSON compatible with Form.io. 

### Schema JSON Format Example

```json
{
  "form_id": "job_application",
  "form_name": "Job Application",
  "form_version": "v1",
  "components": [
    {
      "key": "fullName",
      "type": "textfield",
      "label": "Full Name",
      "input": true,
      "validate": {
        "required": true,
        "minLength": "2",
        "maxLength": "100"
      }
    },
    {
      "key": "age",
      "type": "number",
      "label": "Age",
      "input": true,
      "validate": {
        "minLength": "18",
        "maxLength": "150"
      }
    },
    {
      "type": "panel",
      "label": "Address Details",
      "input": false,
      "components": [
        { "key": "street", "type": "textfield", "label": "Street", "input": true, "validate": { "required": true } }
      ]
    }
  ]
}
```

### Component Properties Reference

| Property       | Type              | Description                                                      |
|----------------|-------------------|------------------------------------------------------------------|
| `key`          | `string`          | **Required for inputs.** The data binding key for submissions.   |
| `type`         | `string`          | **Required.** Component type (e.g., `textfield`, `email`, `panel`, `columns`). |
| `label`        | `string`          | Display label shown to the user.                                 |
| `input`        | `boolean`         | `true` if this component collects data, `false` for layout/static. |
| `validate`     | `object`          | Validation rules (e.g., `required`, `pattern`, `minLength`).     |
| `components`   | `array`           | Child components for layout containers (e.g., `panel`, `well`).  |
| `columns`      | `array`           | Column definitions for `columns`-type layouts.                   |
| `data`         | `object`          | Data source options for `select` / `radio` components.           |

### Component Validation Properties
Contained within the `validate` object:
- `required` (boolean)
- `pattern` (string / regex)
- `minLength` (string number)
- `maxLength` (string number)
- `customMessage` (string)

---

## 3. Form Submission — What the Frontend Sends

**Important Design Principle**: While the schema is a deeply nested tree of layout and input components, the submitted data must be a **flat key-value map** of all the input component `key` values.

### API Endpoint

```http
POST /api/forms/{form_id}/submit
Content-Type: application/json
```

### Request Body Format

```json
{
  "fullName": "Alice Wong",
  "email": "alice@example.com",
  "age": 28,
  "city": "Hong Kong",
  "postalCode": "999077",
  "street": "123 Main St"
}
```

> **Key rule**: The JSON keys in the submission **must match** the `key` values of `input: true` components in the schema exactly (case-sensitive). Layout structure is completely ignored in the submission payload.

---

## 4. Response — What the Frontend Receives

### ✅ Success Response

```json
{
  "valid": true,
  "errors": {}
}
```

### ❌ Validation Error Response

If validations (like `required`, `pattern`, `minLength`, `maxLength`, etc.) fail on the backend, a flat map of errors is returned matching the `key` of the components.

```json
{
  "valid": false,
  "errors": {
    "fullName": "Value must be at least 2 characters",
    "email": "Invalid email format",
    "street": "Field is required",
    "postalCode": "Value does not match pattern: ^[0-9]{5,6}$"
  }
}
```

> **Frontend tip**: Because the response `errors` map keys correspond exactly to the component `key`s in your flat submission payload, you can easily map these back to your form controls to show inline validation messages.

---

## 5. Quick Checklist for Frontend Developers

- [ ] **Schema Management**: Understand that schemas follow a recursive Form.io compatible structure (`components` wrapping other `components` or `columns`).
- [ ] **Rendering**: Implement a recursive rendering function that walks down `components` (and `columns`), ignoring layout nodes for data generation, but using them for UI grouping.
- [ ] **Submission Payload**: Extract all input values into a **flat JSON object** keyed by each component's `key`.
- [ ] **Endpoint Structure**: By default (as seen in `demo-app`), use `/api/forms` for schema CRUD and `/api/forms/{formId}/submit` for submissions.
- [ ] **Error Handling**: On `valid == false`, map the keys in the flat `errors` object directly back to the inputs matching those `key`s.
