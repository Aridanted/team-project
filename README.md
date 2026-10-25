# LogiCoach

A Java Swing app that imports Logisim-evolution circuits (`.circ`), checks them against a truth
table, offers an AI tutor chat that guides without giving answers, and builds practice circuits
from plain-English descriptions. It is a learning tool: it does not solve graded labs.

## Team

| Member | GitHub | Responsibility |
|--------|--------|----------------|
| 1 `<name>` | `@<user>` | Open and check circuits (parser, simulator, checker) |
| 2 `<name>` | `@<user>` | AI connection, circuit JSON format, Swing UI, AI settings |
| 3 `<name>` | `@<user>` | AI tutor chat |
| 4 `<name>` | `@<user>` | AI circuit builder |

## User stories

- **US1 (1):** As a student, I want to open a Logisim `.circ` file and check it against the truth
  table I expect, so that I can see exactly which rows of my circuit are wrong.
- **US2 (2):** As a student, I want to enter my AI API key once and test the connection, so that
  the AI features work without setup every time.
- **US3 (3):** As a student with a failing circuit, I want to chat with a tutor that knows my
  circuit and nudges me without giving the answer, so that I learn to fix it myself.
- **US4 (4):** As a student, I want to describe a circuit in plain English and get a working
  circuit I can open in Logisim, so that I can study how it is built.

## Requirements

- JDK 21 or newer (`java -version`)
- Maven 3.9+ (`mvn -v`), or just use the Maven tool window in IntelliJ

## How to run

```bash
mvn test        # run all tests
mvn exec:java   # launch the app
```

Optional environment variables: `GEMINI_API_KEY`, `GROQ_API_KEY`, `LOGICOACH_DATA_DIR`.
Never commit keys.

## Architecture

Clean Architecture with one top-level package per layer. Dependencies point inward only:

```
app  ->  view / data_access  ->  interface_adapter  ->  use_case  ->  entity
```

`src/test/java/architecture/DependencyRuleTest.java` fails the build if a layer imports
something it must not. Design choices are logged in [`docs/design-decisions.md`](docs/design-decisions.md).

## Workflow

Nobody pushes to `main`. One branch and one pull request per issue (`Closes #N`), one approving
review required, CI must be green. See [`.github/pull_request_template.md`](.github/pull_request_template.md).
