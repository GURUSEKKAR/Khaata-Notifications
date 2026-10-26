# Khaata — Digital Udhaar Book

A digital credit-book ("khaata" / "udhaar") for local Indian shopkeepers. Record
credit given and repayments per customer, see who owes what at a glance, and let
the app **automatically chase customers** to repay — over WhatsApp in production.

Built as a **strong, defensible backend** project: a layered Spring Boot service
around an **append-only transaction ledger**, with a decoupled, idempotent reminder
engine behind a pluggable notification channel.

---

## Monorepo layout

```
RCR M_3/
├── khaata-backend/     Spring Boot 3 (Java 17+) REST API + reminder engine
├── khaata-frontend/    React + Vite + Tailwind dashboard (70/30 layout)
└── docker-compose.yml  Postgres + app, one-command prod run
```

---

## Running it

### Prerequisites
- **JDK 21** (Lombok in this Spring Boot version needs JDK ≤ 21; building on JDK 25
  fails at annotation processing). Point Maven at JDK 21:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
  ```
- Node 18+ for the frontend.

### Backend (dev — H2 in-memory, console reminders, auto-seeded data)
```bash
cd khaata-backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console  (JDBC `jdbc:h2:mem:khaata`)

The dev profile seeds four customers (Guru, Gopi, Shitij, Karthik) with sample
credit/repayments so the dashboard is populated immediately.

**Login:** the app is gated behind a shop-owner login. A default account is seeded
on first run — **username `owner`, password `khaata123`** (override via env in prod:
`KHAATA_OWNER_USERNAME`, `KHAATA_OWNER_PASSWORD`, `KHAATA_JWT_SECRET`).

### Backend (dev against a real, containerized Postgres)
Run just the database in Docker (persistent volume), app locally:
```bash
# from the repo root
docker compose -f docker-compose.db.yml up -d        # start Postgres

cd khaata-backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
SPRING_PROFILES_ACTIVE=devpg mvn spring-boot:run     # app -> Postgres
```
Data persists across restarts (named volume `khaata_pgdata_dev`).

### Frontend
```bash
cd khaata-frontend
npm install
npm run dev        # http://localhost:5173  (proxies /api to :8080)
```

### Production (Postgres + WhatsApp) via Docker
```bash
docker compose up --build
```
Fill the `WHATSAPP_*` env vars in `docker-compose.yml` to enable real delivery.

---

