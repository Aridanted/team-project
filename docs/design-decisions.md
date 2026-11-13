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

## DD-7: One logic-only circuit format shared by the app and the AI
- Date / author: 2026-11-04 / Person 2
- Context: the tutor must show the AI the student's circuit and the builder must read circuits
  back from the AI. The `.circ` XML is too large and layout-heavy for prompts.
- Decision: `entity.design.CircuitDesign` (names, gates, connections; no positions), validated on
  construction, with JSON conversion kept in `data_access.json.CircuitJsonCodec`. Constants `0`/`1`
  are allowed so circuits with Logisim constants can be described. Rules: `docs/circuit-json.md`.
- SOLID / pattern: Single Responsibility (the record validates, the codec converts, the describer
  translates from `Circuit`); Dependency Rule (no JSON in `entity`).
- Alternatives considered: sending the `Circuit` entity with net numbers (hard for the AI to read
  and to produce correctly); a Boolean expression per output (cannot describe the student's
  actual gates, which the tutor needs).

## DD-8: AI providers behind one interface, combined with decorators
- Date / author: 2026-11-04 / Person 2
- Context: two providers (Gemini, Groq), free tiers with rate limits, keys that change while the
  app runs, and tests that must never use the network.
- Decision: `LlmClient` interface. `GeminiClient` and `GroqClient` call their REST APIs through an
  injected `HttpTransport`. `FallbackLlmClient` (tries the next provider), `RateLimitedLlmClient`
  (one call per 2 s) and `SettingsBackedLlmClient` (rebuilds the chain when saved settings change)
  wrap them; `UnavailableLlmClient` is the null object for "no key". `LlmClientFactory` builds the
  chain from a list of `LlmProviderSpec`s, so a new provider is a new client class plus one spec.
  Errors carry a `Kind` (NO_KEY, NETWORK, RATE_LIMIT, AUTH, BAD_RESPONSE, BLOCKED); messages mask
  the key.
- SOLID / pattern: Strategy (providers), Decorator (fallback, rate limit, reloading), Null Object,
  Factory; Open/Closed (new providers need no factory change); Dependency Inversion (the settings
  use case tests connections through its own `AiConnectionTester` port, not through `LlmClient`).
- Alternatives considered: one client with `if (provider == ...)` branches; a vendor SDK (an extra
  dependency per provider, and harder to fake).

## DD-9: Views update only on the Swing event thread
- Date / author: 2026-11-04 / Person 2
- Context: opening files and AI calls run in `SwingWorker`s so the window never freezes, so
  presenters fire view model changes from background threads.
- Decision: views register listeners through `view.OnEventThread.listener(...)`, which re-posts
  the event to the event thread when needed. Presenters stay free of Swing.
- SOLID / pattern: keeps threading a view concern (Single Responsibility); presenters remain
  testable without Swing.
- Alternatives considered: `SwingUtilities.invokeLater` inside presenters (forbidden by the
  dependency rule, and would tie presenters to Swing).

## DD-10: The tutor reaches the AI through its own port
- Date / author: 2026-11-05 / Person 2 (merging Person 3's first tutor version)
- Context: the first tutor returned a fixed reply and sat outside the per-feature packages; the
  tutor must answer with the AI and see the student's circuit and failing rows.
- Decision: `use_case.tutor_chat` owns a `TutorGateway` port and a `TutorChatDataAccessInterface`
  (implemented by `InMemorySessionStore`). `data_access.llm.tutor.LlmTutorGateway` implements the
  port with `LlmClient`; `TutorPromptBuilder` writes the system prompt with the circuit in the
  shared JSON format and up to 6 failing rows. The conversation is an immutable `entity.tutor`
  value that keeps the last 20 messages.
- SOLID / pattern: Dependency Inversion (the use case never imports `data_access.llm`); Single
  Responsibility (prompt text, AI call and chat rules are separate classes); Open/Closed (the
  offline tutor can wrap the same port as a decorator).
- Alternatives considered: calling `LlmClient` from the interactor (breaks the dependency rule).

## DD-11: Each feature tab wired from AppBuilder.build()
- Date / author: 2026-11-09 / team lead
- Context: the course asks whoever leads a use case to write the UI that triggers it, and four
  people editing one window class would collide in every merge.
- Decision: `app.AppBuilder` is the only class that creates concrete objects. `build()` calls
  private `addXxxTab()` methods on a `view.MainFrame`. The Circuit tab is fully wired; Tutor and
  Builder tabs use `PlaceholderView` until their owners replace them. Views are plain `JPanel`s
  that only see their controllers and view models.
- SOLID / pattern: Builder; Open/Closed (a new feature is a new method, not an edit to others);
  Single Responsibility (object creation lives in one place, the composition root).
- Alternatives considered: one person builds every screen (rejected: breaks the course's "lead
  your use case" rule); a public chained `addXxxTab()` API on `AppBuilder` called from `Main`
  (rejected after merge: Alice's `MainFrame` and private tab methods reduce conflicts).

## DD-12: Checkstyle runs in the build, not only in the IDE
- Date / author: 2026-11-09 / team lead
- Context: code quality is graded and four people write code in different styles.
- Decision: `checkstyle.xml` (naming, imports, braces, whitespace, method length 50, complexity
  10, Javadoc on public types) runs in the `validate` phase of `mvn verify`, so CI rejects pull
  requests that break it. The same file can be loaded in IntelliJ's CheckStyle-IDEA plugin.
- Alternatives considered: the bundled Sun checks (rejected: hundreds of warnings about final
  parameters and magic numbers, which teaches people to ignore the tool).

## DD-13: Expected truth tables are saved as the text the student typed
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
