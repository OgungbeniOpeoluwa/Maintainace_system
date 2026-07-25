# Backend — University Maintenance Request System

Spring Boot 3 (Java 17) REST API with JWT authentication, role-based access control, and MongoDB.

This is the **backend only**. The frontend (React SPA) lives in a separate repository: **https://github.com/OgungbeniOpeoluwa/Maintainace-system-frontend**.

## Live

- **API**: https://maintainace-system.onrender.com
- **Swagger UI**: https://maintainace-system.onrender.com/swagger-ui.html
- **Frontend**: https://maintainace-system-frontend.vercel.app

The free tier sleeps after inactivity — the first request after a period of idle time may take 30–60 seconds to respond.

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

### Email Configuration

Officer account creation sends a welcome email with login credentials. Two options, chosen automatically based on which environment variables are set:

| Option | Environment variable(s) | Best for |
|---|---|---|
| **Brevo API** | `BREVO_API_KEY` | Render / any deployed environment |
| **Gmail SMTP** | `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | Local development |

The app uses Brevo whenever `BREVO_API_KEY` is set, and falls back to Gmail SMTP otherwise.

⚠️ **Render's free tier blocks outbound SMTP ports (25, 465, 587)**, so Gmail SMTP does not work once deployed there, even if it works locally. Set `BREVO_API_KEY` for any Render deployment.

**Gmail SMTP setup** (local development):
1. Turn on 2-Step Verification: https://myaccount.google.com/security
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Use the 16-character result (no spaces) as `MAIL_APP_PASSWORD`.

**Brevo setup** (required for Render, optional locally):
1. Sign up free at https://www.brevo.com (300 emails/day free, no card required).
2. Go to **Settings → SMTP & API → API Keys** → generate a new key.
3. Verify a sender email/domain under **Senders & Domains** (required before Brevo will send on your behalf).
4. Set `BREVO_API_KEY`.

---

## 4. First run — seeded data

On startup, if the database is empty, `DataSeeder` creates:
- A default admin account: **`admin@miva.university`** / **`Admin@123`**
- 6 starter request categories: Electrical, Plumbing, Furniture, Internet, Classroom Equipment, Hostel Maintenance

⚠️ Change the seeded admin password after your first login (via `PUT /api/auth/change-password`), or change it before deploying publicly.

---

### Image Storage Configuration

Uploaded evidence photos are stored using one of two options, chosen automatically:

| Option | Environment variable(s) | Best for |
|---|---|---|
| **Cloudinary** | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Render / any deployed environment |
| **Local disk** | *(none — this is the default)* | Local development |

⚠️ **Render's free tier filesystem is ephemeral** — it's wiped on every redeploy and reset on cold-start wake-ups. Photos stored on local disk will be lost; the request record survives in MongoDB, but the image link breaks. Set the three `CLOUDINARY_*` variables for any Render deployment.

**Cloudinary setup** (required for Render, optional locally):
1. Sign up free at https://cloudinary.com (25GB storage/bandwidth free, no card required).
2. Copy your **Cloud Name**, **API Key**, and **API Secret** from the dashboard.
3. Set the three `CLOUDINARY_*` environment variables.

---

## 5. Roles

| Role | How it's created |
|---|---|
| `STUDENT` | Public self-registration (`POST /api/auth/register`), default role if none specified |
| `STAFF` | Public self-registration with `role: "STAFF"` and a required `department` — staff can additionally view (read-only) every request submitted by anyone in their department via `GET /api/requests/department` |
| `OFFICER` | Created only by an admin (`POST /api/admin/officers`) — gets a temp password + category specialization, `mustChangePassword=true` until they set their own |
| `ADMIN` | Seeded by default; more can be created by changing another user's role via `PUT /api/admin/users/{id}/role` |

Note: self-registration only ever creates `STUDENT` or `STAFF`. if a `role` value of `OFFICER` or `ADMIN` is sent to `/api/auth/register`, it's silently downgraded to `STUDENT` server-side, so privilege escalation isn't possible through that endpoint.

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
| GET | `/{id}/logs` | Any | The full audit/status history for a request (who changed what, and when) |
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

These don't require a live MongoDB connection,repositories are mocked, so they run the same locally, in CI, or offline. Screenshots of a green `mvn test` run are good evidence for your report's "Testing evidence" section.

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
- No refresh-token flow — JWTs simply expire after `JWT_EXPIRATION_MS` and require re-login.