# Policy Rule Engine — Project Walkthrough

*Speaking guide for a 10–12 minute Loom recording. Read naturally; avoid verbatim recitation.*

---

## 1. Project Overview

This service is a **firewall policy evaluation engine** — the kind of component you would find at the core of a network security platform like Tufin SecureChange or SecureTrack. Its job is simple to state: given a traffic request (who is asking, what resource they want, and what they want to do with it), decide whether that traffic should be **ALLOWED** or **DENIED** based on a set of configured firewall rules.

The service also keeps a bounded, in-memory audit trail of every evaluation it performs, and fires an asynchronous notification to a downstream audit service whenever it issues a DENY decision.

The project exists as an interview exercise to demonstrate production-grade backend design using Spring Boot, domain-driven design principles, the Strategy pattern, and clean layered architecture — in roughly the same way Tufin's own platform reasons about network policy.

**High-level architecture:**

```
HTTP Client
    │
    ▼
[ Controller Layer ]  ← thin HTTP adapters only
    │
    ▼
[ Service Layer ]     ← all business logic lives here
    │         │
    ▼         ▼
[ Repository ]   [ History Store ]   [ Audit Client ]
(in-memory)      (circular buffer)   (async WebClient)
```

---

## 2. Project Structure

```
src/main/java/com/tufin/policyengine/
├── controller/       HTTP endpoints — translate HTTP ↔ service calls
├── service/          Business logic interfaces and implementations
│   └── impl/
├── repository/       Data access abstraction and in-memory implementation
├── domain/           Core domain objects (Rule, Decision)
├── dto/              Request/response records (API contract)
├── mapper/           Converts domain objects to DTOs
├── strategy/         Rule-matching algorithms (Strategy pattern)
├── history/          Bounded evaluation history (CircularBuffer)
├── client/           Outbound HTTP client (audit service)
├── config/           Spring beans — WebClient, seed data
└── exception/        Custom exceptions and global handler
```

**How a request flows through the application:**

```
POST /api/v1/evaluate
    │
    ▼  EvaluationController
    │  validates @Valid EvaluationRequest
    │
    ▼  PolicyRuleServiceImpl.evaluateTraffic()
    │  ├── ruleRepository.findAll()
    │  ├── filter via RuleMatchingStrategy
    │  ├── sort by priority (highest wins; oldest wins on tie)
    │  ├── map matched rule → EvaluationResponse  (or default DENY)
    │  ├── historyStore.add(entry)
    │  └── if DENY → auditServiceClient.sendDenyAudit()  [async]
    │
    ▼  ResponseEntity<EvaluationResponse>  →  HTTP 200
```

Each layer has exactly one responsibility. Controllers do not contain logic. Services do not know about HTTP. Repositories do not contain business rules.

---

## 3. Domain Model

### `Rule` (`domain/Rule.java`)

The central domain entity. Immutable and self-validating — its constructor enforces every invariant before the object exists. A `Rule` carries:

| Field | Purpose |
|---|---|
| `id` | UUID assigned at creation |
| `name` | Human-readable label (must be unique) |
| `priority` | Integer ≥ 1; higher wins in evaluation |
| `resource` | Path pattern, e.g. `/admin/*` |
| `action` | Operation string, e.g. `READ`, `WRITE` |
| `subject` | Principal, e.g. `ADMIN`, `GUEST` |
| `decision` | `ALLOW` or `DENY` (the `Decision` enum) |
| `description` | Optional human note |
| `createdAt` | Used as a tiebreaker when priorities are equal |

The domain object deliberately contains **no matching logic**. Matching belongs to the Strategy layer. `Rule` is a pure value carrier.

### `EvaluationRequest` (`dto/EvaluationRequest.java`)

A validated record representing incoming traffic: `{ subject, resource, action }`. Bean Validation annotations (`@NotBlank`) enforce completeness at the controller boundary.

### `EvaluationResponse` (`dto/EvaluationResponse.java`)

The engine's verdict: `{ decision, matchedRuleId, matchedRuleName }`. When no rule matches, `matchedRuleId` and `matchedRuleName` are `null` — signalling a default-deny.

### Interaction

