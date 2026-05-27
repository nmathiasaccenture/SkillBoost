# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this app is

SkillBoost is a coding-practice app: users are shown intentionally-buggy code and have to fix it so a hidden test harness passes. Supported languages are **Java, JavaScript, and Python** — selectable via tabs in the header. A Spring Boot backend ([src/main/java/com/skillboost/](src/main/java/com/skillboost/)) serves exercises and runs user submissions against the corresponding runtime on the host (`javac`/`java`, `node`, `python`). A Vite/React/Monaco frontend ([frontend/](frontend/)) hosts the editor and shows results.

## Common commands

Backend (run from repo root — there is **no Maven wrapper**, `.mvn/` is empty, so use a system `mvn`):

- Run server: `mvn spring-boot:run` (listens on `:8080`)
- All tests: `mvn test`
- Single test class: `mvn test -Dtest=JavaJudgeTest`
- Single test method: `mvn test -Dtest=JavaJudgeTest#runReportsAllTestsPassingForCorrectSolution`
- Package: `mvn package`

Frontend (run from [frontend/](frontend/)):

- Dev server: `npm run dev` (listens on `:5173`, opens browser, proxies `/api` → `localhost:8080` — backend must be running)
- Type-check + build: `npm run build`
- Preview build: `npm run preview`

For end-to-end work both servers must be running: backend on 8080, frontend on 5173. The frontend `fetch('/api/...')` calls rely on the Vite proxy ([frontend/vite.config.ts](frontend/vite.config.ts)); the backend's CORS config ([src/main/java/com/skillboost/config/WebConfig.java](src/main/java/com/skillboost/config/WebConfig.java)) only whitelists `http://localhost:5173`.

## Architecture

### Submission flow (the load-bearing path)

1. Frontend posts `{ exerciseId, code }` to `POST /api/submissions` ([frontend/src/api.ts](frontend/src/api.ts)).
2. [SubmissionController](src/main/java/com/skillboost/controller/SubmissionController.java) looks up the exercise and dispatches by `exercise.language()` to one of three judges: `java` → [JavaJudge](src/main/java/com/skillboost/service/JavaJudge.java), `javascript` → [JavaScriptJudge](src/main/java/com/skillboost/service/JavaScriptJudge.java), `python` → [PythonJudge](src/main/java/com/skillboost/service/PythonJudge.java). Unknown languages return a 400 with an "unsupported" message.
3. All three extend [AbstractProcessJudge](src/main/java/com/skillboost/service/AbstractProcessJudge.java), which holds the shared template-method flow: create temp dir → write `solutionFilename()` and `runnerFilename()` → optionally `compile()` → run `executeCommand()` → parse stdout → clean up. Subclasses only declare filenames, the run command, and (for Java) the compile step.
4. **The test protocol is tab-separated lines on stdout**, parsed in `AbstractProcessJudge.execute`:
   - `PASS\t<label>`
   - `FAIL\t<label>\t<expected>\t<actual>`
   - `ERROR\t<label>\t<message>`
   - Any other line is ignored. If no lines are produced but stderr is non-empty, stderr is reported as a single failing `(execution)` result.
5. Both compile and execute steps are bounded by `skillboost.judge.timeout-seconds` (default 5s, see [application.properties](src/main/resources/application.properties)). Timeouts produce a synthetic failing test rather than an exception.

Because the judges shell out to `javac`/`java`, `node`, and `python`, **the runtimes on `PATH` are what run user code** — there is no in-process compilation and no sandbox. Treat this as trusted-input only. The Python judge invokes `python` (not `python3`); on systems where only `python3` exists, change [PythonJudge.executeCommand()](src/main/java/com/skillboost/service/PythonJudge.java).

### Exercise loading

[ExerciseService](src/main/java/com/skillboost/service/ExerciseService.java) loads every `classpath:exercises/*.json` into a `LinkedHashMap<String, Exercise>` once at startup via `@PostConstruct`. There is no reload mechanism — restart the server after editing exercise JSON.

The [Exercise](src/main/java/com/skillboost/model/Exercise.java) record contains both the `buggyCode` shown to the user and the `solutionCode`/`testHarness` used internally. **`Exercise.toPublicView()` nulls out `solutionCode` and `testHarness` before responding to clients** — both controller endpoints map through it. Do not return the raw `Exercise` from a controller or you'll leak solutions.

### Adding a new exercise

Drop a `<id>.json` file into [src/main/resources/exercises/](src/main/resources/exercises/). Required fields: `id`, `language` (one of `"java"`, `"javascript"`, `"python"`), `difficulty`, `title`, `description`, `hint`, `buggyCode`, `solutionCode`, `testHarness`, `tests`. Templates per language:

- Java — see [java-factorial.json](src/main/resources/exercises/java-factorial.json). `buggyCode`/`solutionCode` must define `public class Solution`; `testHarness` must define `public class Runner` with a `main` (judge writes them to `Solution.java`/`Runner.java`).
- JavaScript — see [javascript-find-max.json](src/main/resources/exercises/javascript-find-max.json). `buggyCode`/`solutionCode` must export the function via `module.exports = { ... }` (CommonJS — the harness uses `require('./solution.js')`). `testHarness` is `runner.js`, executed by `node runner.js`.
- Python — see [python-list-sum.json](src/main/resources/exercises/python-list-sum.json). `buggyCode`/`solutionCode` define a module-level function; `testHarness` is `runner.py` and imports it via `from solution import <function>`. Executed by `python runner.py`.

In every language the harness must emit the same tab-separated `PASS`/`FAIL`/`ERROR` lines on stdout.

The per-language parameterized tests ([JavaJudgeTest](src/test/java/com/skillboost/service/JavaJudgeTest.java), [JavaScriptJudgeTest](src/test/java/com/skillboost/service/JavaScriptJudgeTest.java), [PythonJudgeTest](src/test/java/com/skillboost/service/PythonJudgeTest.java)) filter the exercise list by language and assert `solutionCode` passes every harness test AND `buggyCode` fails at least one. The `tests` field on the record is informational only — the harness file is what actually runs.

### Frontend

Single-page React app, no router. [App.tsx](frontend/src/App.tsx) holds all state: exercise list, current language tab, selected exercise, current code buffer, last submission result, hint visibility, fetched solution. The language tabs in the header filter the sidebar; switching languages auto-selects the first exercise in that language. Monaco's syntax is driven by `monacoLanguage(selected.language)`. The `Reset code` button restores `selected.buggyCode`. "Show hint" toggles the inline hint panel. "Show solution" fetches `GET /api/exercises/{id}/solution` lazily (with a confirm prompt) and displays it in a read-only Monaco editor below — the user is expected to type it into the main editor. Failure rendering distinguishes compile errors (`!result.compiled`) from per-test `FAIL`/`ERROR`.
