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
| `MAIL_USERNAME` | No | Gmail address used to send officer welcome emails | none |
| `MAIL_APP_PASSWORD` | No | Gmail App Password (not your normal password) | none |
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

### Getting a Gmail App Password
1. Turn on 2-Step Verification: https://myaccount.google.com/security
2. Generate an App Password: https://myaccount.google.com/apppasswords
3. Use the 16-character result (no spaces) as `MAIL_APP_PASSWORD`.

---

## 4. First run — seeded data

On startup, if the database is empty, `DataSeeder` creates:
- A default admin account: **`admin@miva.university`** / **`Admin@123`**
- 6 starter request categories: Electrical, Plumbing, Furniture, Internet, Classroom Equipment, Hostel Maintenance

⚠️ Change the seeded admin password after your first login (via `PUT /api/auth/change-password`), or rotate it before deploying publicly.

---

## 5. Roles

| Role | How it's created |
|---|---|
| `STUDENT_STAFF` | Public self-registration (`POST /api/auth/register`) |
| `OFFICER` | Created only by an admin (`POST /api/admin/officers`) — gets a temp password + category specialization, `mustChangePassword=true` until they set their own |
| `ADMIN` | Seeded by default; more can be created by changing another user's role via `PUT /api/admin/users/{id}/role` |

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
| PUT | `/{id}/claim` | Officer | Atomically self-claim an unassigned request |
| PUT | `/{id}/assign` | Admin | Assign a request to an officer |
| PUT | `/{id}/status` | Officer/Admin | Update a request's status (writes to audit log) |
| DELETE | `/{id}` | Admin | Delete a request |

### Categories (`/api/categories`) — auth required
| Method | Path | Who | Description |
|---|---|---|---|
| GET | `/` | Any | List all categories |
| POST | `/` | Admin | Create a category |

### Admin (`/api/admin`) — admin only
| Method | Path | Description |
|---|---|---|
| GET | `/users?role=` | List all users, optional role filter (`STUDENT_STAFF`/`OFFICER`/`ADMIN`) |
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
| `service/AuthServiceTest.java` | Unit (Mockito) | Registration always creates `STUDENT_STAFF` with an encoded password, duplicate-email rejection, login token issuance |
| `service/ServiceRequestServiceTest.java` | Unit (Mockito) | Request submission + audit log write, the atomic "claim" operation, and the race-condition guard (second officer's claim correctly fails) |
| `controller/AuthControllerTest.java` | Web layer (MockMvc) | `/api/auth/register` and `/api/auth/login` return correct HTTP status/JSON, and invalid payloads (missing email, short password) return 400 |

Run them with:
```bash
mvn test
```

These don't require a live MongoDB connection — repositories are mocked — so they run the same locally, in CI, or offline. Screenshots of a green `mvn test` run are good evidence for your report's "Testing evidence" section.

For endpoints these unit/web tests don't reach directly (role-based assignment, claiming, admin actions), exercise them manually through Swagger UI (`http://localhost:8080/swagger-ui.html`) or Postman — screenshots from there double as your API documentation evidence too.

---

## 8. Deployment (Render)
1. Push to GitHub, connect the repo on https://render.com, root directory `backend`.
2. Build command: `mvn clean package -DskipTests`
3. Start command: `java -jar target/maintenance-system-1.0.0.jar`
4. Set all the environment variables from section 2 in Render's dashboard (`CORS_ORIGINS` should be your deployed frontend's URL).

## 9. Known limitations
- Uploaded evidence photos are stored on local disk (`backend/uploads/`), served at `/uploads/**`. On Render's free tier this storage is ephemeral and wiped on redeploy — a production version would use S3/Cloudinary instead.
- No refresh-token flow — JWTs simply expire after `JWT_EXPIRATION_MS` and require re-login.