`EvaluationRequest` is handed to the service, which uses it to match against `Rule` objects in the repository. The winning `Rule` (or the absence of one) is converted by `EvaluationMapper` into an `EvaluationResponse`, which is returned to the caller and also recorded in the history store.

---

## 4. Rule Evaluation Engine

This is the most interesting part of the service. The core logic lives in `PolicyRuleServiceImpl.evaluateTraffic()`.

### Algorithm

```
1. Load all rules from the repository
2. Filter: keep only rules where (subject, resource, action) all match
3. Sort:   highest priority first; on tie, oldest createdAt first
4. Pick:   take the first (winning) rule
5. Map:    convert to EvaluationResponse (ALLOW or DENY as declared by rule)
6. Default: if no rule matches → DENY with no matched rule
```

Step 3 uses a named comparator constant `HIGHEST_PRIORITY_OLDEST_FIRST`:

```java
Comparator.comparingInt(Rule::getPriority).reversed()
          .thenComparing(Rule::getCreatedAt)
```

The `reversed()` puts the highest integer first. The `thenComparing(createdAt)` resolves ties in ascending time order — oldest rule wins. This is the classical "first-defined wins" tie-breaking strategy used in firewall policy engines.

### Wildcard Matching (`WildcardResourceMatchingStrategy`)

Resource patterns support `*` as a wildcard. The strategy converts the pattern to a regex at match time:

```
/admin/*      → ^/admin/.*$
/api/*/data   → ^/api/.*/data$
/*            → ^/.*$
```

Exact patterns (no `*`) skip regex compilation and use direct string equality — a minor optimisation.

**Why this approach?** It is simple, readable, covers the common case (suffix wildcard), and requires no external dependency. The trade-off is that `*` always means "zero or more characters" — it cannot distinguish `/admin/users` from `/admin/users/roles` as separate patterns. For production you would want CIDR-style matching or AntPathMatcher.

### First-Match, Not Best-Match

The engine selects **the single highest-priority matching rule** and returns immediately. This is first-match semantics after sorting by priority — not an accumulation of all matching rules. This is how real firewall ACLs work (Cisco IOS, iptables).

### Default Deny

If no rule matches, the engine returns `DENY` with null rule fields. Security systems are fail-closed: if you did not explicitly allow something, it is denied. This avoids accidental access through rule gaps.

---

## 5. REST API Walkthrough

### Rule Management (`/api/v1/rules`)

| Method | Path | Status | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/rules` | 200 | List all rules, sorted by priority descending |
| `GET` | `/api/v1/rules/{id}` | 200 / 404 | Fetch a single rule by ID |
| `POST` | `/api/v1/rules` | 201 / 400 / 409 | Create a new rule |
| `DELETE` | `/api/v1/rules/{id}` | 204 / 404 | Delete a rule by ID |

**POST `/api/v1/rules` request body:**

```json
{
  "name": "Allow Admins",
  "priority": 100,
  "resource": "/admin/*",
  "action": "READ",
  "subject": "ADMIN",
  "decision": "ALLOW",
  "description": "Optional human note"
}
```

The `decision` field accepts `ALLOW` or `DENY` (case-insensitive, enforced by a `@Pattern` constraint). Duplicate names return HTTP 409 Conflict. Missing required fields return 400 with a field-level message.

**DELETE `/api/v1/rules/{id}`:** returns 204 on success, 404 with structured error body if the ID does not exist.

### Traffic Evaluation (`/api/v1/evaluate`)

| Method | Path | Status | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/evaluate` | 200 | Evaluate a traffic request against all rules |
| `GET` | `/api/v1/evaluate/history` | 200 | Return the 20 most recent evaluations, newest first |

**POST `/api/v1/evaluate` request body:**

```json
{
  "subject": "ADMIN",
  "resource": "/admin/users",
  "action": "READ"
}
```

**Response (rule matched):**

```json
{
  "decision": "ALLOW",
  "matchedRuleId": "rule-001",
  "matchedRuleName": "Allow Admins"
}
```

**Response (default deny):**

```json
{
  "decision": "DENY",
  "matchedRuleId": null,
  "matchedRuleName": null
}
```

### Typical request flow

