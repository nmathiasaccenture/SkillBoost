# SkillBoost

A coding-practice app where users are shown intentionally-buggy code and have to fix it so a hidden test harness passes. Each exercise has a difficulty rating, a description, an optional hint, and a reveal-on-demand solution.

## What it supports

- **Languages**: Java, JavaScript, and Python — selectable via tabs in the header. The judge shells out to `javac`/`java`, `node`, and `python` on the host, so those runtimes must be on `PATH`.
- **Accounts**: register / sign in with username + password (BCrypt-hashed). Stateless JWT for API auth.
- **Progress tracking** (signed-in users): solved/unsolved per exercise + attempt counts, persisted in an H2 file DB.
- **Admin UI**: users with the `ADMIN` role can create, edit, and delete exercises (description, hint, buggy code, solution, test harness, test cases) through a web UI — no need to drop JSON files into the resources folder.
- **Sandboxing**: on Linux with `bwrap` installed, user code runs in a bubblewrap sandbox (network unshared, read-only filesystem except per-submission temp dir). On Windows / Linux without bwrap, a warning is logged and code runs unsandboxed — treat as trusted-input only.

## Architecture in a sentence

A Spring Boot backend (`:8080`) serves exercises and runs submissions; a Vite/React/Monaco frontend (`:5173`) hosts the editor. Exercises live in an H2 file DB that is seeded once from `src/main/resources/exercises/*.json` on first boot.

## Quick start

You need: **JDK 21**, **Maven**, **Node 18+ and npm**, plus `node` and `python` (and `javac` for Java exercises) on your `PATH`.

### 1. Start the backend

From the repo root:

```
mvn spring-boot:run
```

This will:
- Create `./data/skillboost.mv.db` (H2 file DB) on first run.
- Seed all built-in exercises from `src/main/resources/exercises/*.json` into the DB the first time the table is empty.
- Seed an initial admin user from `application.properties` if no admin exists yet (defaults to `admin` / `changeme`).
- Listen on `http://localhost:8080`.

> **Before any non-local use**, change `skillboost.security.jwt-secret` and `skillboost.admin.password` in `src/main/resources/application.properties`.

### 2. Start the frontend

From the `frontend/` directory:

```
npm install
npm run dev
```

Vite will open `http://localhost:5173`. Requests to `/api/*` are proxied to the backend, so both must be running.

### 3. Use it

- Browse exercises anonymously — pick a language tab, choose an exercise, edit the code in the Monaco editor, click **Run tests**.
- Click **Sign in** (top right) to register a normal account. Once signed in, solved-checkmarks and attempt counts appear in the sidebar.
- Sign in as the seeded admin (`admin` / `changeme`) → an **Admin** button appears in the header → click it to manage exercises through the UI.

## Common commands

Backend (run from repo root):

| Command | What it does |
| --- | --- |
| `mvn spring-boot:run` | Start the server on `:8080` |
| `mvn test` | Run the full test suite |
| `mvn test -Dtest=JavaJudgeTest` | Run one test class |
| `mvn package` | Build a runnable JAR |

Frontend (run from `frontend/`):

| Command | What it does |
| --- | --- |
| `npm run dev` | Dev server on `:5173`, proxies `/api/*` to `:8080` |
| `npm run build` | Type-check + production build into `frontend/dist/` |
| `npm run preview` | Preview the production build |

## API summary

Public:
- `GET /api/exercises` — list exercises (no solution / harness)
- `GET /api/exercises/{id}` — get one (no solution / harness)
- `GET /api/exercises/{id}/solution` — reveal solution (`{ "solutionCode": "..." }`)
- `POST /api/submissions` — `{ exerciseId, code }` → `{ compiled, compileError, allPassed, results[] }`. If a `Bearer` token is included, progress is recorded.
- `POST /api/auth/register` — `{ username, email?, password }` → `{ token, username, role, expiresInMs }`
- `POST /api/auth/login` — `{ username, password }` → same shape as register

Authenticated:
- `GET /api/me` — current user info
- `GET /api/me/progress` — array of `{ exerciseId, solved, firstSolvedAt, attempts }`

Admin (`ADMIN` role required):
- `GET /api/admin/exercises`
- `GET /api/admin/exercises/{id}`
- `POST /api/admin/exercises` — create
- `PUT /api/admin/exercises/{id}` — update
- `DELETE /api/admin/exercises/{id}`

## Adding exercises without the UI

Built-in exercises live in [src/main/resources/exercises/](src/main/resources/exercises/) as JSON files. They are loaded into the DB the **first time** the table is empty, so to ship a new built-in exercise you can either:

- Add it through the Admin UI after signing in as admin, or
- Drop a new `<id>.json` file in that folder and delete `./data/` so the DB is reseeded on next start.

Required JSON fields: `id`, `language` (`"java"` / `"javascript"` / `"python"`), `difficulty`, `title`, `description`, `hint`, `buggyCode`, `solutionCode`, `testHarness`, `tests`. See [java-factorial.json](src/main/resources/exercises/java-factorial.json) for a complete example.

The test harness must emit tab-separated lines on stdout:

```
PASS\t<label>
FAIL\t<label>\t<expected>\t<actual>
ERROR\t<label>\t<message>
```

## Project layout

```
src/main/java/com/skillboost/
  controller/   REST endpoints (auth, exercises, admin, submissions, me)
  model/        Records + JPA entities (Exercise, AppUser, ProgressEntity, ...)
  repository/   Spring Data JPA repositories
  security/     JWT filter + service, Spring Security config
  service/      ExerciseService, AdminSeeder, judges, sandbox
src/main/resources/
  exercises/    Built-in exercise JSON (seeded into DB on first boot)
  application.properties
frontend/src/
  components/   AuthForms, AdminPanel
  api.ts        Centralized fetch + bearer token plumbing
  auth.tsx      Auth context + hook
  App.tsx       Main practice view
  styles.css
```

## Configuration

All in [src/main/resources/application.properties](src/main/resources/application.properties):

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port |
| `skillboost.judge.timeout-seconds` | `5` | Per-step timeout for compile + execute |
| `skillboost.judge.sandbox.enabled` | `true` | Enable bwrap sandbox (Linux only; ignored elsewhere) |
| `spring.datasource.url` | `jdbc:h2:file:./data/skillboost;...` | H2 file location |
| `skillboost.security.jwt-secret` | placeholder | HS256 signing key — must be ≥ 32 bytes |
| `skillboost.security.jwt-expiration-minutes` | `240` | Token lifetime |
| `skillboost.admin.username` / `password` / `email` | `admin` / `changeme` / `admin@skillboost.local` | Used only when no admin exists yet |

## Resetting

- **DB**: stop the server and delete the `./data/` folder. Next start will recreate the schema and reseed exercises + admin.
- **Frontend deps**: delete `frontend/node_modules/` and run `npm install` again.
