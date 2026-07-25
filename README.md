# Backend — University Maintenance Request System

Spring Boot 3 (Java 17) REST API with JWT authentication, role-based access control, and MongoDB.

## Tech Stack
- Java 17, Spring Boot 3.3.2
- Spring Security + JWT (jjwt)
- Spring Data MongoDB
- Spring Mail (Gmail SMTP) — for officer welcome emails
- springdoc-openapi — Swagger UI
- Lombok

---

## 1. Prerequisites
- Java 17 (works with newer JDKs too, but 17 is the tested/supported version)
- Maven
- A MongoDB connection string (MongoDB Atlas free tier, or local MongoDB)
- (Optional) A Gmail account with an App Password, for sending officer welcome emails

---

## 2. Environment Variables

Set these before running (see "Running locally" below for exact commands):

| Variable | Required | Description | Default |
|---|---|---|---|
| `MONGODB_URI` | Yes | MongoDB connection string | `mongodb://localhost:27017/maintenance_system` |
| `JWT_SECRET` | Yes | Long random string (32+ chars) used to sign JWTs | insecure placeholder — **change this** |
| `JWT_EXPIRATION_MS` | No | Token lifetime in milliseconds | `86400000` (24h) |
| `CORS_ORIGINS` | Yes (for frontend to work) | Comma-separated list of allowed frontend origins | `http://localhost:5173` |
| `MAIL_USERNAME` | No | Gmail address used to send officer welcome emails (local dev / SMTP fallback) | none |
| `MAIL_APP_PASSWORD` | No | Gmail App Password (not your normal password) | none |
| `BREVO_API_KEY` | No (recommended for Render) | Brevo API key — used instead of SMTP when set; required on Render's free tier since SMTP ports are blocked there | none |
| `CLOUDINARY_CLOUD_NAME` | No (recommended for Render) | Cloudinary cloud name — when set (with the two below), uploaded evidence photos persist across redeploys | none |
| `CLOUDINARY_API_KEY` | No (recommended for Render) | Cloudinary API key | none |
| `CLOUDINARY_API_SECRET` | No (recommended for Render) | Cloudinary API secret | none |
| `FRONTEND_URL` | No | Used in the welcome email's login link | `http://localhost:5173` |
| `PORT` | No | Server port | `8080` |

If `MAIL_USERNAME`/`MAIL_APP_PASSWORD` are left blank, officer creation still works — the temporary password is returned directly in the API response instead of emailed, so you're never blocked by email setup.

---

## 3. Running locally

```bash
cd backend

export MONGODB_URI="mongodb+srv://<user>:<pass>@cluster0.xxxxx.mongodb.net/maintenance_system?retryWrites=true&w=majority"
export JWT_SECRET="pick-a-long-random-string-at-least-32-characters"
export CORS_ORIGINS="http://localhost:5173"
export MAIL_USERNAME="youraddress@gmail.com"
export MAIL_APP_PASSWORD="your-16-char-gmail-app-password"
export FRONTEND_URL="http://localhost:5173"

mvn clean spring-boot:run
```

PowerShell (Windows):
```powershell
$env:MONGODB_URI="mongodb+srv://<user>:<pass>@cluster0.xxxxx.mongodb.net/maintenance_system?retryWrites=true&w=majority"
$env:JWT_SECRET="pick-a-long-random-string-at-least-32-characters"
$env:CORS_ORIGINS="http://localhost:5173"
$env:MAIL_USERNAME="youraddress@gmail.com"
$env:MAIL_APP_PASSWORD="your-16-char-gmail-app-password"
$env:FRONTEND_URL="http://localhost:5173"
mvn clean spring-boot:run
```

The API starts on **http://localhost:8080**.

- Swagger UI: http://localhost:8080/swagger-ui.html
- API docs (OpenAPI JSON): http://localhost:8080/api-docs

### Getting a Gmail App Password (local development)
1. Turn on 2-Step Verification: https://myaccount.google.com/security
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Use the 16-character result (no spaces) as `MAIL_APP_PASSWORD`.

