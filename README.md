# Spring Boot API Template

A reusable Spring Boot 4 / Java 17 starter for building secure REST APIs. Comes pre-wired with JWT authentication, RBAC (roles + permissions), JPA auditing, soft-delete, global exception handling, file uploads, structured logging, OpenAPI docs, Flyway migrations, **DB-backed audit logging**, **per-identity rate limiting**, and **`.env`-driven configuration**.

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Quick Start](#quick-start)
3. [Default Credentials](#default-credentials)
4. [Project Structure](#project-structure)
5. [Adding a New API Module](#adding-a-new-api-module)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
   - [Authentication (JWT)](#authentication-jwt)
   - [Authorization](#authorization--hasrole--haspermission)
   - [Current User](#current-user-context)
   - [ApiResponse envelope](#apiresponse-standard-envelope)
   - [Pagination](#pagination--automatic)
   - [Auditing & Soft Delete](#auditing--automatic)
   - [Filtering](#filtering--jpa-specifications)
   - [File Uploads](#file-uploads--filestorageservice)
   - [Auto-Mapping (Entity ↔ DTO)](#auto-mapping-entity--dto)
   - [Audit Logging](#audit-logging-db-backed)
   - [Rate Limiting](#rate-limiting)
   - [Caching](#caching)
   - [Validation & Exceptions](#validation)
7. [Configuration Reference](#configuration-reference)
8. [Built-in Endpoints](#built-in-endpoints)
9. [Database Migrations](#database-migrations)
10. [Logging](#logging)
11. [Switching Cache to Redis](#switching-cache-to-redis)
12. [Build & Test](#build--test)
13. [What's Intentionally NOT in the Box](#whats-intentionally-not-in-the-box)

---

## Tech Stack

| Concern | Choice |
|---|---|
| Runtime | Spring Boot 4.0.x · Spring Framework 7 · Java 17 |
| Web | `spring-boot-starter-webmvc` (servlet, blocking) |
| Persistence | Spring Data JPA · Hibernate · MySQL / PostgreSQL · Flyway |
| Security | Spring Security 7 · JJWT 0.12 (HS256) |
| AOP | aspectjweaver (annotation-driven authorization & audit metadata) |
| JSON | Jackson 3 (`tools.jackson.databind`) |
| Mapping | ModelMapper 3.2 — fully automatic, including `Set<Role>` → `Set<String>` collapses |
| Validation | Jakarta Bean Validation |
| Docs | springdoc-openapi 3 (Swagger UI) |
| Logging | Logback (rolling files, 3-day retention, MDC `requestId` + `username`) |
| Rate limiting | Bucket4j 8.10 (in-memory, per-identity buckets) |
| Caching | Spring Cache abstraction · Caffeine (default) · Redis-ready (one env flip) |
| Config | spring-dotenv 4.0 (auto-loads `.env`) + `@ConfigurationProperties` |
| Build | Maven |

---

## Quick Start

### 1. Prerequisites
- JDK 17+
- MySQL 8 (or PostgreSQL — both drivers ship; flip `DB_DRIVER`)
- Maven (`./mvnw` wrapper is included)

### 2. Bring up your environment file
```bash
cp .env.example .env
# edit .env if your DB creds differ from the defaults
```
`.env` is gitignored. `.env.example` is committed and is the contract — every supported variable is documented there.

### 3. Run
```bash
./mvnw spring-boot:run
```
Boot order: spring-dotenv loads `.env` → Flyway applies migrations → Hibernate validates the schema → `DataInitializer` bootstraps the default admin if missing → app listens on `:8080`.

### 4. Try it
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

---

## Default Credentials

| Field | Value |
|---|---|
| Username | `admin` |
| Email | `admin@gmail.com` |
| Password | `admin123` |
| Role | `ADMIN` (every permission, including `AUDIT_READ`) |

**Change these before any non-local deployment.** Override via `.env` or env vars:
```bash
APP_BOOTSTRAP_ADMIN_PASSWORD='strong-pass' ./mvnw spring-boot:run
```

Get a token:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"usernameOrEmail":"admin","password":"admin123"}'
```

Send the access token as `Authorization: Bearer <token>` on protected endpoints.

---

## Project Structure

```
src/main/java/com/template/springboot/
├── SpringbootApplication.java
├── config/                       ← framework wiring
│   ├── SecurityConfig            (JWT filter chain, public paths, CORS, rate-limit hookup)
│   ├── JpaAuditingConfig         (@CreatedBy / @CreatedDate)
│   ├── ModelMapperConfig         (TypeMaps + collection-collapse converters)
│   ├── OpenApiConfig             (Swagger bearer scheme)
│   └── DataInitializer           (bootstraps default admin)
├── common/                       ← reusable infrastructure (depend on, don't duplicate)
│   ├── audit/                    BaseEntity · AuditorAwareImpl
│   ├── dto/                      ApiResponse · PageResponse · BaseResponse  ← inherited audit fields
│   ├── exception/                GlobalExceptionHandler · custom exceptions
│   ├── mapper/                   GenericMapper (universal map(source, target.class))
│   ├── ratelimit/                RateLimitFilter · RateLimitService · RateLimitProperties
│   ├── security/                 JwtService · JwtAuthenticationFilter · SecurityUtils · CurrentUserService
│   │                             HasRole / HasPermission annotations + AuthorizationAspect
│   │                             CustomUserDetails(Service) · entry-point/access-denied handlers
│   ├── specification/            SpecificationBuilder helpers
│   └── web/                      ApiResponseBodyAdvice (status injection)
│                                 RequestLoggingFilter (MDC requestId / username)
└── modules/                      ← every feature lives here, identical shape
    ├── audit/                    annotation · aspect · config · context · controller · dto · entity ·
    │                             filter · repository · service · specification · util
    ├── auth/                     controller · dto · enums · service · serviceImpl
    ├── file/                     generic file upload + serve service
    ├── permission/               entity · enums · repository · dto · mapper · service · serviceImpl · specification · controller
    ├── role/                     same shape as permission
    ├── user/                     same shape + specification (filtering)
    └── product/                  full reference module — multipart + image, full CRUD
```

Resources:
```
src/main/resources/
├── application.yml               (shared + dev/prod profiles; every value env-driven)
├── logback-spring.xml            (rolling per-category log files)
└── db/migration/                 (Flyway: V1__init, V2__products, V3__role_perms, V4__image_url, V5__soft_delete, V6__audit_logs)
```

Project root:
```
.env                              (gitignored — local dev values)
.env.example                      (committed — documents every variable)
```

---

## Adding a New API Module

The pattern is identical for every module. The mapping layer is now fully automatic — adding a field is a 3-edit operation.

### Recipe — let's say you're adding `Order`

#### 1. Scaffold the folder
```
modules/order/
├── controller/OrderController.java
├── dto/OrderRequest.java
├── dto/OrderResponse.java
├── dto/OrderFilter.java
├── entity/Order.java
├── enums/OrderStatus.java                  (optional)
├── mapper/OrderMapper.java
├── repository/OrderRepository.java
├── service/OrderService.java               (interface)
├── serviceImpl/OrderServiceImpl.java       (NOT inside service/)
└── specification/OrderSpecifications.java  (only if filtered list)
```

#### 2. Entity — extends `BaseEntity` for free auditing + soft delete
```java
@Entity @Table(name = "orders")
@Getter @Setter @NoArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // your fields
}
```

#### 3. Repository — interface only
```java
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {}
```

#### 4. DTOs — Lombok classes (NOT records — ModelMapper needs setters)
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotBlank private String reference;
    @NotNull  private BigDecimal total;
}

// Extends BaseResponse — inherits createdAt/updatedAt/createdBy/updatedBy/deletedAt/deletedBy.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderResponse extends BaseResponse {
    private Long id;
    private String reference;
    private BigDecimal total;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderFilter {
    private String q;
}
```

> No `from()` factory needed. ModelMapper picks up matching field names and inherited setters from `BaseResponse`.

#### 5. Mapper — one-liners, ModelMapper does the work
```java
@Component @RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;
    public Order toEntity(OrderRequest r)            { return modelMapper.map(r, Order.class); }
    public void applyUpdate(OrderRequest r, Order o) { modelMapper.map(r, o); }
    public OrderResponse toResponse(Order o)         { return modelMapper.map(o, OrderResponse.class); }
}
```

If your entity has a `Set<X>` you want collapsed to `Set<String>` (like `User.roles → UserResponse.roles`), register a TypeMap converter once in [ModelMapperConfig.java](src/main/java/com/template/springboot/config/ModelMapperConfig.java).

#### 6. Service interface
```java
public interface OrderService {
    OrderResponse create(OrderRequest request);
    OrderResponse update(Long id, OrderRequest request);
    OrderResponse getById(Long id);
    Page<OrderResponse> search(OrderFilter filter, Pageable pageable);
    void delete(Long id);
}
```

#### 7. Service impl — `@Service @RequiredArgsConstructor @Slf4j`
```java
@Service @RequiredArgsConstructor @Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override @Transactional
    public OrderResponse create(OrderRequest req) {
        Order saved = orderRepository.save(orderMapper.toEntity(req));
        log.info("Order created id={}", saved.getId());
        return orderMapper.toResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return orderRepository.findById(id)
            .map(orderMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
    // update, search, delete...
}
```

#### 8. Controller — `@HasPermission`, `@Auditable`, `ApiResponse` (no `ResponseEntity`)
```java
@RestController @RequestMapping("/api/v1/orders")
@RequiredArgsConstructor @Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @HasPermission(PermissionName.ORDER_WRITE)
    @Auditable(action = "ORDER_CREATE", resourceType = "Order")
    public ApiResponse create(@Valid @RequestBody OrderRequest req) {
        return ApiResponse.created(orderService.create(req));
    }

    @GetMapping
    @HasPermission(PermissionName.ORDER_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @ParameterObject Pageable pageable) {
        OrderFilter filter = new OrderFilter();
        filter.setQ(q);
        return new ApiResponse(orderService.search(filter, pageable));
    }
    // ...
}
```

`Pageable` automatically reads `page=`, `size=`, `sort=`. Returning `Page<T>` to `ApiResponse` triggers automatic `PageResponse` wrapping.

#### 9. Add permission constants to `PermissionName`
```java
public static final String ORDER_READ   = "ORDER_READ";
public static final String ORDER_WRITE  = "ORDER_WRITE";
public static final String ORDER_DELETE = "ORDER_DELETE";
```

#### 10. Flyway migration `V<n>__orders.sql`
```sql
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reference VARCHAR(100) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    deleted_at DATETIME(6) NULL,
    deleted_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('ORDER_READ',   'Read orders',   NOW(6), NOW(6), 'system', 'system'),
    ('ORDER_WRITE',  'Modify orders', NOW(6), NOW(6), 'system', 'system'),
    ('ORDER_DELETE', 'Delete orders', NOW(6), NOW(6), 'system', 'system');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('ORDER_READ', 'ORDER_WRITE', 'ORDER_DELETE');
```

That's it. Restart, log in as ADMIN, hit `/api/v1/orders`. Adding a new field later is a 3-edit job: entity + DTO + Flyway migration. The mapper, service, and controller all keep working.

---

## Cross-Cutting Concerns

### Authentication (JWT)
- Every request goes through [JwtAuthenticationFilter.java](src/main/java/com/template/springboot/common/security/JwtAuthenticationFilter.java).
- Send `Authorization: Bearer <accessToken>` on protected endpoints.
- The token's `uid` and authorities (roles + permissions) populate `SecurityContext`.
- Tokens are issued by `/api/v1/auth/login`. Refresh via `/api/v1/auth/refresh`. Lifetimes: env-driven, defaults 60 min access / 14 day refresh.

### Authorization — `@HasRole` / `@HasPermission`
Custom annotations enforced by [AuthorizationAspect.java](src/main/java/com/template/springboot/common/security/AuthorizationAspect.java). No `@PreAuthorize`, no SpEL — just the bare permission/role name.

```java
@HasPermission(PermissionName.PRODUCT_READ)   // 403 if missing
@HasRole(RoleName.ADMIN)                      // 403 if missing
```

Apply at method or class level (method wins when both present). Fails authentication → 401, fails authorization → 403, both responses are JSON via `GlobalExceptionHandler`.

### Current user context
Two ergonomic options that share the same `SecurityContextHolder` source:

**Static (anywhere on the request thread):**
```java
SecurityUtils.getCurrentUserId()        // Optional<Long>
SecurityUtils.getCurrentUsername()      // Optional<String>
SecurityUtils.getCurrentUserDetails()   // Optional<CustomUserDetails>
SecurityUtils.getCurrentAuthorities()   // Set<String>
SecurityUtils.hasAuthority("X")
SecurityUtils.hasRole("ADMIN")
```

**Injectable bean — `CurrentUserService`** ([source](src/main/java/com/template/springboot/common/security/CurrentUserService.java)) — preferred in services/controllers because it's testable and substitutable:
```java
@Service @RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final CurrentUserService currentUser;

    public OrderResponse create(OrderRequest req) {
        Long uid       = currentUser.getId();          // throws 401 if missing
        String name    = currentUser.getUsername();
        var me         = currentUser.get();            // CustomUserDetails
        boolean admin  = currentUser.hasRole("ADMIN");
        // find* variants return Optional when auth may legitimately be absent
    }
}
```

Both approaches require the call to be on the original request thread (or for `SecurityContextHolder` strategy to propagate). For `@Async` work, capture the user explicitly before crossing thread boundaries.

### `ApiResponse` (standard envelope)
Controllers return raw `ApiResponse` — no `ResponseEntity`. The HTTP status is carried inside the response and applied by [ApiResponseBodyAdvice.java](src/main/java/com/template/springboot/common/web/ApiResponseBodyAdvice.java) before serialization.

```java
new ApiResponse(data)                          // 200, message="OK"
new ApiResponse(data, "msg")                   // 200 + custom message
ApiResponse.created(data)                      // 201
ApiResponse.message("Deleted")                 // 200, no body data
ApiResponse.error("oops", errors)              // 400
ApiResponse.error("not found", null, NOT_FOUND)
new ApiResponse(data, "msg").setStatus(...)    // fluent
```

JSON shape:
```json
{
  "success": true,
  "message": "OK",
  "data": { ... },
  "errors": null,
  "timestamp": "2026-04-29T..."
}
```

### Pagination — automatic
Return `Page<T>` from your service. Pass it straight to `new ApiResponse(...)` and the advice converts it to a `PageResponse`:

```json
"data": {
  "content": [...],
  "page": 0, "size": 20,
  "totalElements": 137, "totalPages": 7,
  "first": true, "last": false, "empty": false
}
```

Query string: `?page=0&size=20&sort=createdAt,desc`

### Auditing — automatic
Every entity extending `BaseEntity` gets:
- `createdAt`, `updatedAt` — Hibernate timestamps
- `createdBy`, `updatedBy` — username from `SecurityContext` (or `"system"`)
- `deletedAt`, `deletedBy` — populated when soft-deleted
- `version` — optimistic locking

Every response extending `BaseResponse` ([source](src/main/java/com/template/springboot/common/dto/BaseResponse.java)) inherits the same six audit/lifecycle fields **without redeclaring them**. ModelMapper populates them through the inherited Lombok setters.

> Adding a new audit field (e.g. `archivedAt`) is a one-place edit: add it to `BaseEntity` + a Flyway migration + (optionally) `BaseResponse`. Every domain module picks it up automatically.

### Soft delete — automatic
- Every entity carries `deleted_at` / `deleted_by`. The `@SQLRestriction("deleted_at IS NULL")` annotation makes Hibernate append the filter to **every** query (including `findById`, joins, fetched collections, `existsBy*`, Specifications), so soft-deleted rows are invisible by default.
- Service `delete(...)` calls `entity.markDeleted(currentUserService.getUsername())` and `repo.save(entity)` — never `repo.deleteById(...)`.
- Restore: load via a native query, call `entity.restore()`, save.
- DB-level `UNIQUE` constraints still apply across both live and soft-deleted rows. Use partial / composite uniques per use case if you need re-use after deletion.

### Filtering — JPA Specifications
Each `*Specifications` class composes predicates from a filter DTO. Repositories extend `JpaSpecificationExecutor`. See [ProductSpecifications.java](src/main/java/com/template/springboot/modules/product/specification/ProductSpecifications.java).

### File uploads — `FileStorageService`
```java
FileUploadResponse uploaded = fileStorageService.save(multipartFile, "products");
String url = uploaded.getUrl();   // full URL pointing at GET /api/v1/files/...
```
- Subfolder is sanitized; path traversal is blocked.
- Filename becomes `<UUID>.<ext>` so uploads can't overwrite each other.
- `GET /api/v1/files/{subfolder}/{filename}` is **public** (so `<img src>` works without a token).
- `POST /api/v1/files/upload` requires authentication.

### Auto-Mapping (Entity ↔ DTO)
A central [ModelMapperConfig](src/main/java/com/template/springboot/config/ModelMapperConfig.java) configures ModelMapper with:
- `STANDARD` matching strategy + `setSkipNullEnabled(true)` (so `applyUpdate` never blanks unset fields)
- `setFieldMatchingEnabled(true)` + private-field access — works with Lombok-only fields and inherited fields from `BaseResponse`
- TypeMaps pre-registered for every entity → response pair
- Two collection-collapse converters: `Set<Role>` → `Set<String>` (role names), `Set<Permission>` → `Set<String>` (permission names)

**Contract: adding a field is a 3-edit operation.** Say you want `barcode` on `Product`:
1. `Product.java` — add `@Column private String barcode;`
2. `ProductRequest.java` — add `private String barcode;` (only if user-settable)
3. New `V<n>__product_barcode.sql` Flyway migration (entity ↔ schema mismatch fails startup; see below)

That's it. ModelMapper handles request → entity, JPA persists, ModelMapper handles entity → response. **No mapper edits, no service edits, no controller edits.**

> All request and response DTOs are Lombok classes (not records). ModelMapper requires setters to populate destination objects — records have none. JSON contract is identical either way.

A central injectable [GenericMapper](src/main/java/com/template/springboot/common/mapper/GenericMapper.java) wraps ModelMapper:
```java
public class FooController {
    private final GenericMapper mapper;
    public ApiResponse get(@PathVariable Long id) {
        return new ApiResponse(mapper.map(repo.findById(id).orElseThrow(), FooResponse.class));
    }
}
```

### Audit Logging (DB-backed)
Every API call is captured into `audit_logs` asynchronously. Implementation in [`modules/audit/`](src/main/java/com/template/springboot/modules/audit/).

What gets recorded per request:
- `requestId` (matches the `X-Request-Id` response header), `timestamp`, `durationMs`
- `userId`, `username` (from `SecurityContext`)
- `method`, `path`, `queryString`, `statusCode`
- `action`, `resourceType`, `resourceId` (from `@Auditable` annotation; SpEL supported on `resourceId`)
- `clientIp` (honors `X-Forwarded-For`), `userAgent`
- `requestBody`, `responseBody` — masked (`password`, `token`, `secret`, etc. → `***`) and truncated at `app.audit.max-body-length`
- `errorMessage` if the request blew up

Read API: `GET /api/v1/audit-logs` — paged, filterable by username/userId/method/path/action/resourceType/resourceId/statusCode/requestId/from/to. Gated by `AUDIT_READ` permission (granted to `ADMIN` by default).

Annotate handlers to label the action:
```java
@PostMapping
@Auditable(action = "ORDER_CREATE", resourceType = "Order")
public ApiResponse create(@Valid @RequestBody OrderRequest req) { ... }

@DeleteMapping("/{id}")
@Auditable(action = "ORDER_DELETE", resourceType = "Order", resourceId = "#id")  // SpEL
public ApiResponse delete(@PathVariable Long id) { ... }

@PostMapping("/healthz")
@Auditable(skip = true)   // opt out
public ApiResponse health() { ... }
```

Even without `@Auditable`, every API call is captured — the annotation only enriches the row with action/resource metadata.

**Toggles** (env-driven):
| Variable | Effect |
|---|---|
| `APP_AUDIT_ENABLED=false` | Disables filter, aspect, async executor, and read API entirely |
| `APP_AUDIT_EXPOSE_API=false` | Keeps capture on, hides `GET /api/v1/audit-logs` |
| `APP_AUDIT_CAPTURE_REQUEST_BODY=false` | Stop persisting request bodies |
| `APP_AUDIT_CAPTURE_RESPONSE_BODY=false` | Stop persisting response bodies |
| `APP_AUDIT_MAX_BODY_LENGTH=10000` | Truncate stored bodies above this size |

**Architecture choices**:
- **Async** via `@Async("auditExecutor")` on a bounded `ThreadPoolTaskExecutor` (core 2, max 8, queue 1k, `DiscardOldestPolicy`). Audit writes never block request threads.
- **`REQUIRES_NEW` transaction** so audit rows survive the caller's rollback.
- **Failures swallowed** — a broken audit never breaks the API.
- **Body masking** is JSON-aware: `{"password":"x"}` → `{"password":"***"}` on disk.

### Rate Limiting
Per-identity token-bucket limiter ([Bucket4j](https://github.com/bucket4j/bucket4j)) wired into the security chain right after JWT auth, so it keys by **username** when authenticated and falls back to **client IP** otherwise.

Implementation in [`common/ratelimit/`](src/main/java/com/template/springboot/common/ratelimit/) — three files: `RateLimitProperties`, `RateLimitService` (in-memory `ConcurrentHashMap<String, Bucket>`), `RateLimitFilter`.

**Two buckets**:
- **Default bucket** — covers all `/api/**` paths.
- **Auth bucket** — stricter limit applied to `/api/v1/auth/*` to mitigate credential stuffing.

**Response when allowed**:
```
HTTP/1.1 200 OK
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
```

**Response when blocked**:
```
HTTP/1.1 429 Too Many Requests
Retry-After: 47
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0

{ "success": false, "message": "Rate limit exceeded. Retry in 47s.", ... }
```

**Toggles** (env-driven, all in [.env.example](.env.example)):
| Variable | Default | Effect |
|---|---|---|
| `APP_RATE_LIMIT_ENABLED` | `true` | Master switch — false unregisters everything |
| `APP_RATE_LIMIT_CAPACITY` | `60` | Default bucket burst size |
| `APP_RATE_LIMIT_REFILL_TOKENS` | `60` | Tokens added each refill period |
| `APP_RATE_LIMIT_REFILL_PERIOD` | `PT1M` | ISO-8601 duration: `PT1S`, `PT1M`, `PT1H` |
| `APP_RATE_LIMIT_AUTH_CAPACITY` | `10` | Stricter bucket capacity for auth endpoints |
| `APP_RATE_LIMIT_AUTH_REFILL_TOKENS` | `10` | Refill rate for auth bucket |
| `APP_RATE_LIMIT_AUTH_REFILL_PERIOD` | `PT1M` | Refill period for auth bucket |

> **Cluster note**: The default `RateLimitService` uses a `ConcurrentHashMap` — fine for single-instance. For multi-instance, replace it with `bucket4j-redis`'s `LockBasedProxyManager`. The filter signature stays identical.

### Caching
Spring's `@Cacheable` / `@CachePut` / `@CacheEvict` abstraction is wired in. The annotations are **provider-agnostic** — switching the backend (Caffeine → Redis) is an env + dependency change, not a code change. See [Switching Cache to Redis](#switching-cache-to-redis) for the migration.

**Implementation lives in** [`common/cache/`](src/main/java/com/template/springboot/common/cache/):
- [CacheConfig.java](src/main/java/com/template/springboot/common/cache/CacheConfig.java) — `@EnableCaching` + Caffeine `CacheManager` bean (guarded by `@ConditionalOnProperty(spring.cache.type=caffeine)` so a Redis swap skips it cleanly).
- [CacheProperties.java](src/main/java/com/template/springboot/common/cache/CacheProperties.java) — per-cache TTL + max-size from `app.cache.*` in [application.yml](src/main/resources/application.yml).

**Caches wired by default:**

| Cache name     | Used by                                                                 | TTL  | Max | Eviction triggers                                                            |
|----------------|--------------------------------------------------------------------------|------|-----|------------------------------------------------------------------------------|
| `products`     | `ProductServiceImpl` — `getById`                                        | 15m  | 5k  | `update` / `delete` / `placeOrder` (any stock change)                        |
| `users`        | `UserServiceImpl` — `getById`                                           | 10m  | 5k  | `update` / `assignRoles` / `delete` (and these also evict `userDetails`)     |
| `roles`        | `RoleServiceImpl` — `getById`                                           | 1h   | 500 | `update` / `assignPermissions` / `delete` (and evict all `userDetails`)      |
| `permissions`  | `PermissionServiceImpl` — `getById`                                     | 1h   | 500 | `update` / `delete` (and evict all `roles` + all `userDetails`)              |
| `userDetails`  | `CustomUserDetailsService.loadUserByUsername` — **every auth'd request** | 5m   | 10k | Any user/role/permission mutation (broad eviction keeps RBAC correct)        |

**Cross-cache eviction strategy:** RBAC changes (a role gaining a permission, a user being reassigned) evict all `userDetails` entries — we don't know which usernames are affected, and authorization correctness wins over hit rate.

**Configuration:** per-cache values are tuned in [application.yml](src/main/resources/application.yml) under `app.cache.*`. They're intentionally **not** exposed as env vars — these are app-internal tradeoffs, not deployment knobs. The only cache-related env var is `SPRING_CACHE_TYPE`.

**Add caching to a new service:**
```java
@Cacheable(value = "orders", key = "#id")              // populate on miss
@CachePut(value = "orders", key = "#id")               // refresh after write
@CacheEvict(value = "orders", key = "#id")             // remove on delete
@CacheEvict(value = "orders", allEntries = true)       // nuclear option
```
Add a matching block under `app.cache.caches.orders` in [application.yml](src/main/resources/application.yml). If you skip the yml entry, the `defaults` block applies.

**Inspect contents at runtime:** add `caches` to `MANAGEMENT_ENDPOINTS` and hit `GET /actuator/caches`.

### Validation
Use Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) on DTOs and `@Valid` on the controller parameter. Failures are caught by `GlobalExceptionHandler.handleValidation`:
```json
{ "success": false, "message": "Validation failed",
  "errors": { "email": "must be a well-formed email address" } }
```

### Exceptions
Throw any of:
- `ResourceNotFoundException(resource, id)` → 404
- `BadRequestException(msg)` → 400
- `DuplicateResourceException(msg)` → 409

Custom mapping lives in [GlobalExceptionHandler.java](src/main/java/com/template/springboot/common/exception/GlobalExceptionHandler.java).

---

## Configuration Reference

Every value in [application.yml](src/main/resources/application.yml) is `${VAR:default}`. spring-dotenv loads `.env` at startup; OS env vars override `.env`; `application.yml` defaults are the last resort.

**Lookup order (highest priority first)**: OS env → `.env` → `application.yml` default.

### Application
| Variable | Default | Purpose |
|---|---|---|
| `APP_NAME` | `springboot` | App name |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile selector — `dev` or `prod` |
| `SERVER_PORT` | `8080` | HTTP port |

### Database
| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | local MySQL | JDBC URL |
| `DB_USERNAME` | `root` | DB user |
| `DB_PASSWORD` | _(empty)_ | DB pass |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | Use `org.postgresql.Driver` for Postgres |
| `DB_POOL_MAX_SIZE` | `10` | HikariCP `maximum-pool-size` |
| `DB_POOL_MIN_IDLE` | `2` | HikariCP `minimum-idle` |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` | HikariCP `connection-timeout` |

### JPA / Hibernate
| Variable | Default | Purpose |
|---|---|---|
| `JPA_DDL_AUTO` | `validate` | **Don't change** — Flyway owns the schema |
| `JPA_OPEN_IN_VIEW` | `false` | Anti-pattern in production; keep off |
| `JPA_TIME_ZONE` | `UTC` | Hibernate timestamp TZ |
| `JPA_FORMAT_SQL` | `true` | Pretty-print logged SQL |
| `JPA_SHOW_SQL` | `true` (dev) / `false` (prod) | Log every SQL statement |

### Flyway
| Variable | Default | Purpose |
|---|---|---|
| `FLYWAY_ENABLED` | `true` | Master switch |
| `FLYWAY_BASELINE_ON_MIGRATE` | `true` | First run on existing DB won't fail |
| `FLYWAY_LOCATIONS` | `classpath:db/migration` | Migration folder |

### JWT
| Variable | Default | Purpose |
|---|---|---|
| `APP_SECURITY_JWT_SECRET` | placeholder | **Override in prod**: `openssl rand -base64 32` |
| `APP_SECURITY_JWT_ISSUER` | `springboot-template` | JWT `iss` |
| `APP_SECURITY_JWT_ACCESS_TTL_MINUTES` | `60` | Access token lifetime |
| `APP_SECURITY_JWT_REFRESH_TTL_DAYS` | `14` | Refresh token lifetime |

### File storage
| Variable | Default | Purpose |
|---|---|---|
| `APP_STORAGE_BASE_PATH` | `uploads` | Disk dir for uploads (gitignored) |
| `MULTIPART_MAX_FILE_SIZE` | `10MB` | Per-file upload limit |
| `MULTIPART_MAX_REQUEST_SIZE` | `12MB` | Whole multipart envelope limit |

### Audit logging
| Variable | Default | Purpose |
|---|---|---|
| `APP_AUDIT_ENABLED` | `true` | Master switch |
| `APP_AUDIT_EXPOSE_API` | `true` | Toggle the read endpoint independently |
| `APP_AUDIT_AUDIT_ALL` | `true` | Audit every API call vs only `@Auditable` ones |
| `APP_AUDIT_CAPTURE_REQUEST_BODY` | `true` | Persist request bodies |
| `APP_AUDIT_CAPTURE_RESPONSE_BODY` | `true` | Persist response bodies |
| `APP_AUDIT_MAX_BODY_LENGTH` | `10000` | Truncate above this many chars |

### Caching
| Variable | Default | Purpose |
|---|---|---|
| `SPRING_CACHE_TYPE` | `caffeine` | Backend selector — `caffeine` (in-memory), `redis`, or `none`. See [Switching Cache to Redis](#switching-cache-to-redis). Per-cache TTLs live in `application.yml` under `app.cache.*`, not env. |

### Rate limiting
| Variable | Default | Purpose |
|---|---|---|
| `APP_RATE_LIMIT_ENABLED` | `true` | Master switch |
| `APP_RATE_LIMIT_CAPACITY` | `60` | Default bucket capacity |
| `APP_RATE_LIMIT_REFILL_TOKENS` | `60` | Default refill amount |
| `APP_RATE_LIMIT_REFILL_PERIOD` | `PT1M` | Default refill period (ISO-8601) |
| `APP_RATE_LIMIT_AUTH_CAPACITY` | `10` | Auth bucket capacity |
| `APP_RATE_LIMIT_AUTH_REFILL_TOKENS` | `10` | Auth refill amount |
| `APP_RATE_LIMIT_AUTH_REFILL_PERIOD` | `PT1M` | Auth refill period |

### OpenAPI / Swagger
| Variable | Default | Purpose |
|---|---|---|
| `SPRINGDOC_SWAGGER_UI_PATH` | `/swagger-ui.html` | Swagger UI route |
| `SPRINGDOC_API_DOCS_PATH` | `/v3/api-docs` | OpenAPI JSON route |

### Logging
| Variable | Default | Purpose |
|---|---|---|
| `LOG_DIR` | `logs` | Logback output dir (gitignored) |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_LEVEL_APP` | `DEBUG` | `com.template.springboot.*` log level |

### Actuator
| Variable | Default | Purpose |
|---|---|---|
| `MANAGEMENT_ENDPOINTS` | `health,info,metrics` | Comma-separated endpoint list |
| `MANAGEMENT_HEALTH_DETAILS` | `when-authorized` | `never` / `when-authorized` / `always` |

### Profiles
- `dev` — local MySQL fallbacks, SQL logging on, all the noise.
- `prod` — `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are required from env, SQL logging off.

### Recommended env overrides for prod
Don't ship `.env` to production — inject secrets through your platform (systemd `EnvironmentFile=`, Kubernetes Secrets + `envFrom`, ECS task definitions, etc.):

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://prod-host:5432/app
DB_USERNAME=app_user
DB_PASSWORD=$(secrets-manager get db-pass)
DB_DRIVER=org.postgresql.Driver
APP_SECURITY_JWT_SECRET=$(openssl rand -base64 32)
APP_BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -base64 24)
APP_STORAGE_BASE_PATH=/var/data/uploads
LOG_DIR=/var/log/springboot
APP_RATE_LIMIT_CAPACITY=300
APP_RATE_LIMIT_AUTH_CAPACITY=5
```

---

## Built-in Endpoints

| Method | Path | Auth | Permission |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | public | — |
| `POST` | `/api/v1/auth/login` | public | — |
| `POST` | `/api/v1/auth/refresh` | public | — |
| `GET`  | `/api/v1/users/me` | yes | — |
| `GET`  | `/api/v1/users` | yes | `USER_READ` |
| `GET`  | `/api/v1/users/{id}` | yes | `USER_READ` |
| `PUT`  | `/api/v1/users/{id}` | yes | `USER_WRITE` |
| `POST` | `/api/v1/users/{id}/roles` | yes | `ROLE_WRITE` |
| `DELETE` | `/api/v1/users/{id}` | yes | `USER_DELETE` |
| `GET`  | `/api/v1/roles` | yes | `ROLE_READ` |
| `POST` | `/api/v1/roles` | yes | `ROLE_WRITE` |
| `GET`  | `/api/v1/roles/{id}` | yes | `ROLE_READ` |
| `PUT`  | `/api/v1/roles/{id}` | yes | `ROLE_WRITE` |
| `POST` | `/api/v1/roles/{id}/permissions` | yes | `ROLE_WRITE` |
| `DELETE` | `/api/v1/roles/{id}` | yes | `ROLE_DELETE` |
| `GET`  | `/api/v1/permissions` | yes | `PERMISSION_READ` |
| `POST` | `/api/v1/permissions` | yes | `PERMISSION_WRITE` |
| `PUT`  | `/api/v1/permissions/{id}` | yes | `PERMISSION_WRITE` |
| `DELETE` | `/api/v1/permissions/{id}` | yes | `PERMISSION_WRITE` |
| `POST` | `/api/v1/products` (multipart) | yes | `PRODUCT_WRITE` |
| `GET`  | `/api/v1/products` | yes | `PRODUCT_READ` |
| `GET`  | `/api/v1/products/{id}` | yes | `PRODUCT_READ` |
| `PUT`  | `/api/v1/products/{id}` (multipart) | yes | `PRODUCT_WRITE` |
| `DELETE` | `/api/v1/products/{id}` | yes | `PRODUCT_DELETE` |
| `POST` | `/api/v1/files/upload` | yes | — |
| `GET`  | `/api/v1/files/{subfolder}/{filename}` | **public** | — |
| `GET`  | `/api/v1/audit-logs` | yes | `AUDIT_READ` |
| `GET`  | `/api/v1/audit-logs/{id}` | yes | `AUDIT_READ` |

---

## Database Migrations

- All schema changes go through Flyway under `src/main/resources/db/migration/`.
- File naming: `V<N>__<description>.sql` (note the **double underscore**).
- Migrations are **immutable** once they've run on any environment — never edit a checked-in `V<N>__*.sql` (Flyway records the file checksum and refuses to start on mismatch).
- For changes after the fact, write a new `V<N+1>__*.sql`.
- `JPA_DDL_AUTO=validate` ensures Hibernate's view of the schema must match what Flyway built — adding an `@Entity` field without a migration **fails startup**.

Existing migrations:
- `V1__init_security_schema.sql` — users, roles, permissions, join tables, seed roles + perms
- `V2__products_and_permissions.sql` — products table, product perms, role grants
- `V3__role_permission_admin_perms.sql` — role/permission management perms for ADMIN
- `V4__product_image_url.sql` — adds `image_url` column to products
- `V5__soft_delete_columns.sql` — adds `deleted_at` / `deleted_by` to all audited tables
- `V6__audit_logs.sql` — `audit_logs` table + `AUDIT_READ` permission for ADMIN

To inspect what's been applied:
```sql
SELECT version, description, success, installed_on
  FROM flyway_schema_history
  ORDER BY installed_rank;
```

---

## Logging

Console + four rolling files in `${LOG_DIR}` (3-day retention, 50 MB per file, 500 MB total cap, gzipped archives in `logs/archived/`):

| File | What's there |
|---|---|
| `logs/springboot.log` | Everything at INFO+ |
| `logs/error.log` | WARN+ only — fast triage |
| `logs/security.log` | Spring Security + your `common.security.*` logs |
| `logs/sql.log` | Hibernate SQL + bound parameters |

Use Lombok's `@Slf4j` and you get `log` for free:
```java
@Slf4j @Service @RequiredArgsConstructor
public class FooServiceImpl implements FooService {
    public Foo create(FooRequest req) {
        log.info("Creating foo name={}", req.getName());     // always use {} placeholders
        // log.error("...", ex);                              // pass exception as last arg
    }
}
```

Every log line carries `requestId` and `username` from MDC ([RequestLoggingFilter.java](src/main/java/com/template/springboot/common/web/RequestLoggingFilter.java)). The same id is returned in the `X-Request-Id` response header — grep the log files by it to follow a single request end-to-end.

Levels per package live in [logback-spring.xml](src/main/resources/logback-spring.xml). Quick override: `LOG_LEVEL_APP=DEBUG`.

---

## Switching Cache to Redis

The cache layer is intentionally provider-agnostic. Flipping from in-memory Caffeine to Redis touches **two files** — [pom.xml](pom.xml) and `.env` — plus one optional helper class if you want per-cache TTLs and JSON serialization (recommended).

### Why the swap is safe to do at any point
- Every `@Cacheable` / `@CachePut` / `@CacheEvict` in the codebase references **cache names** (`"products"`, `"users"`, `"roles"`, `"permissions"`, `"userDetails"`) — never a provider type.
- The Caffeine `CacheManager` bean in [CacheConfig.java](src/main/java/com/template/springboot/common/cache/CacheConfig.java) is guarded by `@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)`. Set `spring.cache.type=redis` and that bean is silently skipped.
- Spring Boot's autoconfiguration detects the Redis starter on the classpath + the property, and provisions its own `RedisCacheManager` automatically.
- Caches are **derived state** — nothing in Redis needs to be migrated. Empty cold cache is fine; it warms on first reads.

### Step 1 — Start a Redis instance

For local dev, the fastest path is Docker:

```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

Or `docker-compose`:

```yaml
services:
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    restart: unless-stopped
```

For production, point at your managed Redis (ElastiCache, MemoryStore, Upstash, Redis Cloud, etc.).

### Step 2 — Add the Spring Data Redis starter to [pom.xml](pom.xml)

Inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

(Spring Boot's BOM manages the version. The starter pulls in the Lettuce client by default.)

### Step 3 — Flip the env

Edit `.env` (already contains commented placeholders — uncomment them):

```bash
SPRING_CACHE_TYPE=redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
# SPRING_DATA_REDIS_PASSWORD=secret      # uncomment if your Redis requires auth
# SPRING_DATA_REDIS_DATABASE=0           # 0–15, defaults to 0
```

In production, inject these via your platform's secret/env mechanism (Kubernetes Secrets, systemd `EnvironmentFile=`, ECS task definition, etc.).

### Step 4 — Restart the app

```bash
./mvnw spring-boot:run
```

That's the minimum migration. The app starts, `@Cacheable` writes/reads land in Redis instead of process memory, every other behaviour is unchanged.

### Step 5 — Verify it's hitting Redis

```bash
# After a cached request, e.g. GET /api/v1/products/1
redis-cli KEYS '*'
# Expected output:
#   1) "products::1"

redis-cli GET 'products::1'
# Expected: the serialized ProductResponse blob

redis-cli TTL 'products::1'
# Expected: a positive integer (seconds remaining), or -1 if no TTL configured (see Step 6)
```

You can also enable the actuator caches endpoint by adding `caches` to `MANAGEMENT_ENDPOINTS` in `.env`, then:
```bash
curl http://localhost:8080/actuator/caches
```

### Step 6 — Recommended: JSON serialization + per-cache TTLs

Out of the box, Spring Boot's Redis auto-configuration has two limitations you'll hit immediately:

1. **JDK serialization is the default** → every cached class (e.g. `ProductResponse`, `CustomUserDetails`) must `implements Serializable`. Cached entries break when you refactor those classes (incompatible `serialVersionUID`).
2. **One global TTL** → the per-cache TTLs in `app.cache.caches.*.ttl` (which Caffeine honoured perfectly) are ignored. Every cache uses the same default.

Fix both with one helper class. Create `src/main/java/com/template/springboot/common/cache/RedisCacheConfig.java`:

```java
package com.template.springboot.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@ConditionalOnClass(RedisCacheManager.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
class RedisCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer cacheCustomizer(CacheProperties properties) {
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getDefaults().getTtl())
                .serializeValuesWith(SerializationPair.fromSerializer(json));

        return builder -> {
            builder.cacheDefaults(defaults);
            properties.getCaches().forEach((name, spec) ->
                    builder.withCacheConfiguration(name, defaults.entryTtl(spec.getTtl())));
        };
    }
}
```

This class:
- Only compiles when `spring-boot-starter-data-redis` is on the classpath (`@ConditionalOnClass`), so you can keep it in source even before you've added the starter — it's dormant.
- Only activates when `spring.cache.type=redis`, so it stays out of the way when Caffeine is active.
- Reads the same `app.cache.*` block from [application.yml](src/main/resources/application.yml) that Caffeine reads — **no duplication of TTL config**.
- Uses JSON serialization so cached blobs survive class refactors and are inspectable with `redis-cli GET <key>`.

### Rolling back to Caffeine

```bash
# 1. Flip the env back
SPRING_CACHE_TYPE=caffeine

# 2. (Optional) Remove the Redis dependency from pom.xml if you don't want the jar in the build.
#    Leaving it in is harmless — RedisCacheConfig.java stays dormant under the conditionals.

# 3. Restart.
```

No data migration. Caches are derived state — Caffeine starts cold and warms up on first reads.

### Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `Cannot get Jedis connection` or `Unable to connect to Redis` on startup | Redis isn't running, or host/port wrong. Check `docker ps` and `redis-cli -h $HOST -p $PORT PING`. |
| `java.io.NotSerializableException: ...` thrown on a cache write | You skipped Step 6. Either `implements Serializable` on the cached class, or add `RedisCacheConfig.java`. |
| Cached values appear as binary garbage in `redis-cli GET ...` | Default JDK serialization is active. Add `RedisCacheConfig.java` for JSON serialization. |
| All caches expire at the same time | The per-cache TTLs aren't being applied. Add `RedisCacheConfig.java` (per-cache `withCacheConfiguration` calls). |
| Cache reads are slower than expected | Verify network latency to Redis (`redis-cli --latency`). Caffeine is in-process (~µs); Redis adds a network round-trip (~ms in same DC, more across regions). |
| Want to clear everything | `redis-cli FLUSHDB` (current DB only) or `FLUSHALL` (all DBs). For one cache: `redis-cli --scan --pattern 'products::*' | xargs redis-cli DEL`. |

---

## Build & Test

```bash
./mvnw -q -DskipTests compile        # compile only
./mvnw -q -DskipTests package        # build runnable jar (target/springboot-*.jar)
./mvnw test                          # run tests (needs running MySQL)
./mvnw spring-boot:run               # start app
java -jar target/springboot-*.jar    # run packaged jar
```

Quick rate-limit sanity check (after starting the app):
```bash
for i in $(seq 1 12); do
  curl -i -X POST http://localhost:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"usernameOrEmail":"x","password":"y"}' \
    | grep -E 'HTTP|X-RateLimit|Retry-After'
done
```
You should see `X-RateLimit-Remaining` count down and a `429` after `APP_RATE_LIMIT_AUTH_CAPACITY` attempts.

---

## What's Intentionally NOT in the Box

So you know when to add them:

- **Refresh-token rotation / revocation table** — refresh tokens are stateless. Add a `revoked_tokens` table if you need server-side invalidation.
- **Account lockout on failed login** — rate limiting blunts credential stuffing, but a per-user lockout counter is a separate concern.
- **Email / password reset** — `spring-boot-starter-mail` + a `PasswordResetToken` table aren't wired.
- **Cluster-safe rate limiter** — current rate limiter is in-memory. Swap to `bucket4j-redis` for multi-instance. (The **cache** abstraction is already cluster-safe — flip to Redis per [Switching Cache to Redis](#switching-cache-to-redis).)
- **Testcontainers / hermetic test profile** — only `contextLoads` exists. Add Testcontainers for hermetic CI.
- **CI workflow** — `.github/workflows/` is empty.
- **Dockerfile / docker-compose.yml** — no one-command spin-up of app + DB.
- **Prometheus / OpenTelemetry** — only basic actuator metrics. Add `micrometer-registry-prometheus` + tracing.
- **Multi-tenancy** — no tenant context, no schema strategy.
