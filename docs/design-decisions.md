# Design decisions

A running log of design choices. Add an entry whenever you make a decision a reviewer or TA
might ask about. Newest at the bottom.

Template:

```
## DD-<n>: <title>
- Date / author:
- Context: what problem we faced
- Decision: what we chose
- SOLID / pattern: which principle or pattern it serves
- Alternatives considered: and why we rejected them
```

## DD-1: One top-level package per Clean Architecture layer
- Date / author: 2026-10-25 / team lead
- Context: four people work in parallel and are graded on SOLID and Clean Architecture.
- Decision: packages `entity`, `use_case`, `interface_adapter`, `view`, `data_access`, `app`,
  with one sub-package per use case inside the middle layers.
- SOLID / pattern: Dependency Inversion (inner layers own the interfaces outer layers implement);
  Single Responsibility at the package level.
- Alternatives considered: package-by-feature only; rejected because layer boundaries become
  invisible and harder to check.

## DD-2: Automated dependency rule test instead of a library
- Date / author: 2026-10-25 / team lead
- Context: dependency rule violations are easy to introduce and hard to spot in review.
- Decision: `DependencyRuleTest` scans imports of every main source file per layer and fails the
  build on a forbidden import. Runs in CI on every pull request.
- SOLID / pattern: protects Dependency Inversion and keeps entities and use cases framework-free.
- Alternatives considered: ArchUnit (more powerful, but an extra library the course does not use).

## DD-3: Java 21 and the JDK HTTP client
- Date / author: 2026-10-25 / team lead
- Context: the app calls LLM REST APIs and needs a modern, widely installed Java.
- Decision: Java 21 (LTS). HTTP via `java.net.http.HttpClient`, hidden behind our own `LlmClient`
  interface, so no HTTP library dependency.
- SOLID / pattern: Dependency Inversion; Strategy (swap LLM providers behind one interface).
- Alternatives considered: OkHttp; rejected as an unnecessary dependency since the JDK client suffices.

## DD-4: Component model as a sealed interface of immutable records
- Date / author: 2026-10-25 / team lead
- Context: a circuit holds pins, constants, gates and unsupported components with different data.
- Decision: `CircuitComponent` is a sealed interface implemented by records (`InputPin`,
  `OutputPin`, `Constant`, `Gate`, `UnsupportedComponent`). Every component exposes
  `inputNets()` and `drivenNet()`, so connectivity code treats them uniformly.
- SOLID / pattern: Liskov (any component works wherever a `CircuitComponent` is expected);
  Interface Segregation (`Pin` adds only what pins need); Open/Closed along the axis that changes:
  new operations (simulate, draw, describe) are new classes, while the set of kinds is fixed by scope.
- Alternatives considered: one `Component` class with nullable fields per kind; rejected because
  half its fields would be meaningless for any given component.

## DD-5: Parser split into single-purpose steps, components resolved through a registry
- Date / author: 2026-10-25 / team lead
- Context: the .circ parser is the riskiest module and must be easy to extend and test.
- Decision: `LogisimCircuitParser` (implements the use case's `CircuitFileDataAccessInterface`)
  only coordinates `CircXmlReader`, `ComponentRegistry`, `NetBuilder`, `PinLabeler` and
  `ConnectivityChecker`. Each Logisim component kind is a `ComponentDefinition` strategy; adding
  one (for example Tunnel) is a new class plus one registry line.
- SOLID / pattern: Single Responsibility; Open/Closed; Dependency Inversion (the use case owns the
  interface); Strategy and Factory.
- Alternatives considered: one large parser class with a switch on component names.

## DD-6: Logisim geometry measured, not guessed
- Date / author: 2026-10-25 / team lead
- Context: port positions are not stored in .circ files and the spec only guessed them.
- Decision: the numbers in `GateGeometry` and `ComponentRegistry` were read from
  Logisim-evolution 3.7.2's own component factories (`tools/logisim/Probe.java`). Fixtures are
  re-saved by Logisim and every fixture's truth table, printed by Logisim, must match the parsed
  circuit (`LogisimTruthTableAgreementTest`).
- Notes: Logisim connects a wire end or a port that lies on another wire's interior, and treats
  pin labels case-insensitively (it blanks duplicates on load). Logisim ignores floating gate
  inputs by default (`gateUndefined=ignore`); LogiCoach treats them as unknown and warns (W102).

## DD-7: Each feature adds its own tab through its own AppBuilder method
- Date / author: 2026-11-09 / team lead
- Context: the course asks whoever leads a use case to write the UI that triggers it, and four
  people editing one window class would collide in every merge.
- Decision: `app.AppBuilder` is the only class that creates concrete objects. Each feature has one
  public method (`addCircuitTab()`, `addTutorTab()`, ...) that builds its use case, adapters and
  view and adds a tab. `Main` chains the calls. Views are plain `JPanel`s that only see their
  controllers and view models.
- SOLID / pattern: Builder; Open/Closed (a new feature is a new method, not an edit to others);
  Single Responsibility (object creation lives in one place, the composition root).
- Alternatives considered: one person builds every screen (rejected: breaks the course's "lead
  your use case" rule); a `MainFrame` class that knows every view (rejected: every feature would
  edit it).

## DD-8: Checkstyle runs in the build, not only in the IDE
- Date / author: 2026-11-09 / team lead
- Context: code quality is graded and four people write code in different styles.
- Decision: `checkstyle.xml` (naming, imports, braces, whitespace, method length 50, complexity
  10, Javadoc on public types) runs in the `validate` phase of `mvn verify`, so CI rejects pull
  requests that break it. The same file can be loaded in IntelliJ's CheckStyle-IDEA plugin.
- Alternatives considered: the bundled Sun checks (rejected: hundreds of warnings about final
  parameters and magic numbers, which teaches people to ignore the tool).

## DD-9: Expected truth tables are saved as the text the student typed
- Date / author: 2026-11-09 / team lead
- Context: the project must show data persistence, and students re-check the same circuit many
  times.
- Decision: the `table_file` use case saves the editor text unchanged (comments included) to a
  `.txt` file, but only if it parses, so every saved file can be loaded and checked. Files are
  read and written by `TableFileStore` behind the use case's `TableFileDataAccessInterface`.
- SOLID / pattern: Dependency Inversion (the use case owns the file interface, so tests use an
  in-memory fake); Single Responsibility (the format lives in `TruthTableTextFormat` only).
- Alternatives considered: JSON (rejected: students can't edit it by hand, and the typed format
  already exists); saving the parsed table (rejected: loses the student's comments).
