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
