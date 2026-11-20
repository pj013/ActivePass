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

Test accounts (DEV seeder): student@test.com / Student123!, organizer@test.com / Organizer123!, admin@test.com / Admin123!

## Endpoints
| Method | Path | Access |
|---|---|---|
| GET  | /api/auth/csrf | public (sets XSRF-TOKEN cookie) |
| POST | /api/auth/login | public, body `{email, password}` |
| GET  | /api/auth/me | any logged-in user |
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
