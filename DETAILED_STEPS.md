
# DETAILED_STEPS.md — Enterprise Spring Boot Project: Interview Preparation Guide

> A comprehensive deep-dive into every feature implemented in **SimpleEnterprizeProj2**.
> Covers three perspectives: **Developer** (how), **Tech Lead** (decisions), and **Architect** (why & trade-offs).

---

## Table of Contents

1. [Tech Stack Overview](#1-tech-stack-overview)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Feature 1 — Liquibase Database Migrations](#3-feature-1--liquibase-database-migrations)
4. [Feature 2 — Domain Models (JPA Entities)](#4-feature-2--domain-models-jpa-entities)
5. [Feature 3 — Spring Data JPA Repositories](#5-feature-3--spring-data-jpa-repositories)
6. [Feature 4 — DTO Layer (Request / PatchRequest / Response)](#6-feature-4--dto-layer-request--patchrequest--response)
7. [Feature 5 — Input Sanitization (XSS Protection)](#7-feature-5--input-sanitization-xss-protection)
8. [Feature 6 — Mapper Layer (Entity-DTO Conversion)](#8-feature-6--mapper-layer-entity-dto-conversion)
9. [Feature 7 — JPA Specifications (Dynamic Filtering)](#9-feature-7--jpa-specifications-dynamic-filtering)
10. [Feature 8 — Service Layer with Transaction Management](#10-feature-8--service-layer-with-transaction-management)
11. [Feature 9 — REST Controllers with HATEOAS](#11-feature-9--rest-controllers-with-hateoas)
12. [Feature 10 — Global Exception Handling](#12-feature-10--global-exception-handling)
13. [Feature 11 — OpenAPI / Swagger Documentation](#13-feature-11--openapi--swagger-documentation)
14. [Feature 12 — Redis Caching (Application-Level)](#14-feature-12--redis-caching-application-level)
15. [Feature 13 — Hibernate L2 Cache with EhCache](#15-feature-13--hibernate-l2-cache-with-ehcache)
16. [Feature 14 — Resilience4j Circuit Breakers](#16-feature-14--resilience4j-circuit-breakers)
17. [Feature 15 — Observability (Logging, Correlation IDs, Security Headers)](#17-feature-15--observability-logging-correlation-ids-security-headers)
18. [Feature 16 — Resilience4j Retry & Fallback](#18-feature-16--resilience4j-retry--fallback)
19. [Feature 17 — Resilience4j Bulkhead (Concurrency Limiter)](#19-feature-17--resilience4j-bulkhead-concurrency-limiter)
20. [Feature 18 — POST Idempotency](#20-feature-18--post-idempotency)
21. [Feature 19 — Graceful Shutdown](#21-feature-19--graceful-shutdown)
22. [Feature 20 — Async Processing](#22-feature-20--async-processing)
23. [Feature 21 — Bulk Operations](#23-feature-21--bulk-operations)
24. [Feature 22 — Webhook Support](#24-feature-22--webhook-support)
25. [Feature 23 — Multi-language Support (i18n)](#25-feature-23--multi-language-support-i18n)
26. [Feature 24 — Timezone Handling (UTC Standardization)](#26-feature-24--timezone-handling-utc-standardization)
27. [Cross-Cutting Concerns Summary](#27-cross-cutting-concerns-summary)
28. [Full API Endpoint Reference](#28-full-api-endpoint-reference)

---

## 1. Tech Stack Overview

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 25 |
| Framework | Spring Boot | 4.0.2 |
| ORM | Hibernate (via Spring Data JPA) | 7.x (managed) |
| Database | H2 (in-memory for dev, file for prod) | managed |
| Schema Migration | Liquibase | managed |
| Connection Pool | HikariCP | managed |
| Application Cache | Redis (Spring Data Redis) | managed |
| ORM Cache | EhCache 3 (via JCache/JSR-107) | managed |
| Resilience | Resilience4j | 2.3.0 |
| API Documentation | springdoc-openapi | 3.0.1 |
| Hypermedia | Spring HATEOAS | managed |
| Validation | Jakarta Bean Validation (Hibernate Validator) | managed |
| Build | Maven + spring-boot-maven-plugin | — |

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT (HTTP)                               │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   SecurityHeadersFilter  │  X-Content-Type-Options, CSP, etc.
                    │   RequestLoggingFilter    │  UUID correlation ID, duration logging
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   @RestController Layer   │  UserController, EmployeeController,
                    │   (Validation + HATEOAS)  │  DepartmentController
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │    @Service Layer         │  @Transactional, @CircuitBreaker,
                    │    (Business Logic)       │  @Bulkhead, @Retry, @Cacheable
                    └─────┬─────────────┬─────┘
                          │             │
              ┌───────────▼──┐   ┌──────▼──────────┐
              │ Mapper Layer │   │ Specification    │
              │ (DTO ↔ Entity│   │ Layer (Criteria  │
              │ + Sanitize)  │   │ API filters)     │
              └───────────┬──┘   └──────┬──────────┘
                          │             │
                    ┌─────▼─────────────▼─────┐
                    │   @Repository Layer       │  JpaRepository + JpaSpecificationExecutor
                    │   (Query Cache hints)     │  @QueryHints(HINT_CACHEABLE)
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                   │
     ┌────────▼────────┐ ┌──────▼──────┐  ┌────────▼────────┐
     │  Hibernate L2   │ │  H2 Database │  │  Redis Cache    │
     │  Cache (EhCache)│ │  (Liquibase  │  │  (10-min TTL,   │
     │  Entity + Query │ │   managed)   │  │   User domain)  │
     └─────────────────┘ └─────────────┘  └─────────────────┘
```

---

## 3. Feature 1 — Liquibase Database Migrations

### What was done
Database schema is managed entirely by Liquibase with 3 incremental changesets, instead of relying on Hibernate's `ddl-auto`.

### Why (Architect perspective)
- **Reproducibility**: Every environment (dev, staging, prod) gets the exact same schema via version-controlled changesets.
- **Auditability**: Each changeset has an ID and author — you can trace who changed what and when.
- **Safety**: Liquibase tracks applied changesets in its `DATABASECHANGELOG` table. It never re-applies a changeset, preventing accidental data loss.
- **Team collaboration**: Multiple developers can add changesets in parallel; conflicts are detected at merge time.

### How (Developer perspective)

**Master changelog** — `src/main/resources/db/changelog/db.changelog-master.yaml`:
```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes/
```

**Changeset 001** — `changes/001-initial-schema.yaml` (author: `initial`):
- Creates `users` table: `id` (BIGINT PK auto-increment), `username` (VARCHAR 255, NOT NULL, UNIQUE), `email` (VARCHAR 255, NOT NULL, UNIQUE), `password` (VARCHAR 255, NOT NULL), `role` (VARCHAR 255)
- Creates `departments` table: `id`, `name` (UNIQUE), `description`
- Creates `employees` table: `id`, `first_name`, `last_name`, `email` (UNIQUE), `phone`, `department_id` (nullable FK)
- Adds FK constraint `fk_employees_department` with `onDelete: SET NULL`

**Changeset 002** — `changes/002-add-indexes.yaml` (author: `initial`):
- `idx_employees_department_id` — speeds up joins/filters by department
- `idx_employees_name` — composite index on `(last_name, first_name)` for name searches
- `idx_users_role` — index on `role` for role-based filtering

**Changeset 003** — `changes/003-add-soft-deletes.yaml` (author: `developer`):
- Adds `deleted BOOLEAN DEFAULT false NOT NULL` to all 3 tables
- Adds indexes: `idx_users_deleted`, `idx_employees_deleted`, `idx_departments_deleted`

**Configuration** — `application-dev.properties`:
```properties
spring.jpa.hibernate.ddl-auto=none          # Liquibase manages schema
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| YAML format | XML, SQL, JSON | YAML is concise and readable; SQL gives more power but is DB-specific |
| `includeAll` | Explicit `include` per file | Auto-discovery reduces boilerplate; files are sorted alphabetically (hence numbering prefix) |
| `ddl-auto=none` | `validate`, `update` | `none` is the only safe option when Liquibase manages schema; `update` would conflict |
| `onDelete: SET NULL` | `CASCADE`, `RESTRICT` | Deleting a department shouldn't cascade-delete employees; they become unassigned instead |
| Separate changeset for indexes | Indexes in initial schema | Incremental changesets show the evolution; easier to reason about in code review |
| Separate changeset for soft deletes | Include `deleted` in initial schema | Demonstrates schema evolution — a realistic pattern for adding features post-launch |

### Interview talking points

**Q: Why Liquibase over Flyway?**
> Both are excellent. Liquibase supports YAML/XML/JSON/SQL formats and has richer rollback support out of the box. Flyway is simpler (SQL-only by default) and may be preferable for teams that want raw SQL control. The project uses Liquibase because it integrates well with Spring Boot's auto-configuration and provides database-agnostic changeset definitions.

**Q: Why not use `ddl-auto=update` for development?**
> `ddl-auto=update` is convenient but dangerous: it never drops columns, can generate suboptimal DDL, and doesn't handle data migrations. Using Liquibase from day one ensures what you test locally is what runs in production. It also catches migration issues early.

**Q: How do you handle rollbacks?**
> Liquibase supports `rollback` blocks per changeset. For `createTable`, the automatic rollback is `dropTable`. For `addColumn`, it's `dropColumn`. In production, rollbacks should be tested in staging first and ideally be forward-only (new changeset to undo) rather than destructive rollbacks.

**Q: What happens if two developers add changesets with the same ID?**
> Liquibase will fail on startup with a checksum/duplicate ID error. The `includeAll` approach combined with numbered prefixes (001, 002, ...) makes this easy to coordinate.

---

## 4. Feature 2 — Domain Models (JPA Entities)

### What was done
Three JPA entities (`User`, `Employee`, `Department`) with Hibernate L2 cache annotations, soft-delete filtering via `@SQLRestriction`, and a `@ManyToOne` relationship between Employee and Department.

### Why (Architect perspective)
- **Separation of domain from schema**: JPA entities map Java objects to relational tables, decoupling business logic from raw SQL.
- **Soft deletes**: Records are never physically removed — they're flagged as `deleted = true`. The `@SQLRestriction("deleted = false")` annotation ensures Hibernate automatically appends `WHERE deleted = false` to all queries, making deleted records invisible without custom query logic.
- **L2 caching at the entity level**: Frequently-read entities (users, employees, departments) are cached in EhCache, reducing database round-trips.

### How (Developer perspective)

**`User.java`** — `src/main/java/.../model/User.java`:
```java
@Entity
@Table(name = "users")
@SQLRestriction("deleted = false")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String role;
    private boolean deleted = false;
    // constructors + getters/setters
}
```

**`Employee.java`** — `src/main/java/.../model/Employee.java`:
```java
@Entity
@Table(name = "employees")
@SQLRestriction("deleted = false")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String firstName;
    @Column(nullable = false) private String lastName;
    @Column(nullable = false, unique = true) private String email;
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    private boolean deleted = false;
}
```

**`Department.java`** — `src/main/java/.../model/Department.java`:
```java
@Entity
@Table(name = "departments")
@SQLRestriction("deleted = false")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String name;
    private String description;
    private boolean deleted = false;
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| `@SQLRestriction` (Hibernate 7) | `@Where` (deprecated), manual query predicates | `@SQLRestriction` is the Hibernate 7 replacement for `@Where`; it's automatic and transparent |
| `CacheConcurrencyStrategy.READ_WRITE` | `NONSTRICT_READ_WRITE`, `READ_ONLY`, `TRANSACTIONAL` | `READ_WRITE` provides strong consistency with locks; Department uses `NONSTRICT_READ_WRITE` because it changes less frequently and eventual consistency is acceptable |
| `FetchType.LAZY` on `@ManyToOne` | `EAGER` (JPA default for `@ManyToOne`) | `LAZY` prevents N+1 in list queries; the department is only loaded when accessed |
| `GenerationType.IDENTITY` | `SEQUENCE`, `TABLE`, `UUID` | `IDENTITY` maps directly to H2's auto-increment; `SEQUENCE` is preferred for PostgreSQL batch inserts but unnecessary here |
| Soft delete via boolean flag | `@SoftDelete` (Hibernate 7), `deletedAt` timestamp | Boolean flag is the simplest approach; a timestamp would also track *when* deletion occurred; `@SoftDelete` is newer but less transparent |

### Interview talking points

**Q: What's the difference between `@SQLRestriction` and `@Where`?**
> `@Where` was deprecated in Hibernate 6.3 and removed in Hibernate 7. `@SQLRestriction` is the direct replacement. Both append a SQL fragment to every query, but `@SQLRestriction` has clearer naming and better integration with Hibernate 7's query model.

**Q: Why `LAZY` fetching on `@ManyToOne`?**
> JPA defaults `@ManyToOne` to `EAGER`, which means loading 100 employees would fire 100 additional queries for departments (N+1 problem). `LAZY` ensures the department is only loaded on demand. In list endpoints, the mapper explicitly accesses `employee.getDepartment()` only when needed, and the L2 cache typically satisfies it.

**Q: How does soft delete work with unique constraints?**
> This is a known challenge. If you soft-delete a user with username "john", you can't create a new user with the same username because the unique constraint still sees the deleted row. Solutions include: composite unique index `(username, deleted)`, partial indexes (Postgres-specific), or removing uniqueness on soft-deleted rows via a trigger. In this project with H2, the simpler boolean approach is used.

**Q: What's the risk of storing password in the entity?**
> The password field is in the entity but is explicitly excluded from `UserResponse` DTO (the mapper never maps it to the response). In production, passwords should be hashed (BCrypt/Argon2) before storage. The current design ensures the password never leaks through the API, but proper hashing should be added before production use.

---

## 5. Feature 3 — Spring Data JPA Repositories

### What was done
Three repository interfaces extending `JpaRepository` and `JpaSpecificationExecutor`, with Hibernate query cache hints on `findAll` and `findById` methods.

### Why (Architect perspective)
- **Zero boilerplate**: Spring Data JPA generates all CRUD implementations at runtime.
- **Specification support**: `JpaSpecificationExecutor` enables type-safe, composable dynamic queries without writing JPQL/SQL.
- **Query cache integration**: By annotating repository methods with `@QueryHints(HINT_CACHEABLE)`, the query results are cached in Hibernate's L2 query cache (EhCache), avoiding repeated identical queries.

### How (Developer perspective)

**`UserRepository.java`** — `src/main/java/.../repository/UserRepository.java`:
```java
public interface UserRepository extends JpaRepository<User, Long>,
                                        JpaSpecificationExecutor<User> {
    @Override
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @Override
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<User> findById(Long id);
}
```

The same pattern is applied to `EmployeeRepository` and `DepartmentRepository`.

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| `JpaSpecificationExecutor` | `@Query` with JPQL, Querydsl, jOOQ | Specifications are part of Spring Data JPA (no extra dependencies), composable, and type-safe via the Criteria API |
| Query cache on `findAll` and `findById` | Cache only at the service layer (Redis) | Hibernate query cache operates at the ORM level — it caches the *query result set* (entity IDs), so even without Redis, repeated identical queries hit the cache |
| Overriding methods to add `@QueryHints` | Global query hint configuration | Per-method hints give fine-grained control; you might not want every query cached (e.g., write-heavy queries) |

### Interview talking points

**Q: How does the Hibernate query cache work differently from Redis?**
> The Hibernate query cache caches *query results* (sets of entity IDs) keyed by the query string + parameters. When the same query runs again, Hibernate retrieves the IDs from the query cache, then loads the entities from the L2 entity cache (EhCache). Redis, in contrast, caches the *serialized DTO response* at the application/service layer. They work at different levels and complement each other.

**Q: What's the risk of enabling query cache?**
> The query cache is invalidated whenever *any* entity in the cached table is modified. For write-heavy tables, this means constant invalidation, making the cache counterproductive. It works best for read-heavy, rarely-changing data. The `default-update-timestamps-region` in EhCache tracks table modification times to detect stale query results.

**Q: Why not use Querydsl?**
> Querydsl generates Q-classes at compile time and offers a more fluent API, but adds build complexity (annotation processing, generated source management). JPA Specifications achieve the same composability with zero extra dependencies since they're part of Spring Data JPA.

---

## 6. Feature 4 — DTO Layer (Request / PatchRequest / Response)

### What was done
For each entity, three DTO types were created:
- **Request** (for `POST`/`PUT`) — all required fields validated with `@NotBlank`
- **PatchRequest** (for `PATCH`) — all fields optional (nullable), only non-null fields are applied
- **Response** — the API response shape, excluding sensitive fields like `password`

### Why (Architect perspective)
- **Security**: DTOs prevent mass-assignment attacks. Without DTOs, a client could send `{"id": 999, "deleted": true}` and directly manipulate entity fields.
- **API contract stability**: The entity structure can change (add columns, rename fields) without breaking the public API contract.
- **Separation of concerns**: Validation rules live in the DTO layer, not the entity. The entity represents the database schema; the DTO represents the API contract.

### How (Developer perspective)

**`UserRequest.java`** — `src/main/java/.../dto/UserRequest.java`:
```java
public class UserRequest {
    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
             message = "Username must contain only alphanumeric characters, underscores, or hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least one uppercase, one lowercase, and one digit")
    private String password;

    @Size(max = 255) @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String role;
}
```

**`UserPatchRequest.java`** — same fields, but **no `@NotBlank`** annotations. `username` has `@Size(min=1)` to prevent empty-string patches.

**`UserResponse.java`** — fields: `id`, `username`, `email`, `role` (no `password`).

**`EmployeeRequest`** validation highlights:
- `firstName`/`lastName`: `@Pattern("^[a-zA-Z '-]+$")` — allows letters, spaces, apostrophes, hyphens
- `phone`: `@Pattern("^[+]?[0-9() -]+$")` — allows international phone formats
- `departmentId`: nullable `Long` — employee can be unassigned

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Separate Request / PatchRequest DTOs | Single DTO with conditional validation | Separate classes make the contract explicit. A PATCH DTO with all-optional fields is semantically different from a POST DTO with all-required fields |
| `@Pattern` regex on all string inputs | Validate only at the database level | Defense-in-depth: validation at the API layer rejects bad input before it reaches the service/database. Patterns also prevent injection via special characters |
| Password excluded from Response | Return all fields | Security best practice. Even hashed passwords shouldn't be exposed through the API |
| `@Size(max = 255)` on all strings | No size limit | Matches the `VARCHAR(255)` database constraint. Prevents oversized payloads and potential denial-of-service |

### Interview talking points

**Q: Why not use a single DTO for both POST and PATCH?**
> A POST requires all fields (`@NotBlank`); a PATCH only applies non-null fields. Combining them requires conditional validation groups (`@Validated(OnCreate.class)` vs `@Validated(OnPatch.class)`), which is more complex and harder to read than separate classes. With records or sealed types in modern Java, you could unify them, but explicit separation is clearer.

**Q: How does PATCH differ from PUT semantically?**
> PUT is a full replacement — the client sends the complete resource, and all fields are overwritten. PATCH is a partial update — only the fields present in the request body are updated. The mapper's `patchEntity()` method checks `if (field != null)` before setting each value.

**Q: Why validate at the DTO level AND use database constraints?**
> Defense-in-depth. DTO validation gives friendly error messages to clients. Database constraints are the last line of defense if a bug bypasses DTO validation. They also protect against direct database access or batch jobs that don't go through the API.

---

## 7. Feature 5 — Input Sanitization (XSS Protection)

### What was done
A utility class `SanitizationUtils` provides two static methods:
1. `sanitize(String)` — OWASP-style HTML entity encoding to prevent stored XSS
2. `escapeWildcards(String)` — escapes SQL LIKE wildcards (`%`, `_`) to prevent wildcard injection in search queries

### Why (Architect perspective)
- **Stored XSS prevention**: If an attacker submits `<script>alert('xss')</script>` as a username, the HTML entity encoding converts it to `&lt;script&gt;...` before storage, neutralizing it when rendered in a browser.
- **SQL wildcard injection prevention**: Without escaping, a search for `%` would match all records, and `_` matches any single character. Escaping ensures user input is treated as literal text.

### How (Developer perspective)

**`SanitizationUtils.java`** — `src/main/java/.../util/SanitizationUtils.java`:
```java
public final class SanitizationUtils {
    private SanitizationUtils() {}

    public static String sanitize(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\"", "&quot;")
                      .replace("'", "&#x27;");
    }

    public static String escapeWildcards(String input) {
        if (input == null) return null;
        return input.replace("%", "\\%").replace("_", "\\_");
    }
}
```

**Usage in Mappers** — every string field is sanitized before being set on the entity:
```java
user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
user.setPassword(request.getPassword());  // Password is NOT sanitized
user.setRole(SanitizationUtils.sanitize(request.getRole()));
```

**Usage in Specifications** — search parameters are escaped before building LIKE clauses:
```java
String escaped = SanitizationUtils.escapeWildcards(username.toLowerCase());
spec = spec.and((root, query, cb) ->
    cb.like(cb.lower(root.get("username")), "%" + escaped + "%", '\\'));
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Manual HTML entity encoding | OWASP Java Encoder library, Jsoup | Zero dependencies. The 5 characters encoded (&, <, >, ", ') cover the OWASP XSS prevention cheat sheet essentials |
| Sanitize on input (store encoded) | Sanitize on output (store raw) | Input sanitization is simpler — you encode once at write time. Output sanitization requires encoding everywhere data is rendered, which is error-prone |
| `&` encoded first | Any order | `&` must be encoded first to prevent double-encoding: if `<` is encoded to `&lt;` first, then encoding `&` would turn `&lt;` into `&amp;lt;` |
| Password NOT sanitized | Sanitize everything | Sanitizing passwords would change the user's actual password. HTML characters in passwords are not a risk because passwords are never rendered in HTML |

### Interview talking points

**Q: Why not use a library like OWASP Java Encoder?**
> In production, using `org.owasp.encoder.Encode.forHtml()` is recommended — it handles edge cases like Unicode escaping. The manual approach here covers the critical 5 characters and demonstrates understanding of the underlying mechanism.

**Q: Input sanitization vs. output encoding — which is better?**
> Output encoding is generally preferred by security experts because it's context-aware (HTML vs. JavaScript vs. URL encoding). However, input sanitization is simpler to implement consistently. The ideal approach is both: sanitize on input as a first defense, and encode on output for defense-in-depth.

**Q: What about SQL injection?**
> SQL injection is already prevented by JPA/Hibernate — parameterized queries are used everywhere. The `escapeWildcards` method doesn't prevent SQL injection; it prevents *wildcard abuse* in LIKE queries, which is a different concern.

---

## 8. Feature 6 — Mapper Layer (Entity-DTO Conversion)

### What was done
Three `@Component` mapper classes (`UserMapper`, `EmployeeMapper`, `DepartmentMapper`) handle all entity-to-DTO and DTO-to-entity conversions. Each provides `toEntity`, `toResponse`, `updateEntity`, and `patchEntity` methods.

### Why (Architect perspective)
- **Single responsibility**: Conversion logic is centralized in one place per domain, not scattered across services and controllers.
- **Testability**: Mappers are simple Spring beans that can be unit-tested without Spring context.
- **Sanitization gate**: All input sanitization happens in the mapper, creating a single enforcement point.

### How (Developer perspective)

**`UserMapper.java`** — `src/main/java/.../mapper/UserMapper.java`:
```java
@Component
public class UserMapper {
    public User toEntity(UserRequest request) {
        User user = new User();
        user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        user.setPassword(request.getPassword());
        user.setRole(SanitizationUtils.sanitize(request.getRole()));
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(),
                                user.getEmail(), user.getRole());
    }

    public void updateEntity(User user, UserRequest request) {
        user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        user.setPassword(request.getPassword());
        user.setRole(SanitizationUtils.sanitize(request.getRole()));
    }

    public void patchEntity(User user, UserPatchRequest request) {
        if (request.getUsername() != null) user.setUsername(SanitizationUtils.sanitize(request.getUsername()));
        if (request.getEmail() != null)    user.setEmail(SanitizationUtils.sanitize(request.getEmail()));
        if (request.getPassword() != null) user.setPassword(request.getPassword());
        if (request.getRole() != null)     user.setRole(SanitizationUtils.sanitize(request.getRole()));
    }
}
```

**`EmployeeMapper`** notable detail — constructor injects `DepartmentMapper` to produce nested `DepartmentResponse` in `EmployeeResponse`:
```java
public EmployeeResponse toResponse(Employee employee) {
    DepartmentResponse deptResponse = employee.getDepartment() != null
        ? departmentMapper.toResponse(employee.getDepartment()) : null;
    return new EmployeeResponse(employee.getId(), employee.getFirstName(),
        employee.getLastName(), employee.getEmail(), employee.getPhone(), deptResponse);
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Manual mappers | MapStruct, ModelMapper, Dozer | Manual mappers give full control over sanitization and null-handling. MapStruct generates code at compile time (faster) but requires annotation processing setup |
| `void updateEntity(entity, request)` | Return new entity | Modifying the existing JPA-managed entity in place is idiomatic — Hibernate's dirty checking handles the rest |
| `patchEntity` with null checks | Reflection-based patching, `Optional<T>` fields | Explicit null checks are verbose but readable and debuggable. `Optional<T>` in DTOs is an anti-pattern per Jackson's design |
| DepartmentMapper injected into EmployeeMapper | Inline department mapping | Composition avoids duplicating department mapping logic |

### Interview talking points

**Q: Why manual mappers instead of MapStruct?**
> MapStruct is the preferred choice for large projects — it generates compile-time mapping code with zero runtime overhead. Manual mappers are used here for transparency and because the sanitization logic in each setter call is easier to understand inline. In a real enterprise project, I'd use MapStruct with custom `@AfterMapping` methods for sanitization.

**Q: How do you handle null vs. empty string in PATCH?**
> Null means "don't change this field." The `patchEntity` method only updates fields where the request value is non-null. However, there's a subtle issue: you can't *clear* a field to null with this approach. Solutions include using `Optional<T>` (awkward with Jackson), a separate "null fields" list, or JSON Merge Patch (RFC 7396).

**Q: Why is `toResponse` important for security?**
> It acts as a whitelist — only explicitly mapped fields appear in the API response. If a new sensitive column is added to the entity (e.g., `ssn`), it won't leak to clients unless someone explicitly adds it to `toResponse`.

---

## 9. Feature 7 — JPA Specifications (Dynamic Filtering)

### What was done
Three specification classes (`UserSpecification`, `EmployeeSpecification`, `DepartmentSpecification`) build composable `Specification<T>` instances for dynamic query filtering using the JPA Criteria API.

### Why (Architect perspective)
- **Dynamic queries**: Filter parameters are optional. Specifications compose with `spec.and(...)` only when a parameter is non-null/non-blank, building the minimal WHERE clause needed.
- **Type safety**: The Criteria API catches invalid field names at compile time (with metamodel generation) or at query execution time.
- **SQL injection prevention**: Parameters are bound via the Criteria API's parameter binding, never concatenated into SQL.

### How (Developer perspective)

**`UserSpecification.java`** — `src/main/java/.../specification/UserSpecification.java`:
```java
public class UserSpecification {
    private UserSpecification() {}

    public static Specification<User> build(String username, String email, String role) {
        Specification<User> spec = Specification.where(null);

        if (username != null && !username.isBlank()) {
            String escaped = SanitizationUtils.escapeWildcards(username.toLowerCase());
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("username")), "%" + escaped + "%", '\\'));
        }
        if (email != null && !email.isBlank()) {
            String escaped = SanitizationUtils.escapeWildcards(email.toLowerCase());
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + escaped + "%", '\\'));
        }
        if (role != null && !role.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("role"), role));
        }
        return spec;
    }
}
```

Key details:
- **Case-insensitive search**: `cb.lower()` on both the column and the search term
- **Wildcard escaping**: `SanitizationUtils.escapeWildcards()` prevents `%` and `_` in user input from acting as SQL wildcards
- **Explicit escape character**: `cb.like(..., '\\')` tells the database that `\\` is the escape character, ensuring cross-database portability
- **`role` uses exact match**: `cb.equal` — roles are looked up by exact value, not partial match

**`EmployeeSpecification`** adds `departmentId` filter:
```java
if (departmentId != null) {
    spec = spec.and((root, query, cb) ->
        cb.equal(root.get("department").get("id"), departmentId));
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Static factory method `build()` | Builder pattern, individual specification methods | Single method is simple; returns a composable `Specification<T>` |
| `Specification.where(null)` as base | `Specification.where(alwaysTrueSpec)` | `where(null)` acts as identity (no-op); Spring Data handles it correctly |
| LIKE with `%prefix%` | Full-text search (PostgreSQL `tsvector`, Elasticsearch) | LIKE is sufficient for small datasets. Full-text search adds infrastructure complexity |
| Explicit escape character `'\\'` | Database default escape character | Not all databases use the same default escape character. Explicitly specifying `'\\'` ensures the escape works on H2, PostgreSQL, MySQL, and Oracle |

### Interview talking points

**Q: What's the N+1 problem and how do Specifications interact with it?**
> Specifications generate a single SQL query with the appropriate WHERE clause. The N+1 problem occurs when *associations* are lazily loaded per entity (e.g., loading department for each employee). The L2 cache mitigates this — after the first load, department lookups hit EhCache instead of the database.

**Q: How would you add sorting to specifications?**
> Sorting is handled by `Pageable` (passed from the controller as `?sort=username,asc`). Spring Data JPA automatically applies `ORDER BY` from the Pageable parameter. The specification only handles filtering (WHERE clause).

**Q: Why escape LIKE wildcards?**
> Without escaping, a user searching for `100%` would match any string starting with `100` (because `%` is a wildcard). Escaping converts it to `100\%`, treating `%` as a literal character. Similarly, `_` (single-character wildcard) is escaped.

---

## 10. Feature 8 — Service Layer with Transaction Management

### What was done
Three service classes (`UserService`, `EmployeeService`, `DepartmentService`) implement business logic with:
- Class-level `@Transactional(readOnly = true)` for read operations
- Method-level `@Transactional` (read-write) for create/update/patch/delete
- Soft-delete implementation (sets `deleted = true` instead of removing rows)
- Audit logging for all write operations

### Why (Architect perspective)
- **Transaction boundaries**: The service layer is the correct place for transaction demarcation. Controllers shouldn't manage transactions (they're HTTP concerns), and repositories are too fine-grained.
- **`readOnly = true` optimization**: Read-only transactions skip dirty checking, reduce memory usage, and can use read replicas in a clustered database setup.
- **Soft delete**: Data is never lost. Compliance requirements (GDPR audit trail, financial regulations) often require keeping records. Soft-deleted data can be restored or permanently purged in a scheduled job.

### How (Developer perspective)

**`UserService.java`** — `src/main/java/.../service/UserService.java`:
```java
@Service
@Transactional(readOnly = true)
public class UserService {
    private static final String CACHE_NAME = "users";
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Constructor injection (no @Autowired needed — single constructor)

    public Page<UserResponse> findAll(String username, String email, String role, Pageable pageable) {
        return userRepository.findAll(UserSpecification.build(username, email, role), pageable)
                .map(userMapper::toResponse);
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public UserResponse findResponseById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    @Transactional  // Overrides class-level readOnly=true
    @CachePut(value = CACHE_NAME, key = "#result.id")
    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Created user id={}", response.getId());
        return response;
    }

    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse update(Long id, UserRequest request) {
        User user = findEntityById(id);
        userMapper.updateEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Updated user id={}", id);
        return response;
    }

    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public UserResponse patch(Long id, UserPatchRequest request) {
        User user = findEntityById(id);
        userMapper.patchEntity(user, request);
        UserResponse response = userMapper.toResponse(userRepository.save(user));
        log.info("Patched user id={}", id);
        return response;
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        User user = findEntityById(id);
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Soft-deleted user id={}", id);
    }
}
```

**`EmployeeService`** notable detail — resolving department association:
```java
private void resolveDepartment(Employee employee, Long departmentId) {
    if (departmentId != null) {
        Department dept = departmentService.findEntityById(departmentId);
        employee.setDepartment(dept);
    } else {
        employee.setDepartment(null);
    }
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Class-level `readOnly=true` + method-level `@Transactional` override | Per-method `@Transactional` everywhere | Reduces annotation noise — most service methods are reads; only writes need the override |
| `findEntityById` returns `User` (entity) | Always return DTOs | Other services (like EmployeeService) need the entity to set associations; the DTO doesn't expose the JPA-managed entity |
| `@CachePut` on create/update | `@CacheEvict` on create/update | `@CachePut` updates the cache with the new value immediately, so the next read hits the cache. `@CacheEvict` would force a cache miss on the next read |
| `@CacheEvict` on delete | No cache action | The deleted entity should not be served from cache. Eviction ensures the next lookup goes to the database (where `@SQLRestriction` filters it out) |
| Logging at `info` level for writes | Audit table, event sourcing | Simple SLF4J logging is sufficient for this scope. In production, an audit table or event stream provides better queryability |

### Interview talking points

**Q: What does `@Transactional(readOnly = true)` actually do?**
> Three things: (1) Hibernate skips dirty checking at flush time (performance), (2) Some JDBC drivers send a `SET TRANSACTION READ ONLY` hint, enabling database optimizations, (3) In a primary-replica setup, Spring can route read-only transactions to the replica.

**Q: What happens if `findEntityById` is called within `create()` — does it open a new transaction?**
> No. Both methods are in the same bean, so Spring's proxy handles it. `create()` opens a read-write transaction, and `findEntityById()` participates in the same transaction (Spring's default propagation is `REQUIRED`). Self-invocation within the same bean doesn't go through the proxy, so the `readOnly` attribute on the class is not re-evaluated.

**Q: Why `@CachePut(key = "#result.id")` on create?**
> At invocation time, the entity hasn't been saved yet, so there's no `id` parameter. `#result.id` tells Spring to use the return value's `id` field as the cache key, which is populated after `repository.save()`.

**Q: How would you handle cache consistency in a multi-instance deployment?**
> Redis is centralized — all instances share the same cache. However, Hibernate's L2 cache (EhCache) is local to each JVM. In a multi-instance setup, you'd replace EhCache with a distributed cache (Infinispan, Hazelcast, or Redis-backed JCache provider) or accept slight staleness with shorter TTLs.

---

## 11. Feature 9 — REST Controllers with HATEOAS

### What was done
Three REST controllers (`UserController`, `EmployeeController`, `DepartmentController`) expose a full CRUD API with:
- HATEOAS links (self + collection) on every response using Spring HATEOAS
- `PagedModel` for paginated list responses with navigation links
- `@Validated` at class level for query parameter validation
- `Location` header on POST (201 Created)

### Why (Architect perspective)
- **HATEOAS (Richardson Maturity Level 3)**: Clients don't need to construct URLs — they follow links from the response. This decouples clients from URL structure and enables API discoverability.
- **`PagedModel`**: Provides standardized pagination metadata (page number, size, total elements, total pages) along with `first`/`last`/`next`/`prev` navigation links.
- **`Location` header on POST**: HTTP 201 spec requires a `Location` header pointing to the newly created resource.

### How (Developer perspective)

**`UserController.java`** — `src/main/java/.../controller/UserController.java`:
```java
@Validated
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {
    private final UserService userService;
    private final PagedResourcesAssembler<UserResponse> pagedAssembler;

    // Constructor injection

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getAll(
            @RequestParam(required = false) @Size(max = 255) String username,
            @RequestParam(required = false) @Size(max = 255) String email,
            @RequestParam(required = false) @Size(max = 255) String role,
            Pageable pageable) {
        Page<UserResponse> page = userService.findAll(username, email, role, pageable);
        return ResponseEntity.ok(pagedAssembler.toModel(page, this::toEntityModel));
    }

    @PostMapping
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(toEntityModel(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<UserResponse> toEntityModel(UserResponse response) {
        return EntityModel.of(response,
            linkTo(methodOn(UserController.class).getById(response.getId())).withSelfRel(),
            linkTo(methodOn(UserController.class).getAll(null, null, null, null)).withRel("users"));
    }
}
```

**Response example** (GET /api/v1/users/1):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "role": "ADMIN",
  "_links": {
    "self": { "href": "http://localhost:8080/api/v1/users/1" },
    "users": { "href": "http://localhost:8080/api/v1/users" }
  }
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Spring HATEOAS `EntityModel` | Plain DTOs, JSON:API, HAL-FORMS | `EntityModel` produces HAL format, which is the most widely adopted hypermedia type. Spring HATEOAS has first-class support |
| `PagedResourcesAssembler` | Manual pagination metadata | Auto-generates `_links` with `first`, `prev`, `next`, `last` page links — less boilerplate |
| `@Validated` on class + `@Size` on query params | Validate in service layer | Query parameter validation (e.g., max length) is an HTTP concern and belongs in the controller. `@Validated` enables method-level constraint validation on parameters |
| API versioning via URL path (`/api/v1/`) | Header versioning, media type versioning | URL path versioning is the most visible, debuggable, and cacheable approach. Header/media-type versioning is more RESTful but less practical |
| `ServletUriComponentsBuilder` for Location header | Hardcoded URL | Dynamically builds the URL from the current request context, respecting proxies and port forwarding |

### Interview talking points

**Q: What's the difference between `@Valid` and `@Validated`?**
> `@Valid` (Jakarta standard) triggers bean validation on `@RequestBody` objects. `@Validated` (Spring extension) enables method-level validation — needed for validating `@RequestParam` and `@PathVariable` constraints. You need both: `@Validated` on the class, `@Valid` on request body parameters.

**Q: Why return `EntityModel<UserResponse>` instead of just `UserResponse`?**
> `EntityModel` wraps the DTO and adds `_links`. This enables HATEOAS — clients can discover related resources (self link, collection link) without hardcoding URLs. It's a key step toward RESTful API maturity (Richardson Level 3).

**Q: How does pagination work?**
> Spring Data's `Pageable` is automatically resolved from query parameters: `?page=0&size=20&sort=username,asc`. `PagedResourcesAssembler` converts the `Page<T>` into a `PagedModel<T>` with navigation links. Default page size (20) and max size (100) are configured in `application.properties`.

**Q: Why 204 No Content on DELETE instead of 200?**
> 204 indicates the action succeeded but there's no response body to return. This is the standard for DELETE operations. Returning 200 with the deleted entity is also valid but reveals information about a resource that no longer exists.

---

## 12. Feature 10 — Global Exception Handling

### What was done
A `@RestControllerAdvice` class (`GlobalExceptionHandler`) catches all exceptions thrown by controllers and converts them into consistent JSON error responses with appropriate HTTP status codes.

### Why (Architect perspective)
- **Consistent error format**: Every error response has the same structure (`timestamp`, `status`, `error`, `message`), making it easy for clients to parse errors programmatically.
- **No stack traces leaked**: The catch-all handler logs the full stack trace server-side but returns only a generic message to the client, preventing information leakage.
- **Centralized**: All error handling is in one place. Controllers don't need try-catch blocks.

### How (Developer perspective)

**`GlobalExceptionHandler.java`** — `src/main/java/.../exception/GlobalExceptionHandler.java`:

| Exception | HTTP Status | Error Key | Response Message |
|---|---|---|---|
| `ResourceNotFoundException` | 404 | "Not Found" | Dynamic (e.g., "User not found with id 42") |
| `MethodArgumentNotValidException` | 400 | "Validation Failed" | "Input validation failed" + `fieldErrors` map |
| `ConstraintViolationException` | 400 | "Validation Failed" | "Input validation failed" + `fieldErrors` map |
| `DataIntegrityViolationException` | 409 | "Conflict" | "A resource with the given unique field(s) already exists" |
| `MethodArgumentTypeMismatchException` | 400 | "Bad Request" | "Parameter 'x' must be of type Y" |
| `HttpMessageNotReadableException` | 400 | "Bad Request" | "Malformed JSON request body" |
| `CallNotPermittedException` | 503 | "Service Unavailable" | "Service is temporarily unavailable, please try again later" |
| `BulkheadFullException` | 429 | "Too Many Requests" | "Too many concurrent requests, please try again later" |
| `Exception` (catch-all) | 500 | "Internal Server Error" | "An unexpected error occurred" |

**Validation error response example**:
```json
{
  "timestamp": "2026-02-16T10:30:00.123",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "fieldErrors": {
    "username": "Username is required",
    "email": "Email must be valid"
  }
}
```

**Key implementation detail** — extracting field name from `ConstraintViolation` path:
```java
ex.getConstraintViolations().forEach(violation -> {
    String path = violation.getPropertyPath().toString();
    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
    fieldErrors.put(field, violation.getMessage());
});
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| `@RestControllerAdvice` | `@ControllerAdvice`, `ErrorController`, `@ExceptionHandler` per controller | `@RestControllerAdvice` combines `@ControllerAdvice` + `@ResponseBody`; global scope eliminates duplication |
| `LinkedHashMap` for response body | Custom `ErrorResponse` POJO | Maps are flexible and match the OpenAPI `ErrorResponse` schema. A POJO would add type safety but another class to maintain |
| `ResourceNotFoundException` ignored by circuit breaker | All exceptions recorded | A 404 is a normal business outcome (resource doesn't exist), not a system failure. Recording it would trip the circuit breaker on legitimate "not found" queries |
| Separate handlers for `MethodArgumentNotValidException` and `ConstraintViolationException` | Single handler | They come from different validation mechanisms: `@Valid` on request bodies vs `@Validated` on method parameters. Different exception types require different extraction logic |
| Log at WARN for 4xx, ERROR for 5xx | Uniform log level | 4xx errors are client mistakes (expected); 5xx errors are server failures (unexpected). Different log levels help operations teams filter alerts |

### Interview talking points

**Q: Why does `DataIntegrityViolationException` return 409 Conflict?**
> A unique constraint violation means the client tried to create/update a resource that conflicts with an existing one. 409 Conflict is semantically correct — the request is valid in isolation but conflicts with the current state of the resource.

**Q: What if the exception handler itself throws an exception?**
> Spring Boot's default `BasicErrorController` handles it as a fallback, returning a standard error page. In practice, exception handlers should be kept simple to avoid this scenario.

**Q: Why not use RFC 7807 Problem Details?**
> Spring 6+ natively supports Problem Details (`application/problem+json`). It's a better standard, but the project uses a simpler custom format. Migrating to Problem Details would involve extending `ResponseEntityExceptionHandler` and returning `ProblemDetail` objects.

**Q: How does the `CallNotPermittedException` handler fit in?**
> When a circuit breaker is in OPEN state, Resilience4j throws `CallNotPermittedException` before the method even executes. The handler converts this to a 503, signaling the client to retry later.

---

## 13. Feature 11 — OpenAPI / Swagger Documentation

### What was done
Integrated `springdoc-openapi` with:
- Swagger UI at `/swagger-ui.html`
- OpenAPI JSON spec at `/v3/api-docs`
- Custom `ErrorResponse` schema component
- `@Operation`, `@ApiResponse`, `@Parameter`, `@Tag` annotations on all controller methods

### Why (Architect perspective)
- **API-first documentation**: Documentation is generated from code annotations, so it's always in sync with the implementation.
- **Client SDK generation**: The OpenAPI spec can be used to auto-generate client SDKs in any language using tools like OpenAPI Generator.
- **Developer experience**: Swagger UI provides an interactive sandbox for testing endpoints without Postman/curl.

### How (Developer perspective)

**`OpenApiConfig.java`** — `src/main/java/.../config/OpenApiConfig.java`:
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        Schema<?> errorSchema = new MapSchema()
            .addProperty("timestamp", new StringSchema().example("2026-02-16T10:30:00.000"))
            .addProperty("status", new Schema<Integer>().type("integer").example(400))
            .addProperty("error", new StringSchema().example("Bad Request"))
            .addProperty("message", new StringSchema().example("Input validation failed"));

        return new OpenAPI()
            .info(new Info().title("SimpleEnterprizeProj2 API").version("1.0")
                  .description("REST API for managing users, employees, and departments"))
            .components(new Components().addSchemas("ErrorResponse", errorSchema));
    }
}
```

**Controller annotations example**:
```java
@Operation(summary = "Create user", description = "Create a new user")
@ApiResponse(responseCode = "201", description = "User created successfully")
@ApiResponse(responseCode = "400", description = "Invalid request body",
    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
@ApiResponse(responseCode = "409", description = "User with given unique field(s) already exists",
    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
```

**Configuration** — `application.properties`:
```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| springdoc-openapi | SpringFox (Swagger 2), hand-written OpenAPI YAML | springdoc is the maintained successor to SpringFox for Spring Boot 3+; it supports OpenAPI 3.0/3.1 natively |
| Schema refs for error responses | Inline error schemas per endpoint | `$ref` to a shared `ErrorResponse` schema avoids duplication and keeps the spec DRY |
| Annotations on controllers | Separate OpenAPI YAML file | Annotations stay close to the code, reducing drift. A separate YAML file gives more control but requires manual synchronization |

### Interview talking points

**Q: What's the difference between OpenAPI 2.0 (Swagger) and OpenAPI 3.x?**
> OpenAPI 3.0 introduced `components` (reusable schemas, responses, parameters), request body as a separate concept (not in parameters), `oneOf`/`anyOf`/`allOf` for schema composition, and server URL definitions. OpenAPI 3.1 aligned with JSON Schema draft 2020-12.

**Q: How do you version your API documentation?**
> The OpenAPI `info.version` tracks the API version ("1.0"). URL path versioning (`/api/v1/`) ensures different API versions can have different documentation. In a multi-version setup, you'd configure separate OpenAPI groups per version.

**Q: How would you secure the Swagger UI in production?**
> Options include: (1) Disable it entirely in prod via Spring profile, (2) Protect it behind authentication (Spring Security), (3) Restrict access by IP/network. In this project, it's always available, which is appropriate for development but should be restricted in production.

---

## 14. Feature 12 — Redis Caching (Application-Level)

### What was done
Redis is used as an application-level cache for the `User` domain with:
- `@Cacheable` on `findResponseById` — cache reads
- `@CachePut` on `create`, `update`, `patch` — update cache on writes
- `@CacheEvict` on `delete` — remove from cache on deletion
- 10-minute TTL with JSON serialization

### Why (Architect perspective)
- **Distributed cache**: Unlike EhCache (JVM-local), Redis is shared across all application instances, ensuring cache consistency in a horizontally-scaled deployment.
- **Reduced DB load**: Frequently accessed user profiles are served from Redis (sub-millisecond) instead of hitting the database.
- **TTL-based expiry**: The 10-minute TTL balances freshness with performance — stale data is at most 10 minutes old.

### How (Developer perspective)

**`RedisCacheConfig.java`** — `src/main/java/.../config/RedisCacheConfig.java`:
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

**Service-layer annotations** (in `UserService`):
```java
@Cacheable(value = "users", key = "#id")          // Read: check cache first
public UserResponse findResponseById(Long id) { ... }

@CachePut(value = "users", key = "#result.id")    // Write: update cache after DB write
public UserResponse create(UserRequest request) { ... }

@CacheEvict(value = "users", key = "#id")          // Delete: remove from cache
public void delete(Long id) { ... }
```

**Configuration** — `application-dev.properties`:
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Redis only for User domain | Cache all entities | Demonstrates selective caching. Employee/Department are less frequently accessed by ID in this domain model. Adding caching is trivial (add annotations) when needed |
| `GenericJackson2JsonRedisSerializer` | `JdkSerializationRedisSerializer`, Kryo, Protobuf | JSON is human-readable (debuggable in Redis CLI), language-agnostic, and doesn't require `Serializable` on DTOs |
| `disableCachingNullValues()` | Allow null caching | Caching null values can prevent cache penetration (repeated lookups for non-existent IDs). Disabled here because `findResponseById` throws `ResourceNotFoundException` on null, so null is never returned |
| `@CachePut` on writes (not `@CacheEvict`) | Evict on writes, let next read populate cache | `@CachePut` immediately refreshes the cache with the new value, eliminating a cache miss on the next read. This is the "write-through" pattern |
| 10-minute TTL | No TTL (manual eviction only) | TTL acts as a safety net: even if a cache eviction is missed (bug, crash), stale data is at most 10 minutes old |

### Interview talking points

**Q: What's the difference between `@Cacheable`, `@CachePut`, and `@CacheEvict`?**
> `@Cacheable` checks the cache before executing the method; if the key exists, the method is skipped. `@CachePut` always executes the method and puts the result in the cache (write-through). `@CacheEvict` removes the entry from the cache. They serve reads, writes, and deletes respectively.

**Q: What about cache stampede (thundering herd)?**
> If the cache entry expires and 1000 concurrent requests arrive, all 1000 hit the database simultaneously. Solutions: (1) `sync = true` on `@Cacheable` (only one thread fetches, others wait), (2) probabilistic early expiration, (3) background refresh before TTL. This project doesn't address it — acceptable for low-traffic scenarios.

**Q: How do you debug what's in Redis?**
> Use `redis-cli` with commands like `KEYS users::*`, `GET users::1`, `TTL users::1`. The JSON serializer makes the values human-readable. In production, use Redis monitoring tools (RedisInsight, Prometheus + Grafana).

**Q: Why not use `@Cacheable` on `findAll` (list endpoint)?**
> List queries with filters and pagination produce an exponential number of cache keys. The cache hit rate would be low, and cache invalidation becomes complex (any create/update/delete affects potentially all list cache entries). The Hibernate query cache handles this more efficiently at the ORM level.

---

## 15. Feature 13 — Hibernate L2 Cache with EhCache

### What was done
A two-level Hibernate cache is configured:
- **L2 Entity Cache**: Caches individual entities (`User`, `Employee`, `Department`) in EhCache after they're loaded from the database.
- **Query Cache**: Caches the result sets (entity IDs) of queries annotated with `@QueryHints(HINT_CACHEABLE)`.
- **Update Timestamps Cache**: Tracks when tables were last modified to detect stale query cache entries.

### Why (Architect perspective)
- **Complementary to Redis**: Redis caches serialized DTOs at the service layer. Hibernate L2 cache operates at the ORM layer, caching entity objects and query results. A `findById` call can be served entirely from EhCache without any SQL execution.
- **Transparent**: The application code doesn't need to know about L2 caching — Hibernate manages it internally. Annotations on entities enable it selectively.
- **Query cache**: For repeated identical queries (same parameters, same pagination), the query cache stores the list of entity IDs. The entities themselves are then fetched from the entity cache, avoiding any SQL.

### How (Developer perspective)

**Entity annotations** (on each entity class):
```java
@Cacheable                                            // JPA standard: opt into L2 cache
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)   // Hibernate: strong consistency with locks
```

**Repository query hints**:
```java
@QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
Page<User> findAll(Specification<User> spec, Pageable pageable);
```

**`HibernateCacheConfig.java`** — `src/main/java/.../config/HibernateCacheConfig.java`:
```java
@Configuration
public class HibernateCacheConfig implements HibernatePropertiesCustomizer {
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(ConfigSettings.CONFIG_URI,
            getClass().getResource("/ehcache.xml").toURI().toString());
    }
}
```

**`ehcache.xml`** — `src/main/resources/ehcache.xml`:
```xml
<!-- User entity: 1000 entries, 10-min TTL -->
<cache alias="org.sample.simpleenterprizeproj2.model.User">
    <expiry><ttl unit="minutes">10</ttl></expiry>
    <heap unit="entries">1000</heap>
</cache>

<!-- Employee entity: 2000 entries, 10-min TTL -->
<cache alias="org.sample.simpleenterprizeproj2.model.Employee">
    <expiry><ttl unit="minutes">10</ttl></expiry>
    <heap unit="entries">2000</heap>
</cache>

<!-- Department entity: 200 entries, 10-min TTL -->
<cache alias="org.sample.simpleenterprizeproj2.model.Department">
    <expiry><ttl unit="minutes">10</ttl></expiry>
    <heap unit="entries">200</heap>
</cache>

<!-- Query results: 500 entries, 5-min TTL -->
<cache alias="default-query-results-region">
    <expiry><ttl unit="minutes">5</ttl></expiry>
    <heap unit="entries">500</heap>
</cache>

<!-- Update timestamps: no expiry (critical!) -->
<cache alias="default-update-timestamps-region">
    <expiry><none/></expiry>
    <heap unit="entries">5000</heap>
</cache>
```

**Properties** — `application.properties`:
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.use_query_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=jcache
spring.jpa.properties.hibernate.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider
spring.jpa.properties.jakarta.persistence.sharedCache.mode=ENABLE_SELECTIVE
```

**Dev-only statistics** — `application-dev.properties`:
```properties
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.properties.hibernate.cache.use_structured_entries=true
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| EhCache 3 (via JCache/JSR-107) | Hazelcast, Infinispan, Caffeine | EhCache 3 is mature, lightweight, and runs in-process. JSR-107 compliance means it's swappable with any JCache provider |
| `ENABLE_SELECTIVE` | `ALL`, `NONE` | Only entities annotated with `@Cacheable` are cached. This prevents accidentally caching entities that shouldn't be (e.g., audit logs, frequently-changing data) |
| Separate TTLs for entity vs. query cache | Uniform TTL | Query results change more frequently (any insert/update/delete invalidates them), so they get a shorter TTL (5 min) than entity data (10 min) |
| `default-update-timestamps-region` has no expiry | TTL on timestamps | This region tracks table modification times. If timestamps expire, Hibernate might serve stale query results. Setting no expiry is critical for correctness |
| Heap-only storage | Off-heap, disk tiering | Heap is fastest. Off-heap (EhCache tiered storage) is useful for large datasets but adds GC complexity. For 1000-2000 entries, heap is sufficient |

### Interview talking points

**Q: How do the Hibernate L2 cache and Redis cache work together?**
> They operate at different levels:
> 1. **Request flow**: Controller → Service → `@Cacheable` (Redis) → Repository → Hibernate L2 (EhCache) → Database
> 2. For `UserService.findResponseById(1)`: Redis is checked first (service layer). If miss, the repository executes a query. Hibernate checks its L2 entity cache before hitting the database.
> 3. For `EmployeeService` (no Redis): only the Hibernate L2 cache + query cache provide caching.

**Q: What's the update-timestamps region and why is it critical?**
> When you execute `UPDATE users SET email='new' WHERE id=1`, Hibernate records a timestamp in `default-update-timestamps-region` for the `users` table. When a cached query for `users` is retrieved, Hibernate compares the query's cache time against the table's last-update timestamp. If the table was modified after the query was cached, the cached result is discarded. Without this region (or if it expires), stale query results would be served indefinitely.

**Q: Why `READ_WRITE` for User/Employee and `NONSTRICT_READ_WRITE` for Department?**
> `READ_WRITE` uses soft locks to prevent dirty reads during concurrent updates — essential for frequently-updated entities. `NONSTRICT_READ_WRITE` has no locking — it can serve stale data during a short window after an update. This is acceptable for Department, which changes rarely (organizational structure is relatively static).

**Q: How do you monitor cache effectiveness?**
> In dev mode, `hibernate.generate_statistics=true` enables stats accessible via JMX or logging. Key metrics: hit ratio, miss count, put count, eviction count. In production, integrate with Micrometer/Prometheus for real-time dashboards.

---

## 16. Feature 14 — Resilience4j Circuit Breakers

### What was done
Every service method is annotated with `@CircuitBreaker` from Resilience4j. Three named circuit breaker instances (`userService`, `employeeService`, `departmentService`) share a common configuration. `ResourceNotFoundException` is explicitly ignored (not counted as a failure).

### Why (Architect perspective)
- **Fail-fast**: When a downstream dependency (database, Redis) is failing, the circuit breaker prevents cascading failures by short-circuiting calls. Instead of waiting for timeouts, calls fail immediately with `CallNotPermittedException`.
- **Self-healing**: The circuit breaker automatically transitions from OPEN → HALF_OPEN → CLOSED as the system recovers.
- **Selective failure recording**: 404 (resource not found) is a normal business outcome, not a system failure. Ignoring it prevents legitimate "not found" queries from tripping the breaker.

### How (Developer perspective)

**Service annotation**:
```java
@CircuitBreaker(name = "userService")
public UserResponse findResponseById(Long id) { ... }
```

**Configuration** — `application.properties`:
```properties
# Shared default configuration
resilience4j.circuitbreaker.configs.default.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.configs.default.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.configs.default.record-exceptions=java.lang.Exception
resilience4j.circuitbreaker.configs.default.ignore-exceptions=\
    org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException

# Named instances inheriting the default
resilience4j.circuitbreaker.instances.userService.base-config=default
resilience4j.circuitbreaker.instances.employeeService.base-config=default
resilience4j.circuitbreaker.instances.departmentService.base-config=default
```

**Circuit breaker state machine**:
```
          5+ calls, <50% failures
CLOSED ──────────────────────────── CLOSED (stay)
   │
   │  5+ calls, ≥50% failures
   ▼
 OPEN ──── (reject all calls with CallNotPermittedException)
   │
   │  after 10 seconds (automatic transition)
   ▼
HALF_OPEN ──── (permit 3 trial calls)
   │         │
   │ <50%    │ ≥50% failures
   │ failures│
   ▼         ▼
CLOSED     OPEN
```

**Exception handling integration** — `GlobalExceptionHandler`:
```java
@ExceptionHandler(CallNotPermittedException.class)
public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
            "Service is temporarily unavailable, please try again later");
}
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| `COUNT_BASED` sliding window | `TIME_BASED` | Count-based is predictable: failure rate is calculated over the last N calls. Time-based calculates over the last N seconds, which can be unpredictable under varying load |
| Window size of 10 | Larger (50+) for smoother averaging | Small window reacts faster to failures but is more sensitive to transient errors. 10 is a good balance for a microservice with moderate traffic |
| 50% failure threshold | Higher (70-80%) for more tolerance | 50% means "half the calls are failing" — a clear signal of systemic issues. Higher thresholds would delay circuit opening |
| `ResourceNotFoundException` ignored | Record all exceptions | A 404 is not a system failure — it's a normal "not found" response. Recording it would cause the breaker to open when users search for non-existent resources |
| `automatic-transition-from-open-to-half-open-enabled=true` | Manual transition via actuator | Automatic transition enables self-healing without operator intervention. In critical systems, you might want manual control via management endpoints |
| No fallback method (initially) | `@CircuitBreaker(fallbackMethod = "fallback")` | Fallbacks were added later in Feature 16 (Retry & Fallback). Initially, the 503 response from `GlobalExceptionHandler` served as the implicit fallback |

### Interview talking points

**Q: What's the difference between a circuit breaker and a retry?**
> A retry repeats the same call hoping for success. A circuit breaker *stops* calling the failing service entirely. They're complementary: retry handles transient errors (network blip), circuit breaker handles sustained outages (database down). Without a circuit breaker, retries can overwhelm an already-failing service.

**Q: Why per-service circuit breakers instead of a global one?**
> Fault isolation. If the department service's database table is corrupted, only `departmentService` circuit opens. User and employee endpoints continue working. A global circuit breaker would take down the entire API.

**Q: How does the sliding window work?**
> COUNT_BASED maintains a ring buffer of the last N call outcomes. After each call, it calculates the failure percentage. With `sliding-window-size=10` and `failure-rate-threshold=50`, if 5 out of the last 10 calls failed, the circuit opens. `minimum-number-of-calls=5` means the circuit won't evaluate the threshold until at least 5 calls have been recorded.

**Q: What happens to in-flight requests when the circuit opens?**
> Calls currently executing are allowed to complete. Only *new* calls are rejected with `CallNotPermittedException`. The circuit opening doesn't cancel or interrupt running requests.

---

## 17. Feature 15 — Observability (Logging, Correlation IDs, Security Headers)

### What was done
Three cross-cutting components provide observability and security:
1. **`RequestLoggingFilter`**: Assigns a UUID correlation ID to every request, measures duration, logs at appropriate levels
2. **`SecurityHeadersFilter`**: Adds security response headers to every HTTP response
3. **`logback-spring.xml`**: Profile-aware logging configuration (text in dev, JSON in prod)

### Why (Architect perspective)
- **Correlation IDs**: In a distributed system, a single user action can generate multiple service calls. The correlation ID (stored in SLF4J MDC) ties all log entries for a single request together, enabling end-to-end tracing across log aggregation systems (ELK, Splunk, CloudWatch).
- **Security headers**: Defense-in-depth. Even without Spring Security, response headers prevent common browser-based attacks (XSS, clickjacking, MIME sniffing).
- **Structured logging in prod**: JSON logs are machine-parseable, enabling log aggregation pipelines to extract fields (correlationId, level, logger) without regex parsing.

### How (Developer perspective)

**`RequestLoggingFilter.java`** — `src/main/java/.../config/RequestLoggingFilter.java`:
```java
@Component
public class RequestLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = httpResponse.getStatus();
            String uri = httpRequest.getQueryString() != null
                ? httpRequest.getRequestURI() + "?" + httpRequest.getQueryString()
                : httpRequest.getRequestURI();

            if (status >= 500) log.error("{} {} {} {}ms", httpRequest.getMethod(), uri, status, duration);
            else if (status >= 400) log.warn("{} {} {} {}ms", httpRequest.getMethod(), uri, status, duration);
            else log.info("{} {} {} {}ms", httpRequest.getMethod(), uri, status, duration);

            MDC.clear();
        }
    }
}
```

**`SecurityHeadersFilter.java`** — `src/main/java/.../config/SecurityHeadersFilter.java`:
```java
@Component
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'none'");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        chain.doFilter(request, response);
    }
}
```

**`logback-spring.xml`** — `src/main/resources/logback-spring.xml`:
```xml
<configuration>
    <!-- Dev: human-readable with correlation ID -->
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <logger name="org.sample.simpleenterprizeproj2" level="DEBUG"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>

    <!-- Prod: structured JSON for log aggregation -->
    <springProfile name="prod">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="ch.qos.logback.classic.encoder.JsonEncoder"/>
        </appender>
        <logger name="org.sample.simpleenterprizeproj2" level="INFO"/>
        <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>
</configuration>
```

**Dev log output example**:
```
10:30:15.123 [http-nio-8080-exec-1] [a1b2c3d4-e5f6-7890-abcd-ef1234567890] INFO  RequestLoggingFilter - GET /api/v1/users?page=0 200 45ms
```

### Security headers explained

| Header | Value | Protection Against |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | MIME-type sniffing attacks (browser respects declared Content-Type) |
| `X-Frame-Options` | `DENY` | Clickjacking (prevents embedding in iframes) |
| `X-XSS-Protection` | `1; mode=block` | Reflected XSS (legacy browser XSS filter) |
| `Content-Security-Policy` | `default-src 'none'` | XSS, data injection (no resources loaded unless explicitly allowed) |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | Sensitive data caching (browsers/proxies don't cache API responses) |

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| `jakarta.servlet.Filter` | Spring `OncePerRequestFilter`, `HandlerInterceptor` | `Filter` operates at the servlet level (before/after Spring MVC), ensuring every request is covered including error paths |
| UUID for correlation ID | Incoming `X-Request-Id` header, Zipkin trace ID | UUID is self-contained (no external dependency). In a microservice mesh, you'd propagate an incoming `X-Request-Id` or use distributed tracing (Zipkin/Jaeger) |
| `MDC.clear()` in `finally` block | Don't clear | MDC is thread-local. Without clearing, a thread returned to the pool could leak the correlation ID to the next request (thread reuse in servlet containers) |
| `JsonEncoder` for prod | Custom JSON layout, Logstash encoder | Logback's built-in `JsonEncoder` is simple and sufficient. `LogstashEncoder` adds more fields (stack trace formatting, custom fields) but requires an extra dependency |
| `Content-Security-Policy: default-src 'none'` | More permissive CSP | For a REST API (no HTML/JS served), `default-src 'none'` is maximally restrictive. A web app serving HTML would need `script-src`, `style-src`, etc. |

### Interview talking points

**Q: Why use MDC instead of passing correlation ID as a method parameter?**
> MDC (Mapped Diagnostic Context) is thread-local storage integrated with SLF4J. Any `log.info()` call anywhere in the call stack automatically includes the correlation ID in the log output — no need to pass it through every method signature. This is especially powerful in deep call stacks.

**Q: What's the risk of `MDC.clear()` vs `MDC.remove("correlationId")`?**
> `MDC.clear()` removes ALL MDC entries, which could wipe out entries set by other filters. `MDC.remove("correlationId")` is safer — it only removes the specific key. In this project, since `RequestLoggingFilter` is the only MDC user, `clear()` is fine.

**Q: Are security headers sufficient without Spring Security?**
> For a REST API, these headers provide good baseline protection. Spring Security adds authentication, authorization, CSRF protection, CORS configuration, and session management. The headers here address browser-based attacks, while Spring Security addresses access control.

**Q: How would you add distributed tracing?**
> Add `micrometer-tracing-bridge-brave` (or OpenTelemetry) to the classpath. Spring Boot auto-configures trace ID and span ID propagation. The correlation ID filter becomes unnecessary as the tracing library handles it with richer context (parent-child spans, cross-service propagation).

---

## 18. Feature 16 — Resilience4j Retry & Fallback

### What was done
Added `@Retry` annotations to all 21 service methods alongside the existing `@CircuitBreaker` annotations. Implemented fallback methods on the circuit breaker (outermost decorator) to provide graceful degradation for list queries and explicit 503 errors for single-resource and write operations. Created `ServiceUnavailableException` with a corresponding `@ExceptionHandler` in `GlobalExceptionHandler`.

### Why (Architect perspective)
- **Transient failure recovery**: Network blips, temporary database connection issues, and brief GC pauses are common in production. A retry with exponential backoff handles these without user-visible errors.
- **Decorator composition**: `CircuitBreaker(Retry(method))` — retry handles transient failures *within* the circuit, and the circuit breaker only records a failure after all retries are exhausted. This prevents a single transient error from counting toward the circuit breaker threshold.
- **Selective fallback**: List endpoints (`findAll`) can gracefully degrade to an empty page — the user sees "no results" rather than an error. Single-resource lookups and write operations cannot be meaningfully degraded, so they throw `ServiceUnavailableException` (503) to signal the client to retry later.
- **Business exception exclusion**: `ResourceNotFoundException` (404) is a legitimate business outcome, not a transient failure. Retrying a "not found" would waste resources and delay the response.

### How (Developer perspective)

**Service annotation pattern** (all 3 services follow this):
```java
// CircuitBreaker is outer (aspect order 1), Retry is inner (aspect order 2)
@CircuitBreaker(name = "userService", fallbackMethod = "findAllFallback")
@Retry(name = "userService")
public Page<UserResponse> findAll(String username, String email, String role, Pageable pageable) {
    return userRepository.findAll(UserSpecification.build(username, email, role), pageable)
            .map(userMapper::toResponse);
}

// Fallback — same parameters + Throwable
private Page<UserResponse> findAllFallback(String username, String email, String role,
                                           Pageable pageable, Throwable t) {
    log.warn("Fallback for findAll triggered: {}", t.getMessage());
    return Page.empty(pageable);
}
```

**Write operations throw ServiceUnavailableException**:
```java
@CircuitBreaker(name = "userService", fallbackMethod = "createFallback")
@Retry(name = "userService")
@Transactional
public UserResponse create(UserRequest request) { ... }

private UserResponse createFallback(UserRequest request, Throwable t) {
    log.warn("Fallback for create triggered: {}", t.getMessage());
    throw new ServiceUnavailableException("User service is temporarily unavailable", t);
}
```

**Retry configuration** — `application.properties`:
```properties
# Decorator ordering: CircuitBreaker(outer=1) → Bulkhead(middle=2147483646) → Retry(inner=2147483647)
resilience4j.circuitbreaker.circuitBreakerAspectOrder=1
resilience4j.retry.retryAspectOrder=2147483647

# Retry defaults
resilience4j.retry.configs.default.max-attempts=3
resilience4j.retry.configs.default.wait-duration=500ms
resilience4j.retry.configs.default.enable-exponential-backoff=true
resilience4j.retry.configs.default.exponential-backoff-multiplier=2
resilience4j.retry.configs.default.retry-exceptions=java.lang.Exception
resilience4j.retry.configs.default.ignore-exceptions=\
    org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException

# Named instances
resilience4j.retry.instances.userService.base-config=default
resilience4j.retry.instances.employeeService.base-config=default
resilience4j.retry.instances.departmentService.base-config=default
```

**ServiceUnavailableException** — `src/main/java/.../exception/ServiceUnavailableException.java`:
```java
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
    public ServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
}
```

**Exception handler** — `GlobalExceptionHandler.java`:
```java
@ExceptionHandler(ServiceUnavailableException.class)
public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
    log.warn("Service unavailable: {}", ex.getMessage());
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
            "Service is temporarily unavailable, please try again later");
}
```

**Execution flow with retry and circuit breaker**:
```
Call → CircuitBreaker (check state)
         │
         ├─ OPEN → fallbackMethod() immediately
         │
         └─ CLOSED/HALF_OPEN → Retry
                                  │
                                  ├─ Attempt 1 → success → return
                                  ├─ Attempt 1 → fail → wait 500ms
                                  ├─ Attempt 2 → success → return
                                  ├─ Attempt 2 → fail → wait 1000ms
                                  ├─ Attempt 3 → success → return
                                  └─ Attempt 3 → fail → exception propagates to CircuitBreaker
                                                          │
                                                          └─ CB records failure → fallbackMethod()
```

### Fallback strategy summary

| Method type | Fallback behavior | Rationale |
|---|---|---|
| `findAll` (list queries) | Return `Page.empty(pageable)` | Graceful degradation — user sees empty results, not an error |
| `findResponseById` | Throw `ServiceUnavailableException` | No meaningful degraded single-resource response |
| `findEntityById` | Throw `ServiceUnavailableException` | Internal method, must propagate failure |
| `create`, `update`, `patch` | Throw `ServiceUnavailableException` | Writes must never silently succeed or return fake data |
| `delete` | Throw `ServiceUnavailableException` | Writes must never silently succeed |

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Retry as inner decorator | Retry as outer (CB records every attempt) | Inner retry means the CB only sees the final outcome. 3 transient failures resolved by retry don't move the CB toward OPEN |
| 3 max attempts | 5+ for more resilience | 3 attempts with exponential backoff (500ms + 1000ms = 1.5s max) keeps total latency under 2 seconds. More attempts increase latency |
| Exponential backoff (multiplier 2) | Fixed delay, jitter | Exponential backoff gives the failing system progressively more time to recover. Jitter would be ideal in high-concurrency scenarios to prevent thundering herd |
| `Page.empty()` for `findAll` | Throw exception, cached stale data | Empty page is a valid API response. The client can display "no results" gracefully. Cached stale data requires a separate cache layer |
| `ServiceUnavailableException` for writes | Return null, return Optional.empty() | Writes *must* report failure explicitly. A silent null return could cause data inconsistency downstream |
| Ignore `ResourceNotFoundException` | Retry everything | A 404 is deterministic — retrying won't find a non-existent resource. Ignoring it avoids wasting 1.5s on guaranteed failures |
| Fallback on `@CircuitBreaker` | Fallback on `@Retry` | CircuitBreaker is the outermost decorator. Its fallback catches both retry-exhaustion and circuit-open scenarios |

### Interview talking points

**Q: Why is the retry the inner decorator and the circuit breaker the outer?**
> The retry handles transient failures (network blip, brief DB hiccup) by retrying up to 3 times. The circuit breaker wraps the retry — it only sees the final outcome. If all 3 retries fail, the circuit breaker records ONE failure. If retry were outer, the CB would see each individual attempt as a separate call, potentially opening the circuit prematurely from what was really a single logical operation.

**Q: What happens when the circuit is OPEN and a request comes in?**
> The circuit breaker rejects the call immediately with `CallNotPermittedException` — the retry is never invoked. The fallback method runs: `findAll` returns `Page.empty()`, others throw `ServiceUnavailableException` which maps to HTTP 503.

**Q: Why not add jitter to the exponential backoff?**
> Jitter (randomized delay) prevents the "thundering herd" problem where many retries from different clients hit the server at the same moment. For a single-instance application with moderate traffic, pure exponential backoff is sufficient. In a distributed system with many instances, you'd add `enable-randomized-wait=true` to spread retries across time.

**Q: Why throw `ServiceUnavailableException` instead of just letting the original exception propagate?**
> Fallback methods provide a controlled error path. The original exception might be a low-level database error, connection timeout, or circuit breaker rejection. Wrapping it in `ServiceUnavailableException` normalizes the error message, includes the original cause for logging, and maps cleanly to HTTP 503 via `GlobalExceptionHandler`.

**Q: Could you use `@Retry` at the class level instead of on every method?**
> Yes, but method-level gives finer control. In the future, you might want different retry configs for reads vs. writes (e.g., more retries for idempotent reads, fewer for non-idempotent writes). Method-level also makes the retry behavior explicitly visible in code review.

---

## 19. Feature 17 — Resilience4j Bulkhead (Concurrency Limiter)

### What was done
Added `@Bulkhead` annotations (semaphore type) to all 21 service methods across 3 services. Each service has a named bulkhead instance with a configured maximum number of concurrent calls. When the limit is reached, additional calls are immediately rejected with `BulkheadFullException`, which maps to HTTP 429 Too Many Requests. The decorator ordering is `CircuitBreaker(order=1) → Bulkhead(order=2147483646, hardcoded) → Retry(order=2147483647)`.

### Why (Architect perspective)
- **Resource isolation**: Without bulkheads, a slow or overloaded service (e.g., a slow department query) can consume all available threads, starving other services. Bulkheads partition thread usage per service, ensuring one degraded service doesn't cascade into a full system outage.
- **Backpressure signal**: HTTP 429 tells clients explicitly that the server is at capacity — enabling client-side throttling, retry-after logic, or load balancer rerouting.
- **Semaphore vs. thread-pool**: Semaphore bulkhead uses a simple counter on the calling thread. Thread-pool bulkhead executes calls on a dedicated thread pool. Semaphore is chosen here because (1) it's lightweight (no thread pool overhead), (2) it preserves `@Transactional` thread-local context (thread-pool would lose it), and (3) it's sufficient for limiting concurrency in a single-instance monolith.
- **Fail-fast (`maxWaitDuration=0`)**: When the bulkhead is full, requests fail immediately rather than queuing. This prevents request pileup and keeps latency predictable. In high-throughput systems, a short wait (e.g., 100ms) might be acceptable to smooth traffic bursts.

### How (Developer perspective)

**Service annotation pattern** (all 3 services follow this):
```java
@CircuitBreaker(name = "userService", fallbackMethod = "findAllFallback")  // order 1
@Bulkhead(name = "userService")                                            // order 2
@Retry(name = "userService")                                               // order 3
public Page<UserResponse> findAll(...) { ... }
```

**Configuration** — `application.properties`:
```properties
# Decorator ordering: CircuitBreaker(outer=1) → Bulkhead(middle=2147483646, hardcoded) → Retry(inner=2147483647)
# Note: Resilience4j 2.3.0 hardcodes bulkhead aspect order at Ordered.LOWEST_PRECEDENCE - 1 (2147483646)
# with no setter, so we set retry order to LOWEST_PRECEDENCE (2147483647) to ensure it's innermost
resilience4j.circuitbreaker.circuitBreakerAspectOrder=1
resilience4j.retry.retryAspectOrder=2147483647

# Bulkhead defaults (semaphore type)
resilience4j.bulkhead.configs.default.max-concurrent-calls=10
resilience4j.bulkhead.configs.default.max-wait-duration=0ms

# Named instances
resilience4j.bulkhead.instances.userService.base-config=default
resilience4j.bulkhead.instances.employeeService.base-config=default
resilience4j.bulkhead.instances.departmentService.base-config=default
resilience4j.bulkhead.instances.departmentService.max-concurrent-calls=5
```

**Exception handling** — `GlobalExceptionHandler.java`:
```java
@ExceptionHandler(BulkheadFullException.class)
public ResponseEntity<Map<String, Object>> handleBulkheadFull(BulkheadFullException ex) {
    log.warn("Bulkhead full: {}", ex.getMessage());
    return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
            "Too many concurrent requests, please try again later");
}
```

**Execution flow with all three decorators**:
```
Call → CircuitBreaker (check state)
         │
         ├─ OPEN → fallbackMethod() immediately
         │
         └─ CLOSED/HALF_OPEN → Bulkhead (check permits)
                                  │
                                  ├─ FULL → BulkheadFullException → CB fallback → 429
                                  │
                                  └─ PERMIT acquired → Retry
                                                        │
                                                        ├─ Attempt 1 → success → release permit → return
                                                        ├─ Attempt 1 → fail → wait 500ms (holding permit)
                                                        ├─ Attempt 2 → success → release permit → return
                                                        ├─ Attempt 2 → fail → wait 1000ms (holding permit)
                                                        ├─ Attempt 3 → success → release permit → return
                                                        └─ Attempt 3 → fail → release permit → exception → CB records failure → fallback
```

### Concurrency limits per service

| Instance | maxConcurrentCalls | maxWaitDuration | Rationale |
|---|---|---|---|
| `userService` | 10 | 0ms (fail-fast) | Primary entity, moderate traffic |
| `employeeService` | 10 | 0ms (fail-fast) | Primary entity, moderate traffic |
| `departmentService` | 5 | 0ms (fail-fast) | Reference data, lower traffic, fewer expected concurrent lookups |

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Semaphore bulkhead | Thread-pool bulkhead | Semaphore is lightweight and preserves `@Transactional` thread-local context. Thread-pool isolates execution but breaks Spring's thread-local transaction binding |
| `maxWaitDuration=0ms` | Short wait (100-500ms) | Fail-fast prevents request pileup under load. A wait duration would smooth bursts but increases tail latency and holds threads longer |
| Bulkhead between CB and Retry | Bulkhead outside CB, or inside Retry | Between CB and Retry: the CB can reject without acquiring a permit (efficient), and each retry holds the same permit (preventing permit exhaustion from retries) |
| 10 concurrent calls for user/employee | Higher (50+) or lower (3-5) | 10 is conservative — prevents thread exhaustion while allowing reasonable concurrency. Tune based on load testing. In production, this should be derived from HikariCP pool size and expected concurrency |
| 5 for department | Same as others (10) | Department is a reference entity with fewer concurrent mutations. Lower limit reserves capacity for user/employee operations |
| Per-service instances | Global bulkhead | Per-service isolation: a burst of department queries doesn't block user operations. Global would be simpler but loses fault isolation |

### Interview talking points

**Q: What's the difference between a semaphore bulkhead and a thread-pool bulkhead?**
> **Semaphore**: Uses an `AtomicInteger` counter on the calling thread. When a call enters, the counter increments; when it exits, it decrements. If the counter equals `maxConcurrentCalls`, new calls are rejected. The call executes on the original thread.
> **Thread-pool**: Submits the call to a dedicated `ThreadPoolExecutor` with a bounded queue. If the pool and queue are full, new calls are rejected. The call executes on a pool thread, not the calling thread.
> Semaphore is preferred for synchronous/blocking code (like JPA + `@Transactional`) because it preserves thread-local state. Thread-pool is better for isolating truly independent workloads (e.g., calling external HTTP APIs) where thread-local context doesn't matter.

**Q: Why does the bulkhead sit between the circuit breaker and retry?**
> Circuit breaker (outermost) can reject calls without consuming a bulkhead permit — efficient when the circuit is OPEN. Retry (innermost) retries *within* the same bulkhead permit — a 3-attempt retry uses 1 permit, not 3. If retry were outside the bulkhead, each retry attempt would acquire a separate permit, potentially exhausting the bulkhead on retries alone.

**Q: What happens when a retry holds a bulkhead permit during backoff?**
> The permit is held during the entire retry sequence (including wait durations). A 3-attempt retry with 500ms + 1000ms backoff holds the permit for up to ~1.5 seconds. This reduces effective throughput during retries but prevents additional load on an already-struggling system.

**Q: How would you size the `maxConcurrentCalls` in production?**
> Start with `HikariCP maxPoolSize` as an upper bound — there's no point allowing more concurrent service calls than available database connections. Then factor in: (1) number of service methods sharing the pool, (2) expected request latency, (3) target throughput. Load test with realistic traffic and adjust. Monitor `resilience4j.bulkhead.available.concurrent.calls` via Micrometer/Prometheus.

**Q: Why 429 Too Many Requests instead of 503 Service Unavailable?**
> 429 signals that the *client* is sending too many requests — it's a rate/concurrency limit, not a server failure. 503 means the server is broken or overloaded. Clients handle them differently: 429 suggests backing off and retrying, 503 suggests the service may be down. Some API gateways and load balancers also treat 429 and 503 differently for routing decisions.

---

## 20. Feature 18 — POST Idempotency

### What was done
Added a servlet filter (`IdempotencyFilter`) that makes POST endpoints idempotent via an optional `Idempotency-Key` HTTP header. When a client retries a POST with the same key, the filter replays the original response instead of creating a duplicate resource. Uses H2-backed JPA storage (no Redis dependency) with a scheduled cleanup task.

### Why (Architect perspective)
- **Safe retries**: Network failures, timeouts, and load balancer retries can cause a single user action to hit the server multiple times. Without idempotency, each retry creates a duplicate resource. The `Idempotency-Key` header lets clients safely retry POSTs with deterministic outcomes.
- **Opt-in design**: The header is optional — existing clients work unchanged. Only clients that send the header get idempotency protection. This follows the Stripe API pattern.
- **POST only**: PUT, PATCH, and DELETE are already idempotent by HTTP specification. POST is the only non-idempotent method that creates resources.
- **Filter vs. interceptor**: A servlet filter runs before Spring MVC dispatching, ensuring idempotency checks happen at the earliest possible point — before validation, service logic, or database writes.

### How (Developer perspective)

**`IdempotencyFilter.java`** — `src/main/java/.../config/IdempotencyFilter.java`:
```java
@Component
@Order(10)
public class IdempotencyFilter implements Filter {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Only intercept POST requests with Idempotency-Key header
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) { chain.doFilter(...); return; }
        String key = httpRequest.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) { chain.doFilter(...); return; }

        // Check for existing record
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            if (record.getStatusCode() != null) replayResponse(response, record);  // Completed → replay
            else writeConflictResponse(response);                                   // In-progress → 409
            return;
        }

        // Insert placeholder (statusCode=null means in-progress)
        try { repository.save(new IdempotencyRecord(key, path, now)); }
        catch (DataIntegrityViolationException e) { writeConflictResponse(response); return; }

        // Execute request with response caching
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
            if (wrapper.getStatus() >= 500) repository.delete(record);     // 5xx → allow retry
            else { record.setStatusCode(status); record.setResponseBody(body); repository.save(record); }
        } catch (Exception e) { repository.delete(record); throw e; }      // Exception → allow retry
        wrapper.copyBodyToResponse();
    }
}
```

**`IdempotencyRecord.java`** — `src/main/java/.../model/IdempotencyRecord.java`:
```java
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_path", length = 512)
    private String requestPath;

    @Column(name = "status_code")
    private Integer statusCode;          // null = in-progress, non-null = completed

    @Lob @Column(name = "response_body")
    private String responseBody;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

**`IdempotencyCleanupScheduler.java`** — `src/main/java/.../config/IdempotencyCleanupScheduler.java`:
```java
@Component
public class IdempotencyCleanupScheduler {
    @Scheduled(fixedRate = 3600000)  // Every hour
    @Transactional
    public void cleanupExpiredKeys() {
        idempotencyRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(24));
    }
}
```

**Filter ordering**:

| Order | Filter | Purpose |
|-------|--------|---------|
| 0 | `SecurityHeadersFilter` | Security headers on ALL responses (including replayed/409) |
| 5 | `RequestLoggingFilter` | Log all requests including replays; sets MDC correlationId |
| 10 | `IdempotencyFilter` | Short-circuited responses still get headers + logging |

**Request flow**:
```
POST with Idempotency-Key header:
1. Filter checks DB for existing key
   a. Found + completed (statusCode != null) → replay stored response
   b. Found + in-progress (statusCode == null) → 409 Conflict
   c. Not found → INSERT placeholder (statusCode=null), proceed
2. Wrap response with ContentCachingResponseWrapper
3. Execute request via chain.doFilter()
4. Capture response → UPDATE record with statusCode, body, contentType
5. On 5xx failure → DELETE record (allow client retry)
6. On exception → DELETE record, re-throw
```

**Liquibase migration** — `004-add-idempotency-keys.yaml`:
```yaml
- createTable:
    tableName: idempotency_keys
    columns:
      - column: { name: id, type: BIGINT, autoIncrement: true, primaryKey: true }
      - column: { name: idempotency_key, type: VARCHAR(255), unique: true, nullable: false }
      - column: { name: request_path, type: VARCHAR(512) }
      - column: { name: status_code, type: INT }               # nullable (null = in-progress)
      - column: { name: response_body, type: CLOB }
      - column: { name: content_type, type: VARCHAR(255) }
      - column: { name: created_at, type: TIMESTAMP, nullable: false }
- createIndex: { tableName: idempotency_keys, indexName: idx_idempotency_keys_created_at, columns: [created_at] }
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| H2/JPA storage | Redis, in-memory `ConcurrentHashMap` | H2 is already available (no new dependency). Survives restarts. Redis would be better for distributed deployments but adds infrastructure complexity |
| Optional `Idempotency-Key` header | Mandatory header on all POSTs | Opt-in avoids breaking existing clients. Stripe, PayPal, and other payment APIs use this pattern |
| UNIQUE constraint for concurrency | `SELECT FOR UPDATE`, distributed lock | UNIQUE constraint is the simplest race-condition handler — the DB does the locking. `DataIntegrityViolationException` on duplicate insert is caught and returns 409 |
| Delete record on 5xx | Keep record, return same 5xx on replay | 5xx errors are transient (DB down, timeout). Deleting the record allows the client to retry the same key and potentially succeed. Keeping it would permanently block retries |
| `Integer` (boxed) for statusCode | `int` (primitive) with sentinel value (-1) | Boxed `Integer` allows `null` to distinguish "in-progress" from "completed". A sentinel value is error-prone and requires documentation |
| 24-hour TTL with hourly cleanup | Shorter TTL, event-driven cleanup | 24 hours is long enough for clients to retry within a reasonable window. Hourly cleanup is simple and predictable |
| `ContentCachingResponseWrapper` | Custom response wrapper, `TeeOutputStream` | Spring's built-in wrapper is well-tested, handles edge cases (character encoding, content length), and requires no custom code |
| No `@Cacheable`/`@Cache` on entity | L2 cache for fast lookups | Idempotency records are write-heavy, short-lived, and rarely read more than once. Caching would waste memory on entries that are never re-read |

### Interview talking points

**Q: Why is idempotency needed if the database has unique constraints?**
> Unique constraints prevent *data* duplication (e.g., two users with the same email), but they don't prevent *semantic* duplication. For example, creating two orders with different IDs but the same items because the client retried. Idempotency ensures the entire operation (including side effects like sending emails, charging payments) happens exactly once.

**Q: How do you handle concurrent requests with the same idempotency key?**
> The UNIQUE constraint on `idempotency_key` handles the race condition at the database level. If two threads try to INSERT the same key simultaneously, one succeeds and the other gets a `DataIntegrityViolationException`, which the filter catches and returns 409 Conflict. The successful thread's placeholder record (statusCode=null) signals "in-progress" to any subsequent lookups.

**Q: Why delete the record on 5xx instead of storing the error?**
> 5xx errors are transient (database timeout, out of memory, network partition). If we stored the 5xx response, retrying with the same key would replay the error forever — the client could never recover. Deleting the record allows the client to retry the same key, and the next attempt may succeed if the transient issue has resolved.

**Q: What about idempotency for non-POST methods?**
> PUT is idempotent by definition (same PUT = same result). PATCH is technically not idempotent in the general case, but in this project's implementation (field-level partial updates), it is effectively idempotent. DELETE is idempotent (deleting twice = same state). Only POST creates new resources, so only POST needs explicit idempotency protection.

**Q: How would you scale this to a distributed system?**
> Replace H2 storage with Redis (`SET NX EX` for atomic insert-if-not-exists with TTL) or a shared database. Redis is ideal because `SET key value NX EX 86400` atomically creates a key only if it doesn't exist, with a 24-hour expiry — no separate cleanup task needed. For very high throughput, consider a Bloom filter for fast "definitely not seen" checks before hitting the database.

**Q: Why a filter instead of a Spring interceptor or AOP aspect?**
> A servlet filter runs before Spring MVC dispatching, at the earliest point in the request lifecycle. An interceptor would run after `DispatcherServlet` has matched a handler, wasting work on duplicate requests. An AOP aspect would require annotation on each controller method. The filter is global, transparent, and catches all POST endpoints without code changes.

---

## 21. Feature 19 — Graceful Shutdown

### What was done
Configured graceful shutdown via properties only — no new Java files. On SIGTERM, the embedded Tomcat server stops accepting new connections while in-flight requests complete, the `TaskScheduler` waits for running `@Scheduled` tasks to finish, and Spring context destruction closes all resources (HikariCP, EhCache, Redis, Resilience4j) in reverse initialization order.

### Why (Architect perspective)
- **Zero dropped requests**: Without graceful shutdown, SIGTERM kills the JVM immediately — in-flight HTTP requests are dropped mid-response, active transactions may be interrupted, and the `@Scheduled` cleanup task could be killed during a database operation. Graceful shutdown ensures all in-progress work completes before exit.
- **Deployment safety**: Container orchestrators (Kubernetes, ECS) send SIGTERM before SIGKILL. The 30-second timeout gives the application enough time to drain requests while staying within the typical 30-60 second termination grace period.
- **Data integrity**: Active `@Transactional` methods complete before HikariCP closes the connection pool, preventing partial writes or corrupted state. The idempotency cleanup scheduler finishes its current `DELETE` batch before the `TaskScheduler` shuts down.
- **Properties-only approach**: Spring Boot's `WebServerGracefulShutdownLifecycle` and `TaskSchedulingAutoConfiguration` handle everything. No custom `@PreDestroy`, `DisposableBean`, or shutdown hooks needed — all infrastructure components implement lifecycle interfaces that Spring auto-manages.

### How (Developer perspective)

**`application.properties`** (shared across all profiles):
```properties
# Graceful shutdown
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

**`application-dev.properties`** and **`application-prod.properties`**:
```properties
# Task scheduling shutdown
spring.task.scheduling.shutdown.await-termination=true
spring.task.scheduling.shutdown.await-termination-period=30s
```

**What each property does:**

| Property | Effect |
|---|---|
| `server.shutdown=graceful` | Tomcat stops accepting new connections; in-flight requests complete (new requests get HTTP 503) |
| `spring.lifecycle.timeout-per-shutdown-phase=30s` | Maximum time to wait for in-flight requests before forcing shutdown |
| `spring.task.scheduling.shutdown.await-termination=true` | `TaskScheduler` waits for currently running `@Scheduled` tasks to finish instead of interrupting them |
| `spring.task.scheduling.shutdown.await-termination-period=30s` | Maximum time to wait for scheduled tasks to complete |

**Shutdown sequence on SIGTERM:**
```
1. JVM receives SIGTERM
2. Spring Boot shutdown hook fires
3. Tomcat stops accepting new connections (HTTP 503 for new requests)
4. In-flight HTTP requests complete (up to 30s timeout)
5. TaskScheduler awaits running @Scheduled tasks (up to 30s)
6. Spring context destruction begins (reverse initialization order):
   a. Resilience4j decorators release
   b. Redis LettuceConnectionFactory closes (implements DisposableBean)
   c. EhCache CacheManager closes (implements Closeable)
   d. Hibernate SessionFactory closes
   e. HikariCP pool closes (waits for active connections to return)
   f. Liquibase cleanup
7. JVM exits
```

**Why no custom Java code is needed:**

| Component | Lifecycle interface | Shutdown behavior |
|---|---|---|
| Embedded Tomcat | `WebServerGracefulShutdownLifecycle` | Stops accepting connections, drains in-flight requests |
| HikariCP | `Closeable` | `close()` waits for active connections to return to pool |
| EhCache `CacheManager` | `Closeable` | JCache provider flushes and closes cache regions |
| Redis `LettuceConnectionFactory` | `DisposableBean` | `destroy()` closes connections |
| `@Scheduled` executor | `TaskSchedulingAutoConfiguration` | Respects `await-termination` properties |
| Resilience4j | Spring bean lifecycle | Circuit breakers, bulkheads, retries release resources |

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| 30s timeout | 5s (fast) or 60s (generous) | 30s matches Kubernetes default `terminationGracePeriodSeconds`. Long enough for most requests; short enough that deploys aren't slow |
| Properties-only (no Java) | Custom `@PreDestroy` methods, `SmartLifecycle` beans | Spring Boot handles all component lifecycles natively. Custom code adds maintenance burden and can interfere with Spring's shutdown ordering |
| `server.shutdown=graceful` | Default `immediate` | Immediate shutdown drops in-flight requests. Acceptable in dev but unacceptable in production where users experience 502/504 errors during deployments |
| `await-termination` in profile files | In shared `application.properties` | Task scheduling shutdown is relevant in all profiles, but keeping it in profile files follows the existing pattern where profile-specific configs are separated |
| Same 30s for both phases | Different timeouts | Simplicity. The HTTP drain and task completion happen in parallel during the same shutdown window. Different timeouts add configuration complexity with minimal benefit |

### Interview talking points

**Q: What happens to new requests during graceful shutdown?**
> Tomcat stops accepting new TCP connections immediately. Requests that arrive during the drain period receive HTTP 503 Service Unavailable. Load balancers should detect this and route traffic to healthy instances. In Kubernetes, the pod is removed from the Service endpoints before SIGTERM is sent (readiness probe fails), so traffic stops arriving before shutdown begins.

**Q: What if an in-flight request takes longer than 30 seconds?**
> After the timeout expires, Spring forcefully terminates remaining requests. The response is dropped (client sees a connection reset). To handle this: (1) ensure API operations complete within a reasonable time, (2) long-running operations should use async processing (return 202 Accepted, process in background), (3) increase the timeout if justified, but keep it under the orchestrator's `terminationGracePeriodSeconds`.

**Q: How does `@Transactional` interact with shutdown?**
> An in-flight request's `@Transactional` method completes normally during the drain period — the transaction commits as usual. If the timeout expires mid-transaction, the thread is interrupted, the transaction rolls back (Hibernate/JDBC detects the interrupt), and no partial writes occur. This is safe because transactions are atomic.

**Q: Why not add a custom `@PreDestroy` method for cleanup?**
> All infrastructure components (HikariCP, EhCache, Redis, Hibernate) already implement Spring lifecycle interfaces (`Closeable`, `DisposableBean`, `SmartLifecycle`). Spring destroys them in reverse initialization order during context shutdown. Adding custom `@PreDestroy` methods risks executing cleanup out of order or duplicating work that Spring already handles.

**Q: How would you verify graceful shutdown is working?**
> (1) Start the app, send a long-running request (e.g., with a `Thread.sleep` in the service), send SIGTERM, and verify the response completes. (2) Check logs for "Commencing graceful shutdown" and "Graceful shutdown complete" messages from Spring Boot. (3) In integration tests, use `SpringApplication.exit()` and assert that pending requests complete. (4) Monitor with `curl` during a rolling deployment.

**Q: What about WebSocket or SSE connections?**
> Graceful shutdown drains HTTP request-response connections. Long-lived connections (WebSocket, SSE) are also subject to the timeout. For WebSocket, you'd send a close frame before shutdown. For SSE, clients should handle connection drops and reconnect. This project uses only request-response HTTP, so no special handling is needed.

---

## 22. Feature 20 — Async Processing

### What was done
Added `@Async` task execution to offload fire-and-forget work (notifications, audit logs, webhooks) from the HTTP request thread. Created `AsyncConfig` with `@EnableAsync`, a custom `ThreadPoolTaskExecutor` with MDC-propagating `TaskDecorator`, and an `AsyncNotificationService` with `@Async` void methods called from all three services after write operations.

### Why (Architect perspective)
- **Reduced response latency**: Write operations (create, update, delete) often trigger side effects — email notifications, audit log writes, webhook calls. These don't need to complete before the HTTP response is sent. Running them asynchronously removes them from the critical path.
- **Thread isolation**: Async tasks run on a dedicated thread pool, not Tomcat's request threads. If a notification endpoint is slow, it doesn't hold up HTTP request processing.
- **MDC correlation propagation**: Without a `TaskDecorator`, async threads lose the `correlationId` set by `RequestLoggingFilter`. The decorator captures MDC context at submission time and applies it on the async thread, so log entries from async tasks are traceable back to the originating HTTP request.
- **Graceful shutdown integration**: `await-termination=true` ensures in-flight async tasks complete before the JVM exits, preventing lost notifications during deployments.

### How (Developer perspective)

**`AsyncConfig.java`** — `src/main/java/.../config/AsyncConfig.java`:
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        TaskDecorator mdcDecorator = runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                if (context != null) { MDC.setContextMap(context); }
                try { runnable.run(); }
                finally { MDC.clear(); }
            };
        };
        ThreadPoolTaskExecutor executor = builder.build();
        executor.setTaskDecorator(mdcDecorator);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("Async exception in {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
```

**`AsyncNotificationService.java`** — `src/main/java/.../service/AsyncNotificationService.java`:
```java
@Service
public class AsyncNotificationService {

    @Async
    public void notifyResourceCreated(String entityType, Long id) {
        log.info("Dispatching creation notification for {} id={}", entityType, id);
    }

    @Async
    public void notifyResourceUpdated(String entityType, Long id) {
        log.info("Dispatching update notification for {} id={}", entityType, id);
    }

    @Async
    public void notifyResourceDeleted(String entityType, Long id) {
        log.info("Dispatching deletion notification for {} id={}", entityType, id);
    }
}
```

**Service integration** (same pattern in all three services):
```java
// In create():
asyncNotificationService.notifyResourceCreated("User", response.getId());
// In update() and patch():
asyncNotificationService.notifyResourceUpdated("User", id);
// In delete():
asyncNotificationService.notifyResourceDeleted("User", id);
```

**Properties** (`application.properties`):
```properties
spring.task.execution.thread-name-prefix=async-
```

**Profile-specific pool configuration:**

| Property | Dev | Prod |
|---|---|---|
| `pool.core-size` | 2 | 4 |
| `pool.max-size` | 4 | 8 |
| `pool.queue-capacity` | 50 | 100 |
| `shutdown.await-termination` | true | true |
| `shutdown.await-termination-period` | 30s | 30s |

**How `TaskDecorator` preserves MDC:**
```
1. HTTP thread calls asyncNotificationService.notifyResourceCreated()
2. TaskDecorator captures MDC.getCopyOfContextMap() (contains correlationId)
3. Task is submitted to ThreadPoolTaskExecutor queue
4. Async thread picks up task, sets MDC from captured map
5. log.info() includes correlationId in log output
6. finally block clears MDC on async thread
```

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Direct injection | `ApplicationEventPublisher` + `@Async @EventListener` | Simpler, fewer files, explicit call chain. Events add indirection and are harder to trace in a debugger |
| `void` return (fire-and-forget) | `CompletableFuture<Void>` | Notifications don't need to report completion. `void` is simpler and avoids callers accidentally blocking on `.get()` |
| `TaskDecorator` for MDC | `InheritableThreadLocal` | `InheritableThreadLocal` only works for child threads, not thread pool reuse. `TaskDecorator` works correctly with pooled threads |
| `ThreadPoolTaskExecutorBuilder` | Manual `new ThreadPoolTaskExecutor()` | Builder auto-reads `spring.task.execution.*` properties. One line instead of five `set*()` calls |
| Profile-specific pool sizes | Same pool for all profiles | Dev doesn't need 8 threads. Smaller pool matches the pattern for HikariCP sizing |

### Interview talking points

**Q: Why use `@Async` instead of `CompletableFuture.supplyAsync()`?**
> `@Async` integrates with Spring's managed `TaskExecutor`, which respects graceful shutdown, pool sizing from properties, and bean lifecycle. `CompletableFuture.supplyAsync()` uses `ForkJoinPool.commonPool()` by default, which has no shutdown integration, no MDC propagation, and pool size isn't configurable via properties.

**Q: How does `@Async` work under the hood?**
> Spring creates a proxy around the `AsyncNotificationService` bean. When a caller invokes an `@Async` method, the proxy wraps the method call in a `Runnable`, applies the `TaskDecorator`, and submits it to the `ThreadPoolTaskExecutor`. The caller returns immediately (void) or gets a `Future`. This is why `@Async` methods must be called from outside the class — self-invocation bypasses the proxy.

**Q: What happens if the async thread pool is exhausted?**
> With `queue-capacity=50` (dev) or `100` (prod), tasks queue up when all core threads are busy. When the queue is full and all max threads are active, the `RejectedExecutionHandler` fires (default: `AbortPolicy` throws `RejectedExecutionException`). The `AsyncUncaughtExceptionHandler` logs the error. The HTTP response is unaffected because the caller already returned.

**Q: How do you ensure MDC context isn't leaked between requests on pooled threads?**
> The `TaskDecorator` wraps each task in a try/finally that calls `MDC.clear()` after execution. This prevents a previous request's `correlationId` from leaking into the next task on the same thread. The decorator also handles the case where `getCopyOfContextMap()` returns null (no MDC set).

**Q: Why not use `@TransactionalEventListener` for post-commit notifications?**
> `@TransactionalEventListener(phase = AFTER_COMMIT)` would ensure notifications fire only after the transaction commits, avoiding "notification sent but data rolled back" scenarios. However, it requires `ApplicationEventPublisher`, custom event classes, and `@EventListener` methods — more infrastructure for a notification that's purely informational. For critical notifications (e.g., payment confirmations), `@TransactionalEventListener` would be the right choice.

---

## 23. Feature 21 — Bulk Operations

### What was done
Added dedicated bulk create, update, and delete endpoints (`POST/PUT/DELETE /api/v1/{resource}/bulk`) for all three resources (Users, Employees, Departments). Created a generic `BulkUpdateRequest<T>` DTO for update payloads. Each bulk operation runs in a single `@Transactional` with `saveAll()` for batch SQL, and fires per-item async notifications consistent with single-item behavior.

### Why (Architect perspective)
- **Network overhead reduction**: Clients creating 50 users previously needed 50 HTTP round-trips, 50 transactions, and 50 Resilience4j decorator invocations. A single bulk request reduces this to 1 round-trip, 1 transaction, and 1 decorator pass.
- **Atomic batch semantics**: `@Transactional` wraps the entire batch — if item 47 of 50 fails validation or causes a constraint violation, all 50 are rolled back. This prevents partial-state scenarios that are difficult for clients to recover from.
- **Batch SQL efficiency**: `saveAll()` allows Hibernate to batch INSERT/UPDATE statements (controlled by `hibernate.jdbc.batch_size`), reducing database round-trips within the transaction.
- **Bounded input**: `@Size(max=100)` prevents unbounded batch sizes that could overwhelm the database connection pool, thread pool, or cause transaction timeouts.

### How (Developer perspective)

**`BulkUpdateRequest.java`** — `src/main/java/.../dto/BulkUpdateRequest.java`:
```java
public class BulkUpdateRequest<T> {
    @NotNull(message = "ID is required")
    private Long id;

    @NotNull(message = "Data is required")
    @Valid
    private T data;
}
```

**Service methods** (same pattern in all three services, UserService shown):
```java
@CircuitBreaker(name = "userService", fallbackMethod = "bulkCreateFallback")
@Bulkhead(name = "userService")
@Retry(name = "userService")
@Transactional
@CacheEvict(value = CACHE_NAME, allEntries = true)  // UserService only
public List<UserResponse> bulkCreate(List<UserRequest> requests) {
    List<User> entities = new ArrayList<>(requests.size());
    for (UserRequest request : requests) {
        entities.add(userMapper.toEntity(request));
    }
    List<User> saved = userRepository.saveAll(entities);
    List<UserResponse> responses = new ArrayList<>(saved.size());
    for (User user : saved) {
        responses.add(userMapper.toResponse(user));
        asyncNotificationService.notifyResourceCreated("User", user.getId());
    }
    return responses;
}
```

**Controller endpoints** (same pattern in all three controllers):
```java
@PostMapping("/bulk")
public ResponseEntity<List<EntityModel<UserResponse>>> bulkCreate(
        @RequestBody @NotEmpty @Size(max = 100) List<@Valid UserRequest> requests) {
    List<UserResponse> responses = userService.bulkCreate(requests);
    List<EntityModel<UserResponse>> models = responses.stream()
            .map(this::toEntityModel).toList();
    return ResponseEntity.status(201).body(models);
}
```

**Key differences between services:**

| Service | bulkCreate extra step | bulkUpdate extra step | Cache annotation |
|---|---|---|---|
| `UserService` | — | — | `@CacheEvict(allEntries=true)` |
| `EmployeeService` | `resolveDepartment()` | `resolveDepartment()` | None |
| `DepartmentService` | — | — | None |

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Dedicated `bulkCreate`/`bulkUpdate`/`bulkDelete` methods | Delegate to existing single-item `create()`/`update()`/`delete()` | Self-invocation within the same class bypasses Spring proxies — Resilience4j `@CircuitBreaker`, `@Bulkhead`, `@Retry` annotations would not fire. Dedicated methods also enable `saveAll()` for batch SQL |
| `findEntityById()` per item | `findAllById()` for batch lookup | `findAllById()` silently skips missing IDs (returns fewer results). Per-item lookup preserves the 404 contract with a specific "not found with id X" message, and rolls back the entire batch |
| `@CacheEvict(allEntries=true)` on UserService | `@CachePut` per item | `@CachePut` can't handle list return types with multiple cache keys. Full eviction is simple and correct — subsequent reads re-populate individual entries |
| `@Size(max=100)` | No limit, or configurable limit | 100 is a reasonable upper bound that prevents abuse while allowing meaningful batches. A configurable property adds complexity for minimal benefit |
| `@NotEmpty` + `List<@Valid T>` | Manual validation in service | Container-level validation produces indexed error paths like `bulkCreate.requests[1].email` automatically, matching the existing validation pattern |
| Single `@Transactional` (all-or-nothing) | Per-item transactions with partial success reporting | All-or-nothing is simpler for clients (either everything succeeded or nothing did). Partial success requires complex error response schemas and client-side retry logic for failed items |

### Interview talking points

**Q: Why not reuse the existing single-item service methods in a loop?**
> Calling `this.create()` from `bulkCreate()` within the same class is self-invocation — it bypasses the Spring proxy, so `@CircuitBreaker`, `@Bulkhead`, `@Retry`, `@Transactional`, and `@CacheEvict` annotations on `create()` won't fire. You'd need to inject the service into itself or use `AopContext.currentProxy()`, both of which are anti-patterns. Dedicated methods with `saveAll()` are cleaner and more efficient.

**Q: What happens if one item in the batch has a duplicate email?**
> The entire batch rolls back. `saveAll()` runs within a single `@Transactional`, so the `DataIntegrityViolationException` from the unique constraint violation propagates up, Spring rolls back the transaction, and `GlobalExceptionHandler` returns a 409 Conflict. No items are persisted.

**Q: Why `findEntityById()` per item instead of `findAllById()`?**
> `findAllById()` is a Spring Data JPA method that returns only the entities it finds — if you pass `[1, 2, 99]` and ID 99 doesn't exist, it silently returns `[entity1, entity2]`. This violates the 404 contract. Per-item `findEntityById()` throws `ResourceNotFoundException` with "not found with id 99", rolling back the batch and returning a clear error.

**Q: How does this interact with the IdempotencyFilter?**
> Bulk POST endpoints inherit idempotency protection for free. The `IdempotencyFilter` intercepts all POST requests with an `Idempotency-Key` header, stores/replays the response. A bulk create with the same idempotency key replays the original response, preventing duplicate batch creation on retries.

**Q: How would you handle partial success instead of all-or-nothing?**
> You'd remove `@Transactional` from the bulk method, wrap each item in a try-catch, collect results and errors in separate lists, and return a composite response (e.g., `{succeeded: [...], failed: [{index: 3, error: "..."}]}`). This is more complex but useful for large batches where a single bad item shouldn't invalidate the entire request. The trade-off is that clients must handle partial failure and potentially retry individual items.

---

## 24. Feature 22 — Webhook Support

### What was done
Added webhook support so external systems can subscribe to entity lifecycle events (CREATED, UPDATED, DELETED) and receive real-time HTTP POST notifications. Includes full CRUD for webhook registrations, HMAC-SHA256 payload signing, delivery logging, scheduled retry of failed deliveries, and scheduled cleanup of old logs.

### Why (Architect perspective)
- **Event-driven integration**: Webhooks are the standard pattern for notifying external systems of changes without polling. Every major SaaS platform (GitHub, Stripe, Slack) uses webhooks.
- **Decoupling**: External systems don't need to know about internal service architecture — they just register a URL and receive events.
- **Auditability**: Delivery logs provide a complete trail of what was sent, when, and whether it succeeded.
- **Security**: HMAC-SHA256 signatures allow consumers to verify that payloads are authentic and untampered.
- **Reliability**: Automatic retry of failed deliveries (max 3 attempts) handles transient network failures without manual intervention.

### How (Developer perspective)

**New files created:**

| File | Purpose |
|---|---|
| `005-add-webhook-tables.yaml` | Liquibase migration — `webhook_registrations` and `webhook_delivery_logs` tables with indexes and CASCADE FK |
| `WebhookRegistration.java` | JPA entity — id, url(2048), entityType(50), eventType(50), secret(255), active, createdAt |
| `WebhookDeliveryLog.java` | JPA entity — id, webhookId(Long), entityType, eventType, entityId, requestUrl, requestBody(@Lob), responseStatus, responseBody(1024), success, attemptCount, createdAt |
| `WebhookRegistrationRepository.java` | JpaRepository with custom JPQL `findActiveByEntityTypeAndEventType` supporting `"*"` wildcard |
| `WebhookDeliveryLogRepository.java` | JpaRepository with `findBySuccessFalseAndAttemptCountLessThan`, `deleteByCreatedAtBefore`, `findByWebhookId` |
| `WebhookRegistrationRequest.java` | Validated DTO — `@NotBlank` url with `@Pattern(^https?://.*)`, entityType, eventType, secret `@Size(min=16)` |
| `WebhookRegistrationPatchRequest.java` | Patch DTO — all fields nullable, `active` as `Boolean` wrapper |
| `WebhookRegistrationResponse.java` | Response DTO — secret intentionally omitted (write-only) |
| `WebhookDeliveryLogResponse.java` | Response DTO — requestBody omitted (may contain sensitive data) |
| `WebhookRegistrationMapper.java` | `@Component` mapper with `toEntity`, `toResponse`, `updateEntity`, `patchEntity`, `toDeliveryLogResponse` |
| `WebhookSignatureUtils.java` | Static utility — `computeSignature(payload, secret)` → `"sha256=<hex>"` using HmacSHA256 + HexFormat |
| `RestClientConfig.java` | `@Configuration` — `RestClient` bean with 5s connect / 10s read timeout via `SimpleClientHttpRequestFactory` |
| `WebhookService.java` | CRUD service with `@CircuitBreaker`/`@Bulkhead`/`@Retry` + fallback methods |
| `WebhookRetryScheduler.java` | `@Scheduled(fixedRate=60000)` — retries failed deliveries with attemptCount < 3 |
| `WebhookCleanupScheduler.java` | `@Scheduled(fixedRate=3600000)` — deletes delivery logs older than 7 days |
| `WebhookController.java` | `@RestController` at `/api/v1/webhooks` — full CRUD + `GET /{id}/deliveries` with HATEOAS and OpenAPI |

**Modified `AsyncNotificationService`** — added constructor injection for `WebhookRegistrationRepository`, `WebhookDeliveryLogRepository`, `RestClient`, and `ObjectMapper`. Each `@Async` notify method now calls `dispatchWebhooks()` which queries matching webhooks, builds a JSON payload, and delivers via HTTP POST with signature headers. No changes to callers (UserService, EmployeeService, DepartmentService).

**Webhook payload format:**
```json
{
  "entityType": "User",
  "eventType": "CREATED",
  "entityId": 42,
  "timestamp": "2026-02-16T10:30:00.000"
}
```

**HTTP headers sent with each webhook POST:**
- `Content-Type: application/json`
- `X-Webhook-Signature: sha256=<hex>` (HMAC-SHA256 of payload with per-webhook secret)
- `X-Webhook-Event: CREATED`
- `X-Webhook-Entity-Type: User`

### Key decisions & trade-offs

| Decision | Alternative | Why this approach |
|---|---|---|
| Modify `AsyncNotificationService` (not a new service) | Create a separate `WebhookDispatchService` | The existing `@Async` methods already run on the right thread pool with MDC propagation. Adding dispatch logic here avoids a new service and keeps the same fire-and-forget contract. Callers don't change |
| `RestClient` over `WebClient` | `WebClient` (reactive) | Project has only `spring-boot-starter-web` (no WebFlux). `RestClient` is Spring 6.1+ built-in, synchronous, fluent, and already on the classpath. Zero new dependencies |
| `webhookId` as plain `Long` (not `@ManyToOne`) | Full JPA relationship | Keeps `WebhookDeliveryLog` lightweight, avoids lazy-loading in async/retry paths. FK constraint in DB ensures referential integrity. CASCADE delete cleans up logs when a webhook is removed |
| Wildcard `"*"` matching in JPQL | Java-side filtering or enum conversion | Simple `OR` conditions in JPQL — no enum conversion needed. `entityType="*"` means "all entities", `eventType="*"` means "all events" |
| Hard delete for webhooks | Soft delete like domain entities | These are infrastructure entities, not domain data. No soft-delete needed. CASCADE FK cleans up delivery logs automatically |
| Secret is write-only | Include secret in responses | Never exposed in `WebhookRegistrationResponse` to prevent leakage via API responses or logs |
| Response body truncated to 1024 chars | Store full response | Prevents unbounded storage from large webhook consumer responses |
| Scheduled retry (max 3 attempts, 60s interval) | Exponential backoff or message queue | Simple and predictable. For most transient failures, 3 attempts within 3 minutes is sufficient. A message queue (RabbitMQ, Kafka) would be more robust but adds infrastructure complexity |

### Interview talking points

**Q: Why not use a message queue (RabbitMQ/Kafka) instead of direct HTTP delivery?**
> Direct HTTP delivery with retry is simpler and sufficient for moderate webhook volumes. It avoids infrastructure dependencies (broker setup, dead-letter queues, consumer groups). For high-volume scenarios with guaranteed delivery, you'd migrate to an event bus, but the webhook registration/delivery-log pattern remains the same — only the transport changes.

**Q: How do consumers verify webhook authenticity?**
> Each webhook registration includes a secret (minimum 16 characters). Every delivery includes an `X-Webhook-Signature` header with `sha256=<hex>` — the HMAC-SHA256 of the JSON payload using the shared secret. Consumers compute the same HMAC on the received body and compare signatures. This prevents tampering and spoofing.

**Q: Why is the secret write-only?**
> The secret is never included in API responses (`WebhookRegistrationResponse` omits it). This follows the principle of least privilege — once set, the secret is only used internally for signing. If exposed in GET responses, it could be leaked via logs, browser history, or network sniffing. If a consumer forgets the secret, they can PATCH/PUT a new one.

**Q: What happens if a webhook endpoint is down?**
> The first delivery attempt fails and is logged with `success=false`. The `WebhookRetryScheduler` picks it up within 60 seconds and retries (up to 3 total attempts). If all attempts fail, the delivery log persists for debugging. The webhook registration remains active — future events will still be attempted.

**Q: Why modify `AsyncNotificationService` instead of creating a new service?**
> The existing `@Async` methods already run on the MDC-propagating thread pool, preserving correlation IDs for logging. Creating a separate service would require either calling it from AsyncNotificationService (same result) or injecting it into all three domain services (violates the plan's "no changes to callers" constraint). Keeping dispatch logic in the notification service maintains the single-responsibility of "notify about resource changes."

---

## 25. Feature 23 — Multi-language Support (i18n)

### What it does
Enables the API to return localized error messages (validation errors, exception responses, filter messages) based on the `Accept-Language` HTTP header. Default locale is English; Spanish is included as a second locale to demonstrate the mechanism.

### Why it matters
Multi-language support is a standard enterprise requirement for APIs serving international users. By externalizing all user-facing messages into resource bundles, adding a new language requires only a new `messages_xx.properties` file — zero code changes.

### How it works

**Configuration (`LocaleConfig.java`):**
```java
@Configuration
public class LocaleConfig {
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

**DTO validation messages** use `{key}` syntax (Jakarta Bean Validation standard):
```java
@NotBlank(message = "{validation.user.username.required}")
@Size(max = 255, message = "{validation.user.username.size}")
private String username;
```

**Service layer** throws `ResourceNotFoundException` with message code + args:
```java
.orElseThrow(() -> new ResourceNotFoundException("error.not.found.user", id));
```

**`GlobalExceptionHandler`** resolves messages via `MessageSource`:
```java
Locale locale = LocaleContextHolder.getLocale();
String message = messageSource.getMessage(ex.getMessageCode(), ex.getMessageArgs(), ex.getMessage(), locale);
```

**Message bundles** (`messages.properties` / `messages_es.properties`):
```properties
# English (default)
error.not.found.user=User not found with id {0}
validation.user.username.required=Username is required

# Spanish
error.not.found.user=Usuario no encontrado con id {0}
validation.user.username.required=El nombre de usuario es obligatorio
```

### Files created
| File | Purpose |
|---|---|
| `LocaleConfig.java` | MessageSource, LocaleResolver, and LocalValidatorFactoryBean beans |
| `messages.properties` | English (default) message bundle (~70 keys) |
| `messages_es.properties` | Spanish translations for all keys |

### Files modified
| File | Change |
|---|---|
| `ResourceNotFoundException.java` | Added `messageCode` + `messageArgs` fields and varargs constructor |
| `GlobalExceptionHandler.java` | Injected `MessageSource`; all 10 handlers resolve messages via bundle |
| `IdempotencyFilter.java` | Conflict message resolved via `MessageSource` |
| 9 DTO files | Replaced hardcoded messages with `{key}` references |
| 4 Service files | `ResourceNotFoundException` now uses message codes |
| `application.properties` | Added `spring.messages.basename=messages` |

### Trade-offs

| Decision | Pro | Con |
|---|---|---|
| `AcceptHeaderLocaleResolver` | Standard HTTP mechanism, stateless | No per-user persistence (rely on client header) |
| `{key}` in validation annotations | Jakarta standard, auto-resolved by Hibernate Validator | Keys must match exactly between annotation and bundle |
| Message codes in exceptions | Clean separation of resolution from business logic | Slightly more verbose than inline messages |
| Log messages stay in English | Consistent for ops/debugging across all locales | Developers in non-English teams see English logs |

### Interview Q&A

**Q: Why not use `ResourceBundleMessageSource` instead of `ReloadableResourceBundleMessageSource`?**
> `ReloadableResourceBundleMessageSource` supports hot-reloading of message files without restarting the application (useful in dev). It also supports `classpath:` prefixed basenames and configurable encoding. The reloading overhead is negligible — it only checks file timestamps periodically.

**Q: How does `{key}` syntax work in validation annotations?**
> Jakarta Bean Validation (JSR 380) specifies that curly-braced values in `message` attributes are interpolated as resource bundle keys. Hibernate Validator (the reference implementation) delegates to a `MessageInterpolator`. By wiring `MessageSource` into `LocalValidatorFactoryBean`, Spring routes key resolution through Spring's `MessageSource`, which reads from `messages.properties` bundles. Constraint attributes like `{max}`, `{min}`, `{value}` are also interpolated automatically.

**Q: What happens if a message key is missing from the bundle?**
> All `messageSource.getMessage()` calls in `GlobalExceptionHandler` use the 4-argument overload that includes a default message. If the key is missing, the default (English) message is returned. For validation annotations, Hibernate Validator falls back to the key string itself (e.g., `{validation.user.username.required}`).

**Q: How would you add a third language (e.g., French)?**
> Create `messages_fr.properties` with French translations for all keys. No code changes needed. The `AcceptHeaderLocaleResolver` automatically picks up `Accept-Language: fr` and resolves from the French bundle. If a key is missing in French, it falls back to the default (`messages.properties`).

**Q: Why are log messages not internationalized?**
> Internal logs are consumed by developers and operations teams using monitoring tools (ELK, Splunk, etc.). Consistent English logging ensures searchability and alerting rules work regardless of which locale triggered the request. Internationalizing logs would complicate log analysis and serve no user-facing benefit.

---

## 26. Feature 24 — Timezone Handling (UTC Standardization)

### What it does

Replaces all `LocalDateTime` usage with `java.time.Instant` to standardize every timestamp in the system on UTC. API responses now include the `Z` suffix (e.g., `"2024-02-16T15:30:45.123Z"`), database columns store timezone-aware values, and the JVM default timezone is set to UTC as a safety net.

### Why it matters

`LocalDateTime` is timezone-naive — it represents a date and time without any offset or zone. In a distributed system where servers may run in different time zones, this causes:
- **Ambiguous timestamps** — `2024-02-16T15:30:45` could mean different instants depending on the server's locale
- **Incomparable values** — cleanup schedulers and retention policies produce wrong results if JVM timezones differ
- **API confusion** — clients have no way to know what timezone a response timestamp refers to

`Instant` is always UTC by definition. It represents an unambiguous point on the timeline with nanosecond precision. Combined with `TIMESTAMP WITH TIME ZONE` in the database and ISO 8601 serialization in JSON, this eliminates all timezone ambiguity.

### How it works

**Java type change — `LocalDateTime` → `Instant`:**
- 3 entities: `WebhookRegistration.createdAt`, `WebhookDeliveryLog.createdAt`, `IdempotencyRecord.createdAt`
- 2 response DTOs: `WebhookRegistrationResponse.createdAt`, `WebhookDeliveryLogResponse.createdAt`
- 2 repositories: `deleteByCreatedAtBefore(Instant cutoff)` parameter type
- All call sites: `Instant.now()` replaces `LocalDateTime.now()` in services, filters, schedulers, exception handler

**JVM default timezone:**
```java
public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(SimpleEnterprizeProj2Application.class, args);
}
```
Runs before Spring context initialization. Ensures any accidental `LocalDateTime.now()` or JDBC driver behavior defaults to UTC.

**Jackson configuration:**
```properties
spring.jackson.datatype.datetime.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
```
In Jackson 3.x (Spring Boot 4.x), `WRITE_DATES_AS_TIMESTAMPS` moved from `SerializationFeature` to `DateTimeFeature`, so the property lives under `spring.jackson.datatype.datetime.*`. Without this setting, Jackson serializes `Instant` as a raw epoch number (e.g., `1708100000.123`). With it, Jackson produces human-readable ISO 8601 strings: `"2024-02-16T15:30:45.123Z"`.

**Database migration (Liquibase `006-convert-timestamps-to-utc.yaml`):**
```yaml
- modifyDataType:
    tableName: idempotency_keys
    columnName: created_at
    newDataType: TIMESTAMP WITH TIME ZONE
```
Applied to all 3 tables. `TIMESTAMP WITH TIME ZONE` stores the UTC offset alongside the value, preventing data loss if the JVM timezone changes.

**Cleanup schedulers:**
```java
// Before: LocalDateTime.now().minusHours(24)
Instant cutoff = Instant.now().minus(Duration.ofHours(24));
```
`Instant` doesn't have `minusHours()` directly — instead, `Duration` is used for time-based arithmetic.

### Key files changed

| File | Change |
|---|---|
| `SimpleEnterprizeProj2Application.java` | `TimeZone.setDefault(UTC)` in `main()` |
| `application.properties` | `write-dates-as-timestamps=false`, `time-zone=UTC` |
| `WebhookRegistration.java` | `LocalDateTime` → `Instant` |
| `WebhookDeliveryLog.java` | `LocalDateTime` → `Instant` |
| `IdempotencyRecord.java` | `LocalDateTime` → `Instant` (field + constructor) |
| `WebhookRegistrationResponse.java` | `LocalDateTime` → `Instant` |
| `WebhookDeliveryLogResponse.java` | `LocalDateTime` → `Instant` |
| `IdempotencyRepository.java` | `deleteByCreatedAtBefore(Instant)` |
| `WebhookDeliveryLogRepository.java` | `deleteByCreatedAtBefore(Instant)` |
| `GlobalExceptionHandler.java` | `Instant.now().toString()` |
| `IdempotencyFilter.java` | `Instant.now()` in record creation + conflict response |
| `AsyncNotificationService.java` | `Instant.now()` in payload + delivery log |
| `WebhookService.java` | `Instant.now()` in `create()` |
| `IdempotencyCleanupScheduler.java` | `Instant.now().minus(Duration.ofHours(24))` |
| `WebhookCleanupScheduler.java` | `Instant.now().minus(Duration.ofDays(7))` |
| `006-convert-timestamps-to-utc.yaml` | `TIMESTAMP` → `TIMESTAMP WITH TIME ZONE` |

### Design decisions & trade-offs

| Decision | Rationale |
|---|---|
| `Instant` over `OffsetDateTime` | `Instant` is always UTC — no ambiguity, no offset to manage. `OffsetDateTime` carries an offset that adds complexity without benefit when standardizing on UTC. |
| `Instant` over `LocalDateTime` + UTC convention | `LocalDateTime` is semantically "no timezone". Relying on convention is fragile. `Instant` makes UTC a compile-time guarantee. |
| `TimeZone.setDefault(UTC)` in `main()` | Defense-in-depth. Catches any accidental `LocalDateTime.now()` or JDBC driver timezone behavior. |
| `TIMESTAMP WITH TIME ZONE` in DB | Stores the UTC offset with the value. H2 supports this natively. Prevents data loss if the JVM timezone changes. |
| `write-dates-as-timestamps=false` | Without this, Jackson serializes `Instant` as epoch numbers (unreadable). ISO 8601 strings are human-readable and include the `Z` suffix. |
| Zero new dependencies | `Instant`, `Duration`, and `JavaTimeModule` are already in the classpath (Java stdlib + jackson-datatype-jsr310 via Spring Boot). |

### Interview Q&A

**Q: Why `Instant` instead of `OffsetDateTime` or `ZonedDateTime`?**
> `Instant` represents a point on the UTC timeline — it has no offset or zone to manage. `OffsetDateTime` carries an offset (e.g., `+00:00`) that adds serialization complexity without benefit when you've standardized on UTC. `ZonedDateTime` carries a full zone ID (e.g., `America/New_York`) which is useful for user-facing display but inappropriate for storage. For backend timestamps, `Instant` is the simplest correct choice.

**Q: What does `TimeZone.setDefault(UTC)` actually do?**
> It sets the JVM's default timezone, which affects `Calendar.getInstance()`, `new Date()`, `SimpleDateFormat`, JDBC `Timestamp` conversions, and any code that calls `ZoneId.systemDefault()`. By setting it to UTC before Spring initialization, we ensure all framework and library code that depends on the default timezone operates in UTC. It's a safety net — our code uses `Instant` explicitly, but third-party libraries or accidental `LocalDateTime.now()` calls will also produce UTC values.

**Q: What happens to existing data when you migrate from `TIMESTAMP` to `TIMESTAMP WITH TIME ZONE`?**
> H2 treats `TIMESTAMP` values as UTC by default. When converting to `TIMESTAMP WITH TIME ZONE`, existing values are interpreted in the session timezone (which we've set to UTC via `TimeZone.setDefault`). So existing data is preserved correctly. In production with PostgreSQL, `TIMESTAMP WITHOUT TIME ZONE` values are also interpreted in the session timezone during conversion, so the same `TimeZone.setDefault(UTC)` ensures correctness.

**Q: Why not use `@CreatedDate` from Spring Data Auditing instead of manual `Instant.now()`?**
> Spring Data Auditing (`@CreatedDate`, `@EnableJpaAuditing`) is a valid approach for entities managed through Spring Data repositories. However, the `IdempotencyRecord` is created in a servlet filter context, and webhook delivery logs are created in async service code. Using `Instant.now()` explicitly is simpler and consistent across all creation paths. Introducing auditing would add framework coupling for a straightforward timestamp assignment.

**Q: How does `Instant` interact with Hibernate and JPA?**
> Hibernate 6+ (used by Spring Boot 3+) natively maps `java.time.Instant` to SQL `TIMESTAMP WITH TIME ZONE`. No `@Temporal` annotation needed (that's for legacy `java.util.Date`). Hibernate stores and retrieves `Instant` values in UTC, and the JDBC driver handles the conversion between Java's `Instant` and the database's timestamp type.

---

## 27. Cross-Cutting Concerns Summary

### How the 24 features interact

```
Request Flow:
1. HTTP Request arrives
2. SecurityHeadersFilter (@Order 0) adds response headers
3. RequestLoggingFilter (@Order 5) generates correlationId, starts timer
4. IdempotencyFilter (@Order 10) checks Idempotency-Key header (POST only → replay/409/proceed)
5. @Validated controller validates @RequestParam (ConstraintViolationException → 400)
6. @Valid validates @RequestBody (MethodArgumentNotValidException → 400)
7. Controller calls Service method
8. @CircuitBreaker checks circuit state (OPEN → fallbackMethod → 503 or Page.empty)
9. @Bulkhead checks permits (FULL → BulkheadFullException → 429)
10. @Retry attempts call (up to 3x with exponential backoff on failure)
11. @Cacheable checks Redis (hit → return cached DTO, skip steps 12-16)
12. @Transactional opens/joins transaction
13. Repository executes query with Specification filters
14. @QueryHints checks Hibernate query cache (hit → return entity IDs from EhCache)
15. Hibernate checks L2 entity cache (hit → return cached entity, no SQL)
16. If cache miss → SQL executes against H2 (schema managed by Liquibase)
17. Mapper converts Entity → Response DTO (with sanitization on writes)
18. AsyncNotificationService fires @Async notification (write operations only, runs on async- pool)
19. Webhook dispatch — queries matching registrations, POSTs payload with HMAC signature, logs delivery
20. HATEOAS links added by controller
21. Response returned (IdempotencyFilter stores response for POST with key)
22. RequestLoggingFilter logs method, URI, status, duration
23. GlobalExceptionHandler catches any exceptions → consistent JSON error
```

### Cache layers (from fastest to slowest)

| Layer | Technology | Scope | TTL | Hit = |
|---|---|---|---|---|
| 1. Application cache | Redis | Distributed | 10 min | Skip steps 9-13 |
| 2. Hibernate query cache | EhCache | JVM-local | 5 min | Skip SQL, use entity IDs |
| 3. Hibernate entity cache | EhCache | JVM-local | 10 min | Skip SQL for entity load |
| 4. Database | H2 + HikariCP | Persistent | — | Full SQL execution |

### Validation layers (defense-in-depth)

| Layer | Mechanism | Example |
|---|---|---|
| 1. Controller | `@Size(max=255)` on query params | Reject oversized search terms |
| 2. DTO | `@NotBlank`, `@Email`, `@Pattern` | Reject invalid format before service layer |
| 3. Mapper | `SanitizationUtils.sanitize()` | Encode HTML entities (XSS prevention) |
| 4. Specification | `SanitizationUtils.escapeWildcards()` | Escape LIKE wildcards |
| 5. Entity | `@Column(nullable=false, unique=true)` | JPA constraints |
| 6. Database | `NOT NULL`, `UNIQUE` constraints (Liquibase) | Last line of defense |

### Profile-aware behavior

| Concern | Dev Profile | Prod Profile |
|---|---|---|
| Database | H2 in-memory (`mem:testdb`) | H2 file-based (`file:./data/proddb`) |
| H2 Console | Enabled at `/h2-console` | Disabled |
| SQL logging | `show-sql=true` | `show-sql=false` |
| HikariCP pool | max=5, min=2 | max=20, min=5 |
| Hibernate stats | Enabled | Disabled |
| Async pool | core=2, max=4, queue=50 | core=4, max=8, queue=100 |
| Log format | Text with correlationId | JSON (structured) |
| Log level | DEBUG for app package | INFO for app package |

---

## 28. Full API Endpoint Reference

### User Endpoints

| Method | Path | Body | Response | Status Codes |
|---|---|---|---|---|
| GET | `/api/v1/users` | — | `PagedModel<EntityModel<UserResponse>>` | 200, 400, 500 |
| GET | `/api/v1/users/{id}` | — | `EntityModel<UserResponse>` | 200, 404, 500 |
| POST | `/api/v1/users` | `UserRequest` | `EntityModel<UserResponse>` | 201, 400, 409, 500 |
| PUT | `/api/v1/users/{id}` | `UserRequest` | `EntityModel<UserResponse>` | 200, 400, 404, 409, 500 |
| PATCH | `/api/v1/users/{id}` | `UserPatchRequest` | `EntityModel<UserResponse>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/users/{id}` | — | — | 204, 404, 500 |
| POST | `/api/v1/users/bulk` | `List<UserRequest>` | `List<EntityModel<UserResponse>>` | 201, 400, 409, 500 |
| PUT | `/api/v1/users/bulk` | `List<BulkUpdateRequest<UserRequest>>` | `List<EntityModel<UserResponse>>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/users/bulk` | `List<Long>` | — | 204, 400, 404, 500 |

**Query parameters for GET list**: `username`, `email`, `role`, `page`, `size`, `sort`

### Employee Endpoints

| Method | Path | Body | Response | Status Codes |
|---|---|---|---|---|
| GET | `/api/v1/employees` | — | `PagedModel<EntityModel<EmployeeResponse>>` | 200, 400, 500 |
| GET | `/api/v1/employees/{id}` | — | `EntityModel<EmployeeResponse>` | 200, 404, 500 |
| POST | `/api/v1/employees` | `EmployeeRequest` | `EntityModel<EmployeeResponse>` | 201, 400, 409, 500 |
| PUT | `/api/v1/employees/{id}` | `EmployeeRequest` | `EntityModel<EmployeeResponse>` | 200, 400, 404, 409, 500 |
| PATCH | `/api/v1/employees/{id}` | `EmployeePatchRequest` | `EntityModel<EmployeeResponse>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/employees/{id}` | — | — | 204, 404, 500 |
| POST | `/api/v1/employees/bulk` | `List<EmployeeRequest>` | `List<EntityModel<EmployeeResponse>>` | 201, 400, 409, 500 |
| PUT | `/api/v1/employees/bulk` | `List<BulkUpdateRequest<EmployeeRequest>>` | `List<EntityModel<EmployeeResponse>>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/employees/bulk` | `List<Long>` | — | 204, 400, 404, 500 |

**Query parameters for GET list**: `firstName`, `lastName`, `email`, `departmentId`, `page`, `size`, `sort`

### Department Endpoints

| Method | Path | Body | Response | Status Codes |
|---|---|---|---|---|
| GET | `/api/v1/departments` | — | `PagedModel<EntityModel<DepartmentResponse>>` | 200, 400, 500 |
| GET | `/api/v1/departments/{id}` | — | `EntityModel<DepartmentResponse>` | 200, 404, 500 |
| POST | `/api/v1/departments` | `DepartmentRequest` | `EntityModel<DepartmentResponse>` | 201, 400, 409, 500 |
| PUT | `/api/v1/departments/{id}` | `DepartmentRequest` | `EntityModel<DepartmentResponse>` | 200, 400, 404, 409, 500 |
| PATCH | `/api/v1/departments/{id}` | `DepartmentPatchRequest` | `EntityModel<DepartmentResponse>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/departments/{id}` | — | — | 204, 404, 500 |
| POST | `/api/v1/departments/bulk` | `List<DepartmentRequest>` | `List<EntityModel<DepartmentResponse>>` | 201, 400, 409, 500 |
| PUT | `/api/v1/departments/bulk` | `List<BulkUpdateRequest<DepartmentRequest>>` | `List<EntityModel<DepartmentResponse>>` | 200, 400, 404, 409, 500 |
| DELETE | `/api/v1/departments/bulk` | `List<Long>` | — | 204, 400, 404, 500 |

**Query parameters for GET list**: `name`, `page`, `size`, `sort`

### Webhook Endpoints

| Method | Path | Body | Response | Status Codes |
|---|---|---|---|---|
| GET | `/api/v1/webhooks` | — | `PagedModel<EntityModel<WebhookRegistrationResponse>>` | 200, 400, 500 |
| GET | `/api/v1/webhooks/{id}` | — | `EntityModel<WebhookRegistrationResponse>` | 200, 404, 500 |
| POST | `/api/v1/webhooks` | `WebhookRegistrationRequest` | `EntityModel<WebhookRegistrationResponse>` | 201, 400, 500 |
| PUT | `/api/v1/webhooks/{id}` | `WebhookRegistrationRequest` | `EntityModel<WebhookRegistrationResponse>` | 200, 400, 404, 500 |
| PATCH | `/api/v1/webhooks/{id}` | `WebhookRegistrationPatchRequest` | `EntityModel<WebhookRegistrationResponse>` | 200, 400, 404, 500 |
| DELETE | `/api/v1/webhooks/{id}` | — | — | 204, 404, 500 |
| GET | `/api/v1/webhooks/{id}/deliveries` | — | `PagedModel<EntityModel<WebhookDeliveryLogResponse>>` | 200, 404, 500 |

**Query parameters for GET list**: `page`, `size`, `sort`

### Utility Endpoints

| Path | Description |
|---|---|
| `/swagger-ui.html` | Interactive API documentation |
| `/v3/api-docs` | OpenAPI 3.0 JSON specification |
| `/h2-console` | Database admin console (dev profile only) |

---

## HikariCP Connection Pool Configuration

**`DatabaseConfig.java`** — `src/main/java/.../config/DatabaseConfig.java`:
```java
@Configuration
public class DatabaseConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDataSourceInfo(ApplicationReadyEvent event) {
        HikariDataSource ds = event.getApplicationContext().getBean(HikariDataSource.class);
        logger.info("HikariCP pool '{}' active — max size: {}, min idle: {}",
                ds.getPoolName(), ds.getMaximumPoolSize(), ds.getMinimumIdle());
    }
}
```

| Setting | Dev | Prod | Why |
|---|---|---|---|
| `maximum-pool-size` | 5 | 20 | Dev has minimal load; prod handles concurrent users |
| `minimum-idle` | 2 | 5 | Keep connections warm for fast response |
| `idle-timeout` | 30s | 30s | Return idle connections to the pool |
| `max-lifetime` | 10min | 10min | Prevent stale connections (firewalls, DB restarts) |
| `connection-timeout` | 20s | 20s | Max wait for a connection from the pool |

### Interview talking points for HikariCP

**Q: How do you size the connection pool?**
> The formula `connections = (core_count * 2) + effective_spindle_count` works for most cases. For an SSD-backed database, `core_count * 2` is a good starting point. Monitor `HikariPoolMXBean` for pending threads and active connections. If `pending > 0` frequently, increase the pool.

**Q: Why not set `maximum-pool-size` very high (e.g., 100)?**
> Each connection consumes memory on both the app and database side. Too many connections cause contention (context switching, lock waits). A smaller pool with queuing often outperforms a large pool.

---

*This document covers all 21 features implemented in the SimpleEnterprizeProj2 project. Each section is designed to help you articulate the what, why, and how at Developer, Tech Lead, and Architect interview levels.*
