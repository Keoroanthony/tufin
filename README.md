# Policy Rule Engine

A Spring Boot microservice that evaluates network traffic requests against a configurable set of policy rules, returning an `ALLOW` or `DENY` decision based on source IP, destination IP, port, and rule priority.

---

## Prerequisites

| Requirement | Version  |
|-------------|----------|
| Java        | 17+      |
| Gradle      | 8+ (wrapper included) |

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

## REST API

| Method | Endpoint                    | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| GET    | `/api/v1/policies`          | List all policy rules                            |
| POST   | `/api/v1/policies`          | Create a new policy rule                         |
| GET    | `/api/v1/policies/{id}`     | Retrieve a policy rule by ID                     |
| PUT    | `/api/v1/policies/{id}`     | Update an existing policy rule                   |
| DELETE | `/api/v1/policies/{id}`     | Delete a policy rule                             |
| POST   | `/api/v1/evaluate`          | Evaluate a traffic request against all policies  |

### Example — Evaluate Traffic Request

**Request**
```json
POST /api/v1/evaluate
{
  "sourceIp": "192.168.1.10",
  "destinationIp": "10.0.0.5",
  "port": 443
}
```

**Response**
```json
{
  "decision": "ALLOW",
  "matchedRuleId": "rule-42",
  "reason": "Matched policy rule with priority 1"
}
```

---

## Project Structure

```
src/
└── main/
    ├── java/com/tufin/
    │   ├── TufinApplication.java               # Spring Boot entry point
    │   └── policyengine/
    │       └── model/
    │           ├── Action.java                 # Enum: ALLOW | DENY
    │           ├── PolicyDecision.java         # Evaluation result
    │           ├── PolicyRule.java             # A single firewall-style rule
    │           └── TrafficRequest.java         # Incoming traffic to evaluate
    └── resources/
        └── application.properties
```