1. Create three rules (allow admin, deny guest, allow user profile)
2. Evaluate ADMIN accessing `/admin/users` → ALLOW by rule
3. Evaluate GUEST accessing `/admin/users` → DENY by rule
4. Evaluate UNKNOWN accessing `/admin/users` → DENY by default (no rule)
5. Call `GET /evaluate/history` → see all three decisions, newest first

---

## 6. Exception Handling

All exceptions are handled in a single class: `GlobalExceptionHandler` (`@RestControllerAdvice`).

| Exception | HTTP Status | Trigger |
|---|---|---|
| `RuleNotFoundException` | 404 Not Found | `getRuleById` or `deleteRule` with unknown ID |
| `DuplicateRuleNameException` | 409 Conflict | `createRule` with a name already in use |
| `MethodArgumentNotValidException` | 400 Bad Request | Bean Validation failure on a request body |
| `HttpMessageNotReadableException` | 400 Bad Request | Malformed JSON body |
| `IllegalArgumentException` | 400 Bad Request | Domain object construction invariant violated |
| `HttpRequestMethodNotSupportedException` | 405 Method Not Allowed | Wrong HTTP verb |
| `Exception` (catch-all) | 500 Internal Server Error | Anything unexpected |

All responses use the same `ErrorResponse` record shape:

```json
{
  "timestamp": "2026-07-17T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Rule not found: ghost-id",
  "path": "/api/v1/rules/ghost-id"
}
```

**Why centralise?** Scattering try/catch in controllers creates inconsistent error shapes, duplicated formatting logic, and untestable code. One handler, one shape, one test class.

---

## 7. Validation

Two validation boundaries exist:

**Controller boundary** — `@Valid` on every `@RequestBody` triggers Bean Validation before the service is called. `CreateRuleRequest` and `EvaluationRequest` both use `@NotBlank`, `@NotNull`, `@Min`, and `@Pattern` constraints. Invalid requests never reach the service.

**Domain boundary** — The `Rule` constructor validates its own invariants (null/blank fields, priority > 0, resource must start with `/`). This is domain-driven defensive programming: the object cannot exist in an invalid state regardless of how it is created.

These two boundaries are complementary. The controller catches user-input errors; the domain catches programming errors.

---

## 8. Design Decisions

### Business logic in the service layer

`PolicyRuleServiceImpl` and `RuleService` contain all business logic. Controllers are pure HTTP adapters — they translate requests, delegate, and wrap responses. This keeps controllers testable without Spring context and services testable without HTTP.

### Thin controllers

`RuleController` and `EvaluationController` each contain fewer than 15 lines of substantive code. No `if` statements, no data transformation, no exception catches. This is intentional: the controller is not a place for decisions.

### Strategy pattern for matching

`RuleMatchingStrategy` and `ResourceMatchingStrategy` are interfaces. `DefaultRuleMatchingStrategy` delegates resource matching to `WildcardResourceMatchingStrategy`. New matching strategies (CIDR, regex, glob) can be introduced without touching the service. The service depends on the interface, not the implementation.

### In-memory storage with `ConcurrentHashMap`

`InMemoryRuleRepository` uses a `ConcurrentHashMap` for thread-safe concurrent access without explicit locking. This is appropriate for a demonstration service. The `RuleRepository` interface means swapping to JPA or Redis requires only a new implementation class, not changes in any caller.

### Bounded evaluation history with a circular buffer

`CircularBuffer<T>` is a fixed-capacity ring buffer. When the buffer is full, the oldest entry is silently overwritten. The capacity is configurable (`evaluation.history.capacity`, defaulting to 20). This gives O(1) write and O(n) read with a predictable memory footprint. The store returns history in **reverse chronological order** (newest first) because that is how operators read audit trails.

### Asynchronous audit via WebFlux

`WebClientAuditServiceClient` fires a non-blocking HTTP POST to the audit service using Project Reactor's `subscribe()`. This means evaluation response time is never held hostage by the availability of the audit endpoint. Audit failures are logged as warnings, not propagated as exceptions.

---

## 9. Possible Improvements

If this moved to production:

- **Persistent storage** — Replace `InMemoryRuleRepository` with a JPA/PostgreSQL implementation behind the same interface. Rules would survive restarts.
- **Authentication and authorisation** — Only authorised principals should be able to create or delete rules. Spring Security with JWT would be the natural fit.
- **Audit persistence** — The evaluation history is ephemeral. In production, every DENY event should be durably recorded (Kafka topic or Postgres table).
- **Rule indexing** — The current O(n) linear scan across all rules is fine for tens of rules. At scale, rules should be indexed by subject and action to reduce the candidate set before wildcard matching.
- **Rule priorities and conflicts** — A proper UI/API for detecting and resolving conflicting rules (same subject, resource, action, different decision at same priority) would be essential.
- **Pagination** — `GET /api/v1/rules` returns all rules. With many rules, pagination (`page`, `size` parameters) is required.
- **Metrics** — Micrometer/Prometheus counters on evaluation outcomes (ALLOW rate, DENY rate, default-deny rate) give operational visibility.
- **Structured logging** — Replace ad-hoc log statements with structured MDC fields (requestId, subject, resource) so every log line is queryable in a log aggregator.
- **Concurrency improvements** — `CircularBuffer` uses `synchronized` (coarse-grained). A lock-striped or lock-free ring buffer would be appropriate under sustained high concurrency.
- **HTTPS / mTLS** — The audit client communicates over plain HTTP. In production, mutual TLS between services is mandatory.

---

## 10. Live Demonstration Script

*Cold terminal. Spring Boot not yet running.*

### Start the application

```bash
# Terminal 1
JAVA_HOME=~/.sdkman/candidates/java/21.0.2-tem \
  PATH=$JAVA_HOME/bin:$PATH \
  ./mvnw spring-boot:run
```

Wait for: `Started TufinApplication`. The seed data initialiser pre-loads three rules on startup.

---

### Step 1 — Create three rules

**Rule 1: exact allow rule**

```bash
curl -s -X POST http://localhost:8080/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Allow Admins Exact",
    "priority": 200,
    "resource": "/admin/dashboard",
    "action": "READ",
    "subject": "ADMIN",
    "decision": "ALLOW",
    "description": "Exact path allow for admin dashboard"
  }' | jq .
```

> "This rule matches only the exact path `/admin/dashboard`. No wildcard — full literal equality."

**Rule 2: wildcard allow rule**

```bash
curl -s -X POST http://localhost:8080/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Allow Admins Wildcard",
    "priority": 150,
    "resource": "/admin/*",
    "action": "READ",
    "subject": "ADMIN",
    "decision": "ALLOW",
    "description": "Wildcard allow for all admin sub-paths"
  }' | jq .
```

> "The `*` here covers any suffix — `/admin/users`, `/admin/settings`, `/admin/users/roles` all match."

**Rule 3: explicit deny rule**

```bash
curl -s -X POST http://localhost:8080/api/v1/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Deny Guests Admin",
    "priority": 100,
    "resource": "/admin/*",
    "action": "READ",
    "subject": "GUEST",
    "decision": "DENY",
    "description": "Guests are never allowed admin access"
  }' | jq .
```

> "Lower priority than the admin rules but applies only to GUEST — this rule explicitly blocks rather than defaulting."

---

### Step 2 — List all rules

```bash
curl -s http://localhost:8080/api/v1/rules | jq .
```

> "Rules come back sorted by priority, highest first. Note the seed rules from startup also appear."

---

### Step 3 — Evaluate traffic

**ALLOW by rule:**

```bash
curl -s -X POST http://localhost:8080/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{"subject":"ADMIN","resource":"/admin/users","action":"READ"}' | jq .
```

> "ADMIN reading `/admin/users` hits the wildcard allow rule. Decision: ALLOW. Note `matchedRuleId` and `matchedRuleName` are populated."

**DENY by explicit rule:**

```bash
curl -s -X POST http://localhost:8080/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{"subject":"GUEST","resource":"/admin/users","action":"READ"}' | jq .
```

> "GUEST hits the explicit DENY rule. The decision carries the rule that denied them — useful for audit."

**DENY by default:**

```bash
curl -s -X POST http://localhost:8080/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{"subject":"UNKNOWN","resource":"/admin/users","action":"READ"}' | jq .
```

> "No rule matches UNKNOWN. `matchedRuleId` is null — this is the default deny. The system is fail-closed."

---

### Step 4 — Delete a rule

**Delete existing rule (note the ID from creation):**

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rules/{id} -w "\nHTTP %{http_code}\n"
```

> "204 No Content — clean success, no body."

**Delete a non-existent rule:**

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rules/ghost-id | jq .
```

> "404 with a structured error body — `Rule not found: ghost-id`. The global exception handler formats this consistently."

---

### Step 5 — Evaluation history

```bash
curl -s http://localhost:8080/api/v1/evaluate/history | jq .
```

> "The three evaluations we just ran appear here, newest first. The history is capped at 20 entries — the oldest are silently dropped when the buffer fills. No persistence."

---

### Step 6 — Walk through IntelliJ

1. Open `PolicyRuleServiceImpl` — show the `HIGHEST_PRIORITY_OLDEST_FIRST` comparator constant and the `evaluateTraffic` method body.
2. Open `WildcardResourceMatchingStrategy` — show the `toRegex()` conversion.
3. Open `CircularBuffer` — show the ring buffer mechanics, the `synchronized` blocks, and `getAll()`.
4. Open `GlobalExceptionHandler` — show every handler and the consistent `ErrorResponse.of(...)` factory.
5. Open `PolicyRuleServiceImplTest` — show the priority tie-breaking test and the audit trigger tests.

---

### Step 7 — Architectural trade-offs

> "The biggest trade-off in this service is that all state lives in memory. Rules disappear on restart, and history is capped at 20. That is entirely intentional for this exercise — persistence would require JPA and a database, which adds significant complexity without adding conceptual interest.
>
> The second trade-off is the wildcard matching strategy. The `*` glob is simple and covers most real-world cases, but it cannot express CIDR ranges, regular expressions, or multi-segment exact matches. A production system would use AntPathMatcher, or for network policy specifically, CIDR prefix matching.
>
> Finally, the audit client is fire-and-forget. There is no retry, no dead-letter queue, and no guarantee the audit event arrives. In production you would route DENY events through a Kafka topic for reliable, ordered audit delivery."

---

## 11. Reflection Notes

### Which two design decisions were hardest to justify?

**1. Keeping `Rule` as a plain Java object rather than a JPA entity.**
Making `Rule` a database entity early would have been tempting. The push-back is that JPA annotations couple the domain model to the persistence mechanism — exactly what the repository pattern is designed to prevent. Keeping `Rule` as a pure Java object means the domain is honest about what it is: an in-memory value, interchangeable for a JDBC-backed record behind the same interface. This required discipline to defend.

**2. Splitting `RuleService` from `PolicyRuleServiceImpl`.**
One question that came up during review: why are CRUD and evaluation in separate service classes? The answer is that rule management (CRUD) and rule evaluation are genuinely different responsibilities with different collaborators, different failure modes, and different test setups. Merging them would make both harder to test and read. But the split does mean a reader has to look in two classes to understand the full behaviour of the system — that is the real cost.

### What would change in a second iteration?

- Replace `CircularBuffer` with a persistent append-only event log (PostgreSQL or Kafka) so no history is lost.
- Add `RuleVersion` tracking — who created or modified a rule, when, and from what previous state. This is mandatory in a security context.
- Introduce rule conflict detection: flag when two rules at the same priority would produce contradicting decisions for the same traffic.
- Replace the in-process `EvaluationHistoryStore` with a read-model updated by application events, separating write (evaluation) from read (history) concerns.

### What did this project teach about designing maintainable backend services?

The most durable lesson is that **good naming is not cosmetic — it is the interface between today's author and tomorrow's reader**. Naming the comparator `HIGHEST_PRIORITY_OLDEST_FIRST` instead of leaving it as an inline chain, naming the predicate `requiresAudit`, naming the wildcard strategy separately from the full rule-matching strategy — each of these halved the cognitive load of the method that used them.

The second lesson is that **interfaces are not about abstraction for its own sake — they are about controlling the direction of dependency**. `RuleRepository`, `RuleMatchingStrategy`, and `AuditServiceClient` are all interfaces that point inward toward the domain. The implementations (ConcurrentHashMap, regex matching, WebClient) are outside the domain. Swapping any one of them is a matter of writing a new class, not changing existing ones. That is what makes the service genuinely extensible without being over-engineered.
