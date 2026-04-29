# Spring Boot API Template

A reusable Spring Boot 4 / Java 17 starter for building secure REST APIs. Comes pre-wired with JWT authentication, RBAC (roles + permissions), JPA auditing, global exception handling, file uploads, structured logging, OpenAPI docs, and Flyway migrations.

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Quick Start](#quick-start)
3. [Default Credentials](#default-credentials)
4. [Project Structure](#project-structure)
5. [Adding a New API Module](#adding-a-new-api-module)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
7. [Configuration Reference](#configuration-reference)
8. [Built-in Endpoints](#built-in-endpoints)
9. [Database Migrations](#database-migrations)
10. [Logging](#logging)

---

## Tech Stack

| Concern | Choice |
|---|---|
| Runtime | Spring Boot 4.0.x · Spring Framework 7 · Java 17 |
| Web | `spring-boot-starter-webmvc` (servlet, blocking) |
| Persistence | Spring Data JPA · Hibernate · MySQL · Flyway |
| Security | Spring Security 7 · JJWT 0.12 (HS256) |
| AOP | aspectjweaver (annotation-driven authorization) |
| JSON | Jackson 3 (`tools.jackson.databind`) |
| Mapping | ModelMapper 3.2 (entity ↔ DTO; records use static factories) |
| Validation | Jakarta Bean Validation |
| Docs | springdoc-openapi 3 (Swagger UI) |
| Logging | Logback (rolling files, 3-day retention) |
| Build | Maven |

---

## Quick Start

### 1. Prerequisites
- JDK 17+
- MySQL 8 running locally (or set `DB_URL` to an external instance)
- Maven (`./mvnw` wrapper is included — no separate install needed)

### 2. Configure the database
The dev profile auto-creates the DB. Defaults are in [application.yml](src/main/resources/application.yml):

```yaml
url: jdbc:mysql://localhost:3306/springboot_dev?createDatabaseIfNotExist=true
username: root
password: ""
```

Override via env vars: `DB_USERNAME=root DB_PASSWORD=secret`.

### 3. Run
```bash
./mvnw spring-boot:run
```

Flyway runs migrations on startup. `DataInitializer` then bootstraps the default ADMIN user if one doesn't exist. The app listens on `:8080`.

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
| Role | `ADMIN` (every permission) |

**Change these before any non-local deployment.** Override at startup:
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
├── config/                     ← framework wiring
│   ├── SecurityConfig          (JWT filter, public paths, CORS)
│   ├── JpaAuditingConfig       (@CreatedBy / @CreatedDate)
│   ├── ModelMapperConfig
│   ├── OpenApiConfig           (Swagger bearer scheme)
│   └── DataInitializer         (bootstraps default admin)
├── common/                     ← reusable infrastructure (don't copy, depend on)
│   ├── audit/                  BaseEntity · AuditorAwareImpl
│   ├── dto/                    ApiResponse · PageResponse
│   ├── exception/              GlobalExceptionHandler · custom exceptions
│   ├── security/               JwtService · JwtAuthenticationFilter · SecurityUtils
│   │                           HasRole / HasPermission annotations + AuthorizationAspect
│   │                           CustomUserDetails(Service) · entry-point/access-denied handlers
│   ├── specification/          SpecificationBuilder helpers
│   └── web/                    ApiResponseBodyAdvice (status injection)
│                               RequestLoggingFilter (MDC requestId / username)
└── modules/                    ← every feature lives here, identical shape
    ├── auth/                   controller · dto · enums · service · serviceImpl
    ├── file/                   generic file upload + serve service
    ├── permission/             entity · enums · repository · dto · mapper · service · serviceImpl · specification · controller
    ├── role/                   same shape as permission
    ├── user/                   same shape + specification (filtering)
    └── product/                full reference module — multipart + image, full CRUD
```

Resources:
```
src/main/resources/
├── application.yml             (shared + dev/prod profiles)
├── logback-spring.xml          (rolling per-category log files)
└── db/migration/               (Flyway: V1__init_security_schema.sql, V2__products_..., V3, V4)
```

---

## Adding a New API Module

This is the most repeated task. The pattern is the same for everything.

### Recipe (let's say you're adding `Order`)

#### 1. Scaffold the folder structure
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

#### 2. Entity — extends `BaseEntity` for free auditing
```java
@Entity @Table(name = "orders")
@Getter @Setter @NoArgsConstructor
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

#### 4. DTOs — records with Jakarta Validation
```java
public record OrderRequest(@NotBlank String reference, @NotNull BigDecimal total) {}

public record OrderResponse(Long id, String reference, BigDecimal total,
                            Instant createdAt, Instant updatedAt) {
    public static OrderResponse from(Order o) {       // ← required for ModelMapper
        return new OrderResponse(o.getId(), o.getReference(), o.getTotal(),
                                 o.getCreatedAt(), o.getUpdatedAt());
    }
}
```

> **Important:** ModelMapper can't instantiate Java records. Always provide a static `from(Entity)` factory on response records — the mapper calls it. See `ProductMapper.toResponse`.

#### 5. Mapper — `@Component`, Lombok DI
```java
@Component @RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;
    public Order toEntity(OrderRequest r)              { return modelMapper.map(r, Order.class); }
    public void applyUpdate(OrderRequest r, Order o)   { modelMapper.map(r, o); }
    public OrderResponse toResponse(Order o)           { return OrderResponse.from(o); }
}
```

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

#### 8. Controller — `@HasPermission` + `ApiResponse` (no `ResponseEntity`)
```java
@RestController @RequestMapping("/api/v1/orders")
@RequiredArgsConstructor @Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @HasPermission(PermissionName.ORDER_WRITE)
    public ApiResponse create(@Valid @RequestBody OrderRequest req) {
        return ApiResponse.created(orderService.create(req));
    }

    @GetMapping
    @HasPermission(PermissionName.ORDER_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @ParameterObject Pageable pageable) {
        return new ApiResponse(orderService.search(new OrderFilter(q), pageable));
    }
    // ...
}
```

`Pageable` automatically reads `page=`, `size=`, `sort=` from the query string. The advice wraps `Page<T>` in a `PageResponse` automatically.

#### 9. Add permission constants to `PermissionName`
```java
public static final String ORDER_READ   = "ORDER_READ";
public static final String ORDER_WRITE  = "ORDER_WRITE";
public static final String ORDER_DELETE = "ORDER_DELETE";
```

#### 10. Flyway migration `V<n>__orders.sql`
Create the table with the audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`, `version`), seed the new permissions, and grant them to roles:

```sql
CREATE TABLE orders ( id BIGINT NOT NULL AUTO_INCREMENT, ..., PRIMARY KEY (id) );

INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('ORDER_READ',   'Read orders',   NOW(6), NOW(6), 'system', 'system'),
    ('ORDER_WRITE',  'Modify orders', NOW(6), NOW(6), 'system', 'system'),
    ('ORDER_DELETE', 'Delete orders', NOW(6), NOW(6), 'system', 'system');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('ORDER_READ', 'ORDER_WRITE', 'ORDER_DELETE');
```

That's it. Restart, log in as ADMIN, hit `/api/v1/orders`.

---

## Cross-Cutting Concerns

### Authentication (JWT)
- Every request goes through [JwtAuthenticationFilter.java](src/main/java/com/template/springboot/common/security/JwtAuthenticationFilter.java).
- Send `Authorization: Bearer <accessToken>` on protected endpoints.
- The token's `uid` and authorities (roles + permissions) populate `SecurityContext`.
- Tokens are issued by `/api/v1/auth/login`. Refresh via `/api/v1/auth/refresh`.

### Authorization — `@HasRole` / `@HasPermission`
Custom annotations enforced by [AuthorizationAspect.java](src/main/java/com/template/springboot/common/security/AuthorizationAspect.java). No `@PreAuthorize`, no SpEL — just the bare permission/role name.

```java
@HasPermission(PermissionName.PRODUCT_READ)   // 403 if missing
@HasRole(RoleName.ADMIN)                      // 403 if missing
```

Apply at method or class level (method wins when both present). Fails authentication → 401, fails authorization → 403, both responses are JSON via `GlobalExceptionHandler`.

### Current user context — `SecurityUtils`
```java
SecurityUtils.getCurrentUserId()        // Optional<Long>
SecurityUtils.getCurrentUsername()      // Optional<String>
SecurityUtils.getCurrentAuthorities()   // Set<String>
SecurityUtils.hasAuthority("X")
SecurityUtils.hasRole("X")
```

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

### Soft delete — automatic
- Every entity with `BaseEntity` carries `deleted_at` / `deleted_by`. The `@SQLRestriction("deleted_at IS NULL")` annotation on each `@Entity` class makes Hibernate append the filter to **every** query (including `findById`, `findAll`, joins, fetched collections, `existsBy*`, Specifications), so soft-deleted rows are invisible to the application by default.
- Service `delete(...)` methods call `entity.markDeleted(SecurityUtils.getCurrentUsername())` and `repo.save(entity)` — never `repo.deleteById(...)`. That records who deleted it and when.
- To restore: load via a native query (since `findById` won't see it), call `entity.restore()`, save.
- DB-level `UNIQUE` constraints on natural keys (`username`, `email`, `sku`, etc.) still apply across both live and soft-deleted rows. If you need to allow re-using a key after deletion, change the schema to a partial / composite unique constraint per use case.

### Filtering — JPA Specifications
Each `*Specifications` class composes predicates from a filter DTO. The repository extends `JpaSpecificationExecutor`, so the service just calls `repository.findAll(spec, pageable)`. See [ProductSpecifications.java](src/main/java/com/template/springboot/modules/product/specification/ProductSpecifications.java).

### File uploads — `FileStorageService`
Inject and call:
```java
FileUploadResponse uploaded = fileStorageService.save(multipartFile, "products");
String url = uploaded.url();   // full URL pointing at GET /api/v1/files/...
```
- Subfolder is sanitized; path traversal is blocked.
- Filename becomes `<UUID>.<ext>` so users can't overwrite each other's files.
- `GET /api/v1/files/{subfolder}/{filename}` is **public** (so `<img src>` works without a token).
- `POST /api/v1/files/upload` requires authentication.

### Validation
Use Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) on DTOs and `@Valid` on the controller parameter. Failures are caught by `GlobalExceptionHandler.handleValidation` and returned as:
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

All settings live in [application.yml](src/main/resources/application.yml). Override via env var or CLI flag. The yml format `app.security.jwt.secret` becomes env var `APP_SECURITY_JWT_SECRET`.

| Key | Default | Purpose |
|---|---|---|
| `spring.profiles.active` | `dev` | Profile selector |
| `spring.datasource.url` | local MySQL | DB connection |
| `spring.datasource.username` / `password` | `root` / `""` | DB creds |
| `spring.jpa.hibernate.ddl-auto` | `validate` | **Don't change** — Flyway owns schema |
| `spring.servlet.multipart.max-file-size` | `10MB` | Per-file upload limit |
| `spring.servlet.multipart.max-request-size` | `12MB` | Whole multipart envelope limit |
| `spring.flyway.locations` | `classpath:db/migration` | Migration folder |
| `app.security.jwt.secret` | placeholder (32-byte base64) | **Override in prod** |
| `app.security.jwt.issuer` | `springboot-template` | JWT `iss` |
| `app.security.jwt.access-token-ttl-minutes` | `60` | Access token lifetime |
| `app.security.jwt.refresh-token-ttl-days` | `14` | Refresh token lifetime |
| `app.storage.base-path` | `uploads` | Disk dir for file uploads (gitignored) |
| `app.bootstrap.admin.username` / `email` / `password` | `admin` / `admin@gmail.com` / `admin123` | Initial admin |
| `logging.file.path` | `logs` | Logback output dir (gitignored) |
| `server.port` | `8080` | HTTP port |

### Profiles
- `dev` — local MySQL, SQL logging on, all the noise.
- `prod` — DB pulled from env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), SQL logging off.

### Recommended env overrides for prod
```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://prod-host:3306/app
DB_USERNAME=app_user
DB_PASSWORD=$(secrets-manager get db-pass)
APP_SECURITY_JWT_SECRET=$(openssl rand -base64 32)
APP_BOOTSTRAP_ADMIN_PASSWORD=$(openssl rand -base64 24)
APP_STORAGE_BASE_PATH=/var/data/uploads
LOG_DIR=/var/log/springboot
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

---

## Database Migrations

- All schema changes go through Flyway under `src/main/resources/db/migration/`.
- File naming: `V<N>__<description>.sql` (note the **double underscore**).
- Migrations are immutable once they've run on any environment — never edit a checked-in `V<N>__*.sql`.
- For changes after the fact, write a new `V<N+1>__*.sql`.
- `spring.jpa.hibernate.ddl-auto=validate` ensures Hibernate's view of the schema must match what Flyway built.

Existing migrations:
- `V1__init_security_schema.sql` — users, roles, permissions, join tables, seed roles + perms
- `V2__products_and_permissions.sql` — products table, product perms, role grants
- `V3__role_permission_admin_perms.sql` — role/permission management perms for ADMIN
- `V4__product_image_url.sql` — adds `image_url` column to products

---

## Logging

Console + four rolling files in `logs/` (3-day retention, 50 MB per file, 500 MB total cap, gzipped archives in `logs/archived/`):

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
        log.info("Creating foo name={}", req.name());     // always use {} placeholders
        // log.error("...", ex);                          // pass exception as last arg
    }
}
```

Every log line carries `requestId` and `username` from MDC ([RequestLoggingFilter.java](src/main/java/com/template/springboot/common/web/RequestLoggingFilter.java)). The same id is returned in the `X-Request-Id` response header — grep the log files by it to follow a single request end-to-end.

Levels per package live in [logback-spring.xml](src/main/resources/logback-spring.xml). Quick override via env: `LOGGING_LEVEL_COM_TEMPLATE_SPRINGBOOT=DEBUG`.

---

## Build & Test

```bash
./mvnw -q -DskipTests compile        # compile only
./mvnw -q -DskipTests package        # build runnable jar (target/springboot-*.jar)
./mvnw test                          # run tests (needs running MySQL)
./mvnw spring-boot:run               # start app
java -jar target/springboot-*.jar    # run packaged jar
```

---

## What's Intentionally NOT in the Box

So you know when to add them:

- **Refresh-token rotation / revocation table** — refresh tokens are stateless. Add a `revoked_tokens` table if you need server-side invalidation.
- **Rate limiting** — bring `bucket4j` or put rate limits at the edge.
- **Email / password reset** — not wired.
- **Testcontainers / `test` profile** — `contextLoads` test still needs a real MySQL. Add Testcontainers for hermetic CI.
- **Distributed cache / session store** — auth is stateless JWT, no session.
- **CI workflow** — only `.github/` is empty.
