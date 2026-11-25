# Auth + RBAC backend (Spring Boot 3, session-based)

Run: `mvn spring-boot:run`  (Java 17+)

## Database configuration

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment before starting the backend. For example, in PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://<your-host>:5432/postgres?sslmode=require"
$env:DB_USERNAME = "<your-database-username>"
$env:DB_PASSWORD = "<your-database-password>"
mvn spring-boot:run
```

Replace the placeholders with your database connection values. Do not commit credentials or put them in frontend `VITE_` variables.

Password recovery requires an SMTP server. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and optionally `MAIL_FROM` and `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS`. Set `FRONTEND_URL` to the public frontend origin so reset links point to the correct app. Reset tokens expire after 15 minutes, are stored only as SHA-256 hashes, and can be used once.

For Gmail delivery, the SMTP sender account needs 2-Step Verification and a Google App Password; a normal Gmail sign-in password will not work. Set these variables in the same PowerShell window used to start Maven, then restart the backend:

```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "<sender Gmail address>"
$env:MAIL_PASSWORD = "<Google App Password>"
$env:MAIL_FROM = "<sender Gmail address>"
$env:MAIL_SMTP_AUTH = "true"
$env:MAIL_SMTP_STARTTLS = "true"
mvn spring-boot:run
```

`MAIL_USERNAME` and `MAIL_PASSWORD` authenticate the sender. The reset email is delivered to the Gmail address stored on the matching ActivePass user account. The forgot-password response intentionally does not reveal whether an account exists or whether mail delivery succeeded; inspect the backend console for SMTP errors.

Test accounts (DEV seeder): student@test.com / Student123!, organizer@test.com / Organizer123!, admin@test.com / Admin123!

## Endpoints
| Method | Path | Access |
|---|---|---|
| GET  | /api/auth/csrf | public (sets XSRF-TOKEN cookie) |
| POST | /api/auth/login | public, body `{email, password}` |
| GET  | /api/auth/me | any logged-in user |
| PUT  | /api/auth/profile | any logged-in user; updates only the current user |
| POST | /api/auth/change-password | any logged-in user; requires current password |
| POST | /api/auth/forgot-password | public; body `{email}` |
| POST | /api/auth/reset-password | public; body `{token, newPassword, confirmPassword}` |
| POST | /api/auth/logout | any logged-in user |
| GET  | /api/student/dashboard | STUDENT |
| GET  | /api/organizer/dashboard | ORGANIZER |
| GET  | /api/admin/dashboard | ADMINISTRATOR |

## Quick curl test
```
curl -i -c jar -b jar http://localhost:8080/api/auth/csrf
TOKEN=$(grep XSRF-TOKEN jar | awk '{print $7}')
curl -i -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"Student123!"}' http://localhost:8080/api/auth/login
curl -i -b jar http://localhost:8080/api/student/dashboard   # 200
curl -i -b jar http://localhost:8080/api/admin/dashboard     # 403
```
