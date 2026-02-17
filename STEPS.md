# Implementation Steps

Summary of all features implemented on the `dev` branch, in chronological order.

## 1. CRUD Foundation (`db7231a`)

Implemented domain models, JPA repositories, service layer, and REST controllers for three entities: **User**, **Employee**, and **Department**. Configured H2 in-memory database with console access and a global exception handler for consistent error responses.

## 2. Spring Profiles & Connection Pooling (`d97bf60`)

Introduced profile-specific configuration for **dev** (H2 in-memory) and **prod** (H2 file-based) environments. Tuned HikariCP connection pool settings per profile and added a `DatabaseConfig` class that exposes the DataSource bean with startup logging.

## 3. Liquibase Database Migrations (`1e87146`)

Replaced Hibernate `ddl-auto` with **Liquibase**-managed schema migrations. Added an initial changeset that creates `users`, `departments`, and `employees` tables with named constraints and foreign keys, ensuring repeatable and version-controlled schema evolution.

## 4. Transaction Management (`9dda3c3`)

Added `@Transactional` annotations to the service layer. Class-level `readOnly=true` by default with read-write overrides on `create`, `update`, and `delete` methods to ensure multi-step operations (especially cross-repository calls in EmployeeService) are atomic.

## 5. Database Indexes (`6b85cfd`)

Added performance indexes via Liquibase migration:
- `employees.department_id` — accelerates FK joins
- `employees(last_name, first_name)` — speeds up name-based searches
- `users.role` — optimizes role-based filtering

## 6. Soft Deletes (`69e3593`)

Replaced hard deletes with soft deletes using a `deleted` boolean column. Applied Hibernate `@SQLRestriction` to automatically filter deleted records from all queries. Includes a Liquibase migration for the new columns and indexes.

## 7. RESTful API Standards (`7fca845`)

Major API overhaul:
- **DTOs** (request/patch/response) to decouple API from JPA entities and hide sensitive fields (e.g., password)
- **Mapper classes** for entity-DTO conversion with full and partial update support
- **JPA Specifications** for filterable queries on all resources
- **Pagination** with `Page<ResponseDTO>`, default page size 20, max 100
- **HATEOAS** with `EntityModel`/`PagedModel` and self-links
- **PATCH endpoints** for partial updates
- **Location headers** on resource creation
- Expanded `GlobalExceptionHandler` with validation (400), data integrity (409), type mismatch, and malformed JSON handling
- Added `spring-boot-starter-validation` and `spring-boot-starter-hateoas` dependencies

## 8. Input Validation & Sanitization (`c498497`)

Introduced `SanitizationUtils` for HTML stripping, whitespace trimming, and LIKE wildcard escaping. Added `@Pattern` constraints to DTOs for username, role, phone, password complexity, and name fields. Applied sanitization in mappers, escaped wildcards in specifications, added `@Size` limits on query parameters with `@Validated` controllers, and handled `ConstraintViolationException`.

## 9. Cross-DB LIKE Escape Portability (`0e69da5`)

Declared the LIKE escape character explicitly in JPA Specification queries to ensure consistent behavior across different database engines.

## 10. XSS Protection (`0eddcb0`)

Added a `SecurityHeadersFilter` servlet filter for response security headers (`X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy`, etc.). Replaced the bypassable regex-based `sanitize()` with OWASP-style HTML entity encoding. Added `@Pattern` validation to previously unprotected department description fields.

## 11. Centralized Logging (`fe4e3f7`)

Added structured logging across the application:
- **Request tracing** — logs incoming HTTP requests with correlation IDs
- **Exception logging** — consistent error logging in the global exception handler
- **Service audit logs** — logs create/update/delete operations in service methods

## 12. OpenAPI Documentation (`bb4dc2a`)

Integrated **springdoc-openapi** with Swagger UI. API documentation is auto-generated from controllers and available at `/swagger-ui.html`. Configured API docs endpoint at `/v3/api-docs`.

## 13. Redis Caching (`bc406c0`)

Added **Spring Data Redis** caching for the User domain with a 10-minute TTL. Applied `@Cacheable`, `@CachePut`, and `@CacheEvict` annotations to UserService methods. Configured `RedisCacheConfig` with JSON serialization.

## 14. Circuit Breakers (`4f99688`)

Added **Resilience4j** circuit breakers to all three services (`UserService`, `EmployeeService`, `DepartmentService`):
- `@CircuitBreaker` annotation on all public service methods
- Count-based sliding window (size=10), 50% failure threshold, 10s open-state wait
- `ResourceNotFoundException` ignored (404s are normal business logic, not failures)
- `CallNotPermittedException` handler in `GlobalExceptionHandler` returns 503 Service Unavailable
- One circuit breaker instance per service for isolated failure domains
- All configuration externalized in `application.properties`

## 15. Hibernate Second-Level Cache & Query Cache (`4a43cb2`)

Added **Hibernate L2 cache** and **query cache** using EhCache 3 via hibernate-jcache, complementing the existing Redis application-level cache:
- **Entity caching** — `@Cacheable` + `@Cache` on all entities: `READ_WRITE` for User/Employee, `NONSTRICT_READ_WRITE` for Department
- **Query caching** — `@QueryHints(HINT_CACHEABLE)` on `findAll(Specification, Pageable)` and `findById` in all repositories
- **EhCache regions** — User (1000 entries), Employee (2000), Department (200) with 10-min TTL; query results (500) with 5-min TTL; update timestamps (5000) with no expiry
- **ENABLE_SELECTIVE** shared cache mode — only annotated entities are cached
- **Profile-aware statistics** — `hibernate.generate_statistics=true` in dev, disabled in prod
- `HibernateCacheConfig` resolves ehcache.xml classpath URI for Hibernate's JCache integration

## 16. Retry & Fallback (`dev`)

Added **Resilience4j Retry** with fallback methods alongside existing circuit breakers:
- `@Retry` annotation on all 21 service methods across 3 services
- Exponential backoff: 3 attempts, 500ms initial wait, multiplier 2 (500ms → 1000ms)
- `ResourceNotFoundException` ignored (404 is business logic, not transient failure)
- Decorator ordering: `CircuitBreaker(outer)` → `Retry(inner)` via aspect order properties
- **Fallback methods** on `@CircuitBreaker`: `findAll` returns `Page.empty(pageable)` for graceful degradation; all other methods throw `ServiceUnavailableException` (503)
- New `ServiceUnavailableException` with `@ExceptionHandler` in `GlobalExceptionHandler` returning 503

## 17. Bulkhead Pattern (`dev`)

Added **Resilience4j Semaphore Bulkhead** to limit concurrent calls per service, preventing one overloaded service from starving thread resources for others:
- `@Bulkhead` annotation on all 21 service methods across 3 services
- **Semaphore type** (default): lightweight counter-based concurrency limiter — no extra thread pool, preserves `@Transactional` thread-local context
- `userService` and `employeeService`: max 10 concurrent calls; `departmentService`: max 5 (lower traffic)
- `maxWaitDuration=0ms` — fail-fast when bulkhead is full (no queuing)
- Updated decorator ordering: `CircuitBreaker(1)` → `Bulkhead(2147483646, hardcoded)` → `Retry(2147483647)` — each retry attempt holds a permit
- `BulkheadFullException` → 429 Too Many Requests via `GlobalExceptionHandler`