### ⚠️ Email on Render (production) — Gmail SMTP will NOT work there
Render's free tier blocks all outbound traffic on SMTP ports 25, 465, and 587. Gmail SMTP uses port 587, so officer-creation emails that work fine locally will silently fail once deployed — you'll see `Failed to send officer welcome email via SMTP` in the logs, and the admin dashboard will show the "email could not be sent" fallback (with the temp password shown on-screen instead).

**Fix: use Brevo's HTTP API instead**, which sends over HTTPS (port 443, never blocked):
1. Sign up free at https://www.brevo.com (300 emails/day free, no credit card).
2. Go to **Settings → SMTP & API → API Keys** → generate a new key.
3. Verify a sender email/domain under **Senders & Domains** (Brevo requires this before it'll send on your behalf).
4. Set `BREVO_API_KEY` as an environment variable **on Render only** (leave it unset locally if you'd rather keep using Gmail SMTP for local dev — the app automatically prefers Brevo when the key is present, and falls back to Gmail SMTP when it isn't).

---

## 4. First run — seeded data

On startup, if the database is empty, `DataSeeder` creates:
- A default admin account: **`admin@miva.university`** / **`Admin@123`**
- 6 starter request categories: Electrical, Plumbing, Furniture, Internet, Classroom Equipment, Hostel Maintenance

⚠️ Change the seeded admin password after your first login (via `PUT /api/auth/change-password`), or rotate it before deploying publicly.

---

### ⚠️ Uploaded photos on Render (production) — local disk does NOT persist
Render's free tier filesystem is ephemeral: it's wiped on every redeploy, and even reset when the service wakes up after being idle. If evidence photos are stored on local disk (the default fallback), they'll silently disappear — the request record survives in MongoDB, but the image link breaks.

**Fix: use Cloudinary**, a free image-hosting API:
1. Sign up free at https://cloudinary.com (25GB storage/bandwidth free, no card required).
2. On your Cloudinary dashboard, copy your **Cloud Name**, **API Key**, and **API Secret**.
3. Set `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` as environment variables **on Render**.
4. That's it — the app automatically uploads to Cloudinary when these are set, and falls back to local disk when they aren't (so local dev needs zero setup).

---

## 5. Roles

| Role | How it's created |
|---|---|
| `STUDENT` | Public self-registration (`POST /api/auth/register`), default role if none specified |
| `STAFF` | Public self-registration with `role: "STAFF"` and a required `department` — staff can additionally view (read-only) every request submitted by anyone in their department via `GET /api/requests/department` |
| `OFFICER` | Created only by an admin (`POST /api/admin/officers`) — gets a temp password + category specialization, `mustChangePassword=true` until they set their own |
| `ADMIN` | Seeded by default; more can be created by changing another user's role via `PUT /api/admin/users/{id}/role` |

Note: self-registration only ever creates `STUDENT` or `STAFF` — if a `role` value of `OFFICER` or `ADMIN` is sent to `/api/auth/register`, it's silently downgraded to `STUDENT` server-side, so privilege escalation isn't possible through that endpoint.

---

## 6. API Reference

### Auth (`/api/auth`) — public except where noted
| Method | Path | Description |
|---|---|---|
| POST | `/register` | Create a Student/Staff account |
| POST | `/login` | Log in, returns JWT |
| PUT | `/change-password` | *(auth required)* Change your own password |

### Service Requests (`/api/requests`) — auth required
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/` | Any | Submit a request (`multipart/form-data`: `request` JSON part + optional `image`) |
| GET | `/?status=&page=&size=` | Any | List requests, scoped by role (own / assigned / all) |
| GET | `/{id}` | Any | Get one request |
| GET | `/available?page=&size=` | Officer | Unassigned requests in the officer's categories |
| GET | `/department?page=&size=` | Staff | Every request submitted by anyone in the staff member's department (read-only) |
| PUT | `/{id}/claim` | Officer | Atomically self-claim an unassigned request |
| PUT | `/{id}/assign` | Admin | Assign a request to an officer |
| PUT | `/{id}/status` | Officer/Admin | Update a request's status (writes to audit log) |
| DELETE | `/{id}` | Admin (any request) / Student\|Staff (own request, only while PENDING) | Delete a request |

### Categories (`/api/categories`) — auth required
| Method | Path | Who | Description |
|---|---|---|---|
| GET | `/` | Any | List all categories |
| POST | `/` | Admin | Create a category |

### Admin (`/api/admin`) — admin only
| Method | Path | Description |
|---|---|---|
| GET | `/users?role=` | List all users, optional role filter (`STUDENT`/`STAFF`/`OFFICER`/`ADMIN`) |
| GET | `/officers` | List all officers |
| POST | `/officers` | Create an officer (name, email, categoryIds) — generates temp password, emails credentials |
| PUT | `/users/{id}/role` | Change a user's role |
| PUT | `/users/{id}/deactivate` | Deactivate a user |
| GET | `/reports/summary` | Request counts by status/category, user counts by role |
| GET | `/reports/export?status=` | Download all requests as CSV |

All authenticated endpoints expect: `Authorization: Bearer <token>`

---

## 7. Testing

Real, runnable tests are included under `backend/src/test/java/...`:

| Test | Type | Covers |
|---|---|---|
| `security/JwtServiceTest.java` | Unit | Token generation, username extraction, validity checks (including rejecting a token issued to a different user) |
| `service/AuthServiceTest.java` | Unit (Mockito) | Registration defaults to `STUDENT`, Staff registration requires a department, self-registration can't escalate to Officer/Admin, duplicate-email rejection, login token issuance |
| `service/ServiceRequestServiceTest.java` | Unit (Mockito) | Request submission + audit log write, the atomic "claim" operation and its race-condition guard, and the delete-authorization rules (owner + still-pending required for Student/Staff, Admin bypasses both checks) |
| `controller/AuthControllerTest.java` | Web layer (MockMvc) | `/api/auth/register` and `/api/auth/login` return correct HTTP status/JSON, and invalid payloads (missing email, short password) return 400 |

Run them with:
```bash
mvn test
```

These don't require a live MongoDB connection — repositories are mocked — so they run the same locally, in CI, or offline. Screenshots of a green `mvn test` run are good evidence for your report's "Testing evidence" section.

For endpoints these unit/web tests don't reach directly (role-based assignment, claiming, admin actions), exercise them manually through Swagger UI (`http://localhost:8080/swagger-ui.html`) or Postman — screenshots from there double as your API documentation evidence too.

---

## 8. Deployment (Render)
Render has no native Java runtime — only Node.js, Python, Ruby, Go, Rust, and Elixir are native. Java apps deploy as a **Docker image** instead, which is already set up here (`backend/Dockerfile`).

1. Push to GitHub, connect the repo on https://render.com → **New → Web Service**, root directory `backend`.
2. Render should auto-detect the Dockerfile and pick **Docker** as the environment. If prompted to choose a runtime manually, choose **Docker** — there's no Java option because Render doesn't run one natively.
3. Leave Build Command / Start Command blank — the Dockerfile defines both.
4. Set all the environment variables from section 2 in Render's dashboard (`CORS_ORIGINS` should be your deployed frontend's URL; don't set `PORT` — Render provides it automatically).
5. First deploy takes a few minutes (it's compiling the Java app inside the Docker build) — that's expected.

## 9. Known limitations
- Uploaded evidence photos use local disk storage only as a fallback when Cloudinary isn't configured (see section 4a) — set `CLOUDINARY_CLOUD_NAME`/`CLOUDINARY_API_KEY`/`CLOUDINARY_API_SECRET` on any deployed environment to avoid Render's ephemeral-disk problem.
- No refresh-token flow — JWTs simply expire after `JWT_EXPIRATION_MS` and require re-login.