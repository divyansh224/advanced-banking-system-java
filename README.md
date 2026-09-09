# 🏦 SecureBank — Online Banking System

A full-stack banking web application built with **Spring Boot / Spring Security / Hibernate (JPA) / MySQL** on the backend and **HTML5 / Bootstrap 5 / vanilla JavaScript** on the frontend.

Users can register, log in, open accounts, check balances, transfer funds between accounts, and view their transaction history — all backed by a properly layered REST API.

---

## 1. Tech Stack

| Layer          | Technology                                             |
|----------------|---------------------------------------------------------|
| Language        | Java 21                                                 |
| Framework       | Spring Boot 3.3 (Spring MVC, Spring Data JPA, Spring Security) |
| Auth            | JWT (JSON Web Tokens) + BCrypt password hashing         |
| ORM             | Hibernate / JPA                                         |
| Database        | MySQL 8                                                 |
| Build Tool      | Maven                                                    |
| Frontend        | HTML5, CSS3, Bootstrap 5, vanilla JavaScript (fetch API) |

---

## 2. Project Structure

```
online-banking-system/
├── pom.xml
├── schema.sql                     # Reference DDL (Hibernate also auto-manages this)
├── src/main/java/com/bankapp/banking/
│   ├── BankingApplication.java
│   ├── config/
│   │   └── SecurityConfig.java     # JWT, stateless sessions, CORS, route rules
│   ├── controller/                 # REST controllers (Controller layer)
│   │   ├── AuthController.java
│   │   ├── AccountController.java
│   │   ├── TransferController.java
│   │   ├── TransactionController.java
│   │   └── AdminFraudController.java   # ROLE_ADMIN only - fraud review queue
│   ├── service/                    # Business logic (Service layer)
│   │   ├── AuthService.java
│   │   ├── AccountService.java
│   │   ├── TransferService.java    # Atomic fund-transfer logic + fraud check
│   │   ├── TransactionService.java
│   │   ├── FraudAdminService.java  # Approve/reject flagged transfers
│   │   └── SecurityUtils.java      # Resolves "current logged-in user"
│   ├── fraud/
│   │   ├── FraudRiskEngine.java    # Rule-based risk scoring
│   │   └── FraudAssessment.java    # Immutable scoring result
│   ├── repository/                 # Spring Data JPA repositories
│   │   ├── UserRepository.java
│   │   ├── AccountRepository.java
│   │   ├── TransactionRepository.java
│   │   └── FraudAnalysisRepository.java
│   ├── entity/                     # JPA entities
│   │   ├── User.java
│   │   ├── Account.java
│   │   ├── Transaction.java
│   │   ├── FraudAnalysis.java
│   │   └── enums/ (Role, AccountType, AccountStatus, TransactionType, TransactionStatus, RiskLevel, FraudReviewStatus)
│   ├── dto/                        # Request/response DTOs
│   ├── security/                   # JWT filter, JWT util, UserDetailsService
│   └── exception/                  # Custom exceptions + @RestControllerAdvice
├── src/main/resources/
│   └── application.properties
└── frontend/
    ├── index.html                  # Public landing page (redirects to dashboard if already logged in)
    ├── login.html                  # Login
    ├── register.html               # Registration
    ├── dashboard.html              # Account overview
    ├── account-details.html        # Single account details / balance / settings
    ├── transfer.html               # Money transfer form
    ├── transactions.html           # Transaction history
    ├── admin-fraud.html            # ROLE_ADMIN only - fraud review queue
    ├── css/style.css
    └── js/ (api.js, auth.js, dashboard.js, account-details.js, transfer.js, transactions.js, admin-fraud.js)
```

**Architecture:** `Controller → Service → Repository → Database`, with DTOs used at the controller boundary so JPA entities are never exposed directly over the API.

---

## 3. Database Schema

Database name: **`bankingdb`** (created automatically on first run via `createDatabaseIfNotExist=true`, or manually with `schema.sql` / MySQL Workbench).

Three related tables (see `schema.sql` for full DDL):

- **users** — `id (PK)`, `full_name`, `username (unique)`, `email (unique)`, `password (BCrypt hash)`, `phone_number`, `role`, `enabled`, `created_at`
- **accounts** — `id (PK)`, `account_number (unique)`, `user_id (FK → users.id)`, `account_type`, `balance`, `status`, `created_at`, `version` (optimistic-locking column)
- **transactions** — `id (PK)`, `transaction_ref (unique)`, `from_account_id (FK → accounts.id, nullable)`, `to_account_id (FK → accounts.id, nullable)`, `amount`, `type`, `status`, `description`, `timestamp`

Relationships: `User 1—N Account`, `Account 1—N Transaction` (as either sender or receiver). A `CHECK` constraint keeps balances non-negative and transaction amounts positive.

Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so the schema is created/updated automatically on startup — running `schema.sql` by hand is optional and provided purely for reference/interview purposes.

---

## 4. REST API Reference

All endpoints except `/api/auth/**` require an `Authorization: Bearer <token>` header.

| Method | Endpoint                        | Description                                  |
|--------|----------------------------------|-----------------------------------------------|
| POST   | `/api/auth/register`             | Register a new user, returns a JWT            |
| POST   | `/api/auth/login`                | Log in, returns a JWT                         |
| POST   | `/api/auth/logout`               | No-op endpoint (JWT is stateless; client discards token) |
| POST   | `/api/accounts`                  | Open a new account for the logged-in user     |
| GET    | `/api/accounts`                  | List the logged-in user's accounts            |
| GET    | `/api/accounts/{id}`             | Get one account's details                     |
| PUT    | `/api/accounts/{id}`             | Update account status (e.g. freeze)           |
| DELETE | `/api/accounts/{id}`             | Close an account (must have $0 balance)       |
| GET    | `/api/accounts/{id}/balance`     | Get current balance                           |
| POST   | `/api/transfers`                 | Transfer funds between two accounts (risk-scored — see §9)|
| GET    | `/api/transactions/{accountId}`  | Transaction history for an account            |
| GET    | `/api/admin/fraud/pending`       | **ADMIN** — list transfers awaiting fraud review |
| POST   | `/api/admin/fraud/{transactionId}/approve` | **ADMIN** — approve a flagged transfer, releases funds |
| POST   | `/api/admin/fraud/{transactionId}/reject`  | **ADMIN** — reject a flagged transfer, funds never move |

### Example: Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Jane Doe",
  "username": "janedoe",
  "email": "jane@example.com",
  "password": "secret123",
  "phoneNumber": "555-0100"
}
```

### Example: Transfer
```http
POST /api/transfers
Authorization: Bearer <token>
Content-Type: application/json

{
  "fromAccountNumber": "1234567890",
  "toAccountNumber": "9876543210",
  "amount": 250.00,
  "description": "Rent payment"
}
```

Every response uses meaningful HTTP status codes (`200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `500`) and validation errors return a structured JSON body with per-field messages.

---

## 5. Security Design

- Passwords are hashed with **BCrypt** — plaintext passwords are never stored.
- Authentication is **stateless JWT**: login returns a signed token, which the client sends on every subsequent request in the `Authorization` header. No server-side session state.
- `SecurityConfig` enforces that `/api/auth/**` is public and everything else requires a valid token.
- **Authorization** is enforced at the service layer: `AccountService` / `TransferService` / `TransactionService` all verify that the authenticated user owns the account being accessed (or is an admin) before allowing the operation — a user can never view or move money out of someone else's account.

---

## 6. Atomic Fund Transfers (Interview Talking Point)

`TransferService.transfer()` is annotated `@Transactional`, so debit + credit + transaction-log insert either **all succeed or all roll back together** — there's no window where money can vanish or be duplicated.

To make transfers safe under concurrency, it also:
1. Acquires **pessimistic write locks** (`SELECT ... FOR UPDATE`) on both the sender and receiver account rows before touching balances, so two simultaneous transfers can't read a stale balance and cause a lost update / double-spend.
2. Always locks accounts in a **consistent order** (by account number) regardless of transfer direction, to avoid classic deadlocks when two transfers move money in opposite directions between the same two accounts.
3. Validates account status (`ACTIVE`), sender ≠ receiver, and sufficient balance *before* mutating any state, and still logs a `FAILED` transaction record for insufficient-balance attempts so the audit trail is complete either way.
4. Uses a `@Version` column on `Account` as a secondary optimistic-locking safety net.