## API summary

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login` | Log in, returns a JWT (public) |
| POST | `/api/auth/register` | Sign up a new owner account, returns a JWT (public) |
| POST | `/api/customers` | Register a customer |
| GET  | `/api/customers` | List customers with derived balances |
| GET  | `/api/customers/{id}` | One customer with balances |
| PUT  | `/api/customers/{id}` | Update customer details |
| DELETE | `/api/customers/{id}` | Delete customer (body: your password; blocked if balance owed) |
| POST | `/api/customers/{id}/transactions` | Record CREDIT or REPAYMENT (Add / Clear) |
| GET  | `/api/customers/{id}/transactions` | Ledger history (raw) |
| GET  | `/api/customers/{id}/history` | Ledger history with running balance (history panel) |
| GET  | `/api/dashboard/summary` | The four shop-wide boxes |
| POST | `/api/reminders/run` | Trigger a reminder run now |
| GET  | `/api/reminders/log` | Reminder audit trail |

All `/api/**` except `/api/auth/**` require a `Authorization: Bearer <token>` header.

---

## Core design: the append-only ledger

There is **no mutable `balance` column**. Every event is an immutable row in
`ledger_transaction` (CREDIT or REPAYMENT, amount always positive). The balance is
**derived**:

```
outstanding = SUM(CREDIT.amount) − SUM(REPAYMENT.amount)
overdue     = SUM(CREDIT past due_date), capped at outstanding
```

Why: full auditability (a disputed payment is in the history), and no read-modify-write
race on a balance column. Money uses `BigDecimal`, never floating point.

---

## System-design concepts (defense cheat sheet)

| Concept | Where it lives | Trade-off defended |
|---|---|---|
| Layered architecture | `controller` → `service` → `repository` | boilerplate vs reuse/testability |
| Stateless auth (JWT) | `JwtService`, `JwtAuthFilter`, `SecurityConfig` | no server session → horizontal scaling; token revocation tradeoff |
| Password hashing | BCrypt in `OwnerSeeder` / `AuthService` | never store plaintext; salted + slow by design |
| Append-only ledger / event sourcing | `LedgerTransaction`, derived sums | compute-on-read vs auditability |
| ACID transactions | `@Transactional` on repayment + guard | atomic check-then-insert |
| Concurrency control | `@Version` optimistic lock on `Customer` | optimistic (retry) vs pessimistic |
| Async decoupling | `@Scheduled` reminder job off the write path | don't block API on flaky messaging |
| Idempotency | `reminder_log` (customer, day) dedupe | extra check vs spamming customers |
| Strategy / Dependency Inversion | `NotificationSender` interface | abstraction cost vs pluggable channels |
| REST + DTOs + validation | `dto/*`, Bean Validation | mapping cost vs protected contract |
| Read model / CQRS-lite | aggregate queries in `TransactionRepository` | separate read model from event log |
| Config & profiles (12-factor) | `application-{dev,prod}.yml` | same jar, env-driven behavior |
| Fault tolerance / resilience | Resilience4j retry + circuit breaker on WhatsApp | degrade gracefully vs fail loud |
| Migrations | Flyway `V1__init.sql` | versioned schema vs auto-ddl |
| Observability | Spring Actuator, structured logs | insight vs overhead |

**30-second verbal defense:** Khaata is a layered Spring Boot service built on an
append-only ledger — balances are derived, not mutated, giving auditability and
dodging lost-update bugs. Writes are fast and transactional; the unreliable part,
chasing customers, is decoupled into a scheduled, idempotent reminder job behind a
pluggable notification interface — console in dev, WhatsApp in prod, with retries and
a circuit breaker. It starts simple but has a clear scaling path: cached read models,
a message queue, and horizontal scaling behind a stateless API.

---

## The reminder channel, honestly

There is no free open protocol to message an arbitrary phone. Khaata ships multiple
pluggable senders (all behind the same `NotificationSender` interface) — pick one with
`khaata.notification.channel`:

| channel | what it does | setup |
|---|---|---|
| `console` (default) | logs the message, records it in the log — **no real send** | none |
| `telegram` | real Telegram messages via a bot — **free, no templates** | a bot token |
| `twilio` | real SMS or WhatsApp via Twilio | Twilio account + credentials |

Everything is **credential-only**: drop values into `.env` and it auto-sends. No code change.

### Telegram (recommended free option)
1. In Telegram, message **@BotFather** → `/newbot` → copy the **token**.
2. Message your new bot once, then open
   `https://api.telegram.org/bot<token>/getUpdates` and copy your **chat id**.
3. In `.env`: `KHAATA_NOTIFICATION_CHANNEL=telegram`, `TELEGRAM_BOT_TOKEN=...`, and either
   set a customer's Telegram chat id in the app or `TELEGRAM_DEFAULT_CHAT_ID=...` for demos.
4. Run and click **Send Reminders Now** (or a customer's 🔔) — a real Telegram arrives.

### Twilio WhatsApp (note: free trial needs a Content Template)

Twilio sends both SMS and WhatsApp from one account and has a free trial. The app has a
`TwilioSender` behind the same `NotificationSender` interface — enable it with
`khaata.notification.channel=twilio` and set `twilio.mode` to `sms` or `whatsapp`.

**One-time Twilio setup (free trial):**
1. Sign up at twilio.com/try-twilio (no card needed).
2. From the Console dashboard copy your **Account SID** and **Auth Token**.
3. **Verify your own phone** (Console → Phone Numbers → Verified Caller IDs) — the trial
   only sends to verified numbers.
4. For **SMS**: buy the free trial number (Phone Numbers → Buy a number) and note it.
5. For **WhatsApp**: enable the sandbox (Messaging → Try it out → Send a WhatsApp message),
   then from your phone send the `join <code>` message to the sandbox number once.

**Run with Twilio (WhatsApp sandbox example):**
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
KHAATA_NOTIFICATION_CHANNEL=twilio \
TWILIO_MODE=whatsapp \
TWILIO_ACCOUNT_SID=ACxxxxxxxx \
TWILIO_AUTH_TOKEN=xxxxxxxx \
TWILIO_WHATSAPP_FROM='whatsapp:+14155238886' \
mvn spring-boot:run
```
For SMS instead, set `TWILIO_MODE=sms` and `TWILIO_SMS_FROM=+1415xxxxxxx`.
Then add a customer whose phone is your verified number, give them overdue credit, and
click **Send Reminders Now** — a real SMS/WhatsApp arrives.

> Trial limits: messages only to verified numbers, an SMS "trial account" prefix, and the
> WhatsApp sandbox join step. Adding Twilio credit removes these — no code change.

---

## Tests

```bash
cd khaata-backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21) && mvn test
```
`LedgerServiceTest` covers the money math: balance = credit − repayment, overpayment
rejection, and overdue detection (past vs future due dates).
