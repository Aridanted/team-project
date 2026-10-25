# LogiCoach

A Java Swing app that imports Logisim-evolution circuits (`.circ`), checks them against a truth
table, gives Socratic AI hints (with an offline fallback) and tracks practice progress.
It is a tutor and checker: it does not solve graded labs.

## Team

| Member | GitHub | Use case |
|--------|--------|----------|
| A `<name>` | `@<user>` | UC1 import and view a circuit |
| B `<name>` | `@<user>` | UC2 check against a truth table |
| C `<name>` | `@<user>` | UC3 get a hint |
| D `<name>` | `@<user>` | UC4 practice problems and progress |

## User stories

- **US1 (A):** As a student, I want to open a Logisim `.circ` file and see its components, pins
  and connections, so that I can confirm LogiCoach understood my circuit.
- **US2 (B):** As a student, I want to enter an expected truth table and see which rows of my
  circuit pass or fail, so that I can find where my circuit is wrong.
- **US3 (C):** As a student with a failing circuit, I want graduated hints that nudge me without
  giving the answer, so that I learn to fix it myself.
- **US4 (D):** As a student, I want practice problems on a topic and a record of my attempts, so
  that I can see my progress over time.

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