---

## 7. Lazy Loading & Ownership Checks (Interview Talking Point)

`Account.user` is deliberately kept `FetchType.LAZY` — it is **not** switched to `EAGER`. Instead of ever calling `account.getUser().getId()` (which can throw `LazyInitializationException` once the loading Hibernate session has closed, e.g. `open-in-view=false` plus a read-only, non-`@Transactional` service method), ownership and owner-name lookups are pushed down into the repository layer:

- **Ownership checks** use `AccountRepository.findByIdAndUserId(id, userId)` / `existsByIdAndUserId(id, userId)` — these never touch the `user` association at all, so they can't trigger a lazy-load regardless of the caller's transaction boundaries. A user can never access another user's account or transactions by guessing an id: the query itself only returns a row when the id *and* the owner match, and `AccountService` / `TransactionService` translate "no match" into a 404 (doesn't exist) vs. 403 (exists, not yours) using a cheap `existsById` check — again without loading the user.
- **Responses that genuinely need the owner's name** (`AccountResponse.ownerName`) use `JOIN FETCH` queries — `findByUserIdWithUser`, `findByIdAndUserIdWithUser`, `findByIdWithUser` — so the `User` is fetched in the *same* query as the `Account`, in one round trip, with no risk of a stale proxy.
- The same problem existed on `Transaction.fromAccount` / `Transaction.toAccount` (also `FetchType.LAZY`): `TransactionService.getTransactionsForAccount()` isn't `@Transactional`, so mapping `Transaction → TransactionResponse` after the repository call returned would have hit the same lazy-init failure. `TransactionRepository.findByAccountIdWithAccounts()` fixes it with `LEFT JOIN FETCH t.fromAccount LEFT JOIN FETCH t.toAccount`, which also avoids an N+1 query per row.

