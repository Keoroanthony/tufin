# Policy Rule Engine

A Spring Boot microservice that manages policy rules and evaluates incoming requests against those rules, returning an `ALLOW` or `DENY` decision based on subject, resource, action, and rule priority.

---

## Prerequisites

| Requirement | Version                   |
|-------------|---------------------------|
| Java        | 21                        |
| Gradle      | 8+ (wrapper included)     |

---

## Building & Running

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew bootRun
```

The application starts on **http://localhost:8080** by default.

### Run Tests

```bash
./gradlew test
```

---

## Architecture

The project follows a clean layered architecture with strict separation of concerns:

```
com.tufin.policyengine/
├── controller/     HTTP layer — routes requests, returns DTOs
├── service/        Business logic orchestration
├── repository/     Data access — thread-safe in-memory store
├── domain/         Core domain model with self-enforcing invariants
├── dto/            Request / response data transfer objects (Java records)
└── exception/      Custom exceptions + global error handler
```

Each layer depends only on the layer directly below it. Domain objects never leak into the HTTP layer; controllers and responses always use DTOs.

---

## REST API

### Implemented

| Method | Endpoint              | Status | Description               |
|--------|-----------------------|--------|---------------------------|
| GET    | `/api/v1/rules`       | ✅     | List all configured rules |
| GET    | `/api/v1/rules/{id}`  | ✅     | Retrieve a rule by ID     |

### Planned

| Method | Endpoint                      | Status   | Description                                  |
|--------|-------------------------------|----------|----------------------------------------------|
| POST   | `/api/v1/rules`               | Planned  | Create a new policy rule                     |
| DELETE | `/api/v1/rules/{id}`          | Planned  | Delete a rule                                |
| POST   | `/api/v1/evaluate`            | Planned  | Evaluate a request against all rules         |
| GET    | `/api/v1/evaluate/history`    | Planned  | Return recent evaluation results (circular buffer) |

---

## Endpoint Details

### GET `/api/v1/rules`

Returns all configured rules ordered by priority descending.

**Response `200 OK`**
```json
[
  {
    "id": "rule-001",
    "name": "Allow Admins",
    "priority": 100,
    "resource": "/admin/*",
    "action": "READ",
    "subject": "ADMIN",
    "decision": "ALLOW",
    "description": "Allows administrators to read admin resources",
    "createdAt": "2026-07-14T10:00:00Z"
  }
]
```

---

### GET `/api/v1/rules/{id}`

Returns a single rule by its identifier.

**Response `200 OK`**
```json
{
  "id": "rule-001",
  "name": "Allow Admins",
  "priority": 100,
  "resource": "/admin/*",
  "action": "READ",
  "subject": "ADMIN",
  "decision": "ALLOW",
  "description": "Allows administrators to read admin resources",
  "createdAt": "2026-07-14T10:00:00Z"
}
```

**Error `404 Not Found`**
```json
{ "message": "Rule not found" }
```

---

### POST `/api/v1/rules` *(Planned)*

Creates a new policy rule.

**Request body**
```json
{
  "name": "Allow Admins",
  "priority": 100,
  "resource": "/admin/*",
  "action": "READ",
  "subject": "ADMIN",
  "decision": "ALLOW",
  "description": "Allows admin access"
}
```

**Response `201 Created`** — full rule object (same shape as GET response).

**Errors**

| Status | Condition                                    |
|--------|----------------------------------------------|
| 400    | `priority` ≤ 0, missing required field, etc. |
| 409    | A rule with the same `name` already exists   |

---

### DELETE `/api/v1/rules/{id}` *(Planned)*

Deletes a rule by ID.

**Response `204 No Content`** — no body.

**Error `404 Not Found`** — `{ "message": "Rule not found" }`

---

### POST `/api/v1/evaluate` *(Planned)*

Evaluates an incoming request against all configured rules.

**Request body**
```json
{
  "subject": "ADMIN",
  "resource": "/admin/users",
  "action": "READ"
}
```

**Response `200 OK`**
```json
{
  "decision": "ALLOW",
  "matchedRuleId": "rule-001",
  "matchedRuleName": "Allow Admins"
}
```

If no rule matches, the default decision is `DENY` (principle of least privilege):
```json
{ "decision": "DENY", "matchedRuleId": null, "matchedRuleName": null }
```

**Error `400 Bad Request`** — missing required field.

#### Evaluation Algorithm

1. Load all rules.
2. Filter rules where `subject`, `action`, and `resource` all match. Resource matching supports `*` as a wildcard (e.g., `/admin/*` matches `/admin/users` and `/admin/profile/edit`).
3. Select the rule with the **highest priority**.
4. On a tie in priority, select the **oldest rule** (by `createdAt`) to ensure deterministic results.
5. If no rule matches, return `DENY`.

---

### GET `/api/v1/evaluate/history` *(Planned)*

Returns the most recent evaluation results from a fixed-size **circular buffer**, ordered from oldest to newest stored entry.

**Response `200 OK`**
```json
[
  {
    "timestamp": "2026-07-14T10:30:12Z",
    "subject": "ADMIN",
    "resource": "/admin/users",
    "action": "READ",
    "decision": "ALLOW",
    "matchedRuleId": "rule-001"
  }
]
```

The buffer has a fixed capacity. When full, the oldest entry is evicted automatically on each new evaluation. Memory usage therefore remains constant regardless of total evaluation count.

---

## Rule Data Model

| Field         | Type     | Required | Description                          |
|---------------|----------|----------|--------------------------------------|
| `id`          | String   | yes      | Unique identifier                    |
| `name`        | String   | yes      | Human-readable name (must be unique) |
| `priority`    | Integer  | yes      | Must be > 0. Higher wins.            |
| `resource`    | String   | yes      | Resource path. Supports `*` wildcard |
| `action`      | String   | yes      | e.g. `READ`, `WRITE`, `DELETE`       |
| `subject`     | String   | yes      | Role or user identifier              |
| `decision`    | String   | yes      | `ALLOW` or `DENY`                    |
| `description` | String   | no       | Optional free-text description       |
| `createdAt`   | DateTime | yes      | Set automatically on creation        |