Net effect: `AccountResponse.fromEntity(...)` never touches `account.getUser()` internally — callers that have the user available (because they loaded it via `JOIN FETCH`, or because it's the just-loaded `currentUser` for account creation) explicitly pass the owner's name via `AccountResponse.fromEntityWithOwner(account, ownerName)`.

---

## 8. Fraud Detection & Risk Scoring (Interview Talking Point)

Every transfer is risk-scored by a rule-based engine (`fraud.FraudRiskEngine`) **before any balance is touched** — the scoring itself never blocks anything; `TransferService` decides what to do with the resulting score.

**Rules evaluated** (each is independent and additive, so new ones can be added without touching the others):
1. **Unusually large amount** — tiered: >20,000 adds 40 points, >5,000 adds 20.
2. **Transaction velocity** — how many transfers this account has *sent* in the last 10 minutes (≥3 adds 15, ≥6 adds 35).
3. **Behavioral deviation** — compares the current amount against this account's own recent average transfer size (needs ≥5 prior transfers of history); more than 3x the average adds 20.
4. **Repeated failures** — 3+ failed transfer attempts from this account in the last 24h adds 20.

**Risk bands:** 0–30 `LOW`, 31–60 `MEDIUM`, 61–80 `HIGH`, 81–100 `CRITICAL`.

**What happens with the score:**
- `LOW` / `MEDIUM` — the transfer proceeds immediately, exactly as before. The score is still stored (`FraudAnalysis`, one row per transaction) so the full population of scores is available for later analytics, not just the outliers.
- `HIGH` / `CRITICAL` — **the transfer is blocked automatically.** No money moves. The transaction is recorded with status `PENDING` and a `FraudAnalysis` row with `reviewStatus = PENDING_REVIEW`, and it sits in the admin queue (`GET /api/admin/fraud/pending`) until a `ROLE_ADMIN` user approves or rejects it (`admin-fraud.html` in the frontend).
- **Approve** re-validates everything that could have changed since the transfer was flagged (account still `ACTIVE`, sender still has sufficient balance) using the exact same pessimistic-lock / consistent-lock-order pattern as a normal transfer, then actually moves the money and marks the transaction `SUCCESS`.
- **Reject** simply finalizes the transaction as `FAILED` — since funds were never moved when the transfer was first flagged, there's nothing to reverse.

**Security:** `/api/admin/fraud/**` is protected two ways — a path-level rule in `SecurityConfig` (`hasRole("ADMIN")`) and `@PreAuthorize("hasRole('ADMIN')")` on `AdminFraudController` itself, so a missing annotation on one wouldn't leave the endpoint open.

> **Getting an admin account for testing:** registration always creates `ROLE_USER` accounts (there's no self-service admin signup, deliberately). To test the fraud-review flow, register normally, then promote that user directly in MySQL:
> ```sql
> UPDATE users SET role = 'ROLE_ADMIN' WHERE username = 'your_username';
> ```
> Log out and back in afterward so the JWT reflects the new role.

---

## 9. Development Status & What's Next

This project is being built incrementally rather than as one large rewrite. Currently implemented: the full original banking system (auth, accounts, transfers, transaction history) plus **Fraud Detection & Risk Scoring** (§9) as the first advanced module.

**Not yet implemented** (intentionally deferred, in roughly this order): Financial Analytics dashboard, Scheduled/Recurring Payments, Notifications, Financial Health Score, a broader Admin Monitoring Dashboard (user/transaction search beyond fraud review), a React frontend, an AI financial assistant with MCP-style tool access, Docker/Compose, CI/CD, automated tests, and OpenAPI/Swagger docs. Each of those is a substantial standalone effort — building them all at once tends to produce code nobody (including the person who "wrote" it) can actually defend in an interview. Ask for the next module by name and it'll be built the same way this one was: fully wired into the real transfer/account flow, not a stub.

---

## 10. Running the Application Locally

### Prerequisites
- Java 21+
- Maven 3.6+
- MySQL 8 running locally (or update the connection string for a remote instance)

### Step 1 — Create the database
MySQL will be auto-created on first run because `application.properties` has `createDatabaseIfNotExist=true`, but you can also create it manually in MySQL Workbench or the CLI:
```sql
CREATE DATABASE bankingdb;
```
or run the reference DDL:
```bash
mysql -u root -p < schema.sql
```

### Step 2 — Configure credentials
Edit `src/main/resources/application.properties` if your MySQL username/password differ from the defaults (`root` / `root`):
```properties
spring.datasource.username=root
spring.datasource.password=root
```

### Step 3 — Run the backend
```bash
mvn spring-boot:run
```
The API will start on **http://localhost:8080**.

### Step 4 — Run the frontend
The frontend is static HTML/CSS/JS, so any static file server works. From the `frontend/` folder:
```bash
# Option A: Python's built-in server
python3 -m http.server 5500

# Option B: VS Code "Live Server" extension
```
Then open **http://localhost:5500/index.html** in your browser (this is the public landing page; it links to `login.html` and `register.html`, and auto-redirects to `dashboard.html` if you're already logged in).

> The frontend calls the API at `http://localhost:8080/api` (see `API_BASE` in `frontend/js/api.js`) — update that constant if you deploy the backend elsewhere. CORS is already enabled on the backend for all origins.

### Step 5 — Try it out
1. Register a new user on the Register page.
2. Open one or two accounts from the Dashboard.
3. Use another browser/incognito window to register a second user and open an account, so you have a real "recipient" account number.
4. Transfer money between the two accounts and watch the balances and transaction history update.

---

## 11. Notable Design Decisions

- **DTOs everywhere at the API boundary** — entities are never serialized directly, preventing accidental exposure of internal fields (e.g. password hash) and decoupling the API contract from the database schema.
- **Global exception handling** via `@RestControllerAdvice` maps domain exceptions (`ResourceNotFoundException`, `InsufficientBalanceException`, etc.) to proper HTTP status codes with a consistent JSON error shape.
- **Bean validation** (`jakarta.validation`) on all request DTOs, with field-level error messages surfaced to the client.
- **Soft-delete for accounts** (`DELETE /api/accounts/{id}` sets status to `CLOSED`) rather than a hard row delete, preserving referential integrity for the transaction history.
- **UUID transaction references** so each transaction has a stable, non-guessable external identifier separate from its numeric primary key.
