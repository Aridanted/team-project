# LogiCoach team plan

What is already built, what each person builds next, and how we work together.
Last updated: 2026-10-25.

---

## 1. The app in one paragraph

LogiCoach is a Java Swing desktop app for students learning digital logic. A student opens a
Logisim-evolution `.circ` file, sees the circuit, types the truth table they expect, and sees
which rows pass or fail. An **AI tutor** chats with the student about their circuit and nudges
them toward the fix without giving the answer. An **AI circuit builder** turns a plain-English
description ("an alarm that goes off when exactly one door is open") into a working circuit and
saves it as a `.circ` file that opens in Logisim. It is a learning tool, never a way to do graded
labs.

## 2. Team split

| Person | Owns | Use case (course requirement: one each) |
|--------|------|-----------------------------------------|
| 1 (lead) | Opening and checking circuits | Open a circuit and check it against a truth table (**done**) |
| 2 | AI connection, circuit JSON format, the whole Swing UI | AI settings: save an API key and test the connection |
| 3 | AI tutor | Chat with a tutor about my circuit |
| 4 | AI circuit builder | Build a circuit from a description |

Reviews rotate: **1 reviews 2, 2 reviews 3, 3 reviews 4, 4 reviews 1.**

---

## 3. What is already done

Everything below is on `main`, has tests, and passes `mvn test` (196 tests).

### 3.1 Project setup
- Maven project, Java 21, JUnit 5, `org.json`.
- One package per Clean Architecture layer: `entity`, `use_case`, `interface_adapter`, `view`,
  `data_access`, `app`.
- `DependencyRuleTest` fails the build if a layer imports something it must not (for example an
  entity importing Swing, or a view importing a use case). It runs in CI on every pull request.
- GitHub Actions workflow, pull request template with a SOLID checklist, issue template.
- `docs/design-decisions.md`: log of design choices for the presentation. Add to it.

### 3.2 Circuit model (`entity.circuit`)
Immutable classes describing a circuit independent of any file format:
`Circuit`, `CircuitComponent` (sealed: `InputPin`, `OutputPin`, `Constant`, `Gate`,
`UnsupportedComponent`), `GateType` (AND, OR, NAND, NOR, XOR, XNOR, XOR_ONE, XNOR_ONE, NOT,
BUFFER), `GateInput`, `Wire`, `Point`, `Orientation`, `CircuitWarning`, `LabelOrder`.
Connections are integer **nets**: a gate's inputs read nets, its output drives a net.

### 3.3 Reading `.circ` files (`data_access.circ`)
`LogisimCircuitParser` reads Logisim-evolution 3.7.2 files. Pin positions were measured from
Logisim itself, and a test compares every row of Logisim's own truth tables with ours for 8 test
circuits. Errors (E001 to E005) and warnings (W101 to W105) have clear messages. Details and the
test circuits: `src/test/resources/circuits/README.md`.

### 3.4 Simulation and truth tables (`entity.simulation`, `entity.truthtable`)
- `Logic`: ZERO, ONE, UNKNOWN (unknown = floating, or "don't care" in an expected table).
- `CircuitSimulator` (implements `CircuitEvaluator`): outputs for given inputs; rejects feedback
  loops and unsupported components. Gate behaviour is pluggable (`GateFunction`, `GateFunctions`).
- `TruthTableGenerator`: the full table of a circuit (up to 10 inputs).
- `TruthTableTextFormat`: reads and writes the typed format below, with line-numbered errors.
- `TruthTableChecker`: compares a circuit with an expected table -> `CheckResult`.

```
inputs: A, B
outputs: Sum, Cout
0 0 | 0 0
0 1 | 1 0
1 0 | 1 0
1 1 | 0 1
```

### 3.5 Use cases with their adapters (ready for the UI)

| Use case | Controller (call it) | View model (listen to it) |
|----------|----------------------|---------------------------|
| Open a circuit (`use_case.import_circuit`) | `ImportCircuitController.execute(String filePath)` | `ImportCircuitViewModel` -> `ImportCircuitState`: circuit name, summary, labels, `DrawableComponent`s and `DrawableWire`s for drawing, warnings, error |
| Check a circuit (`use_case.check_circuit`) | `CheckCircuitController.execute(String specText)` | `CheckCircuitViewModel` -> `CheckCircuitState`: labels, `ResultRow`s ("PASS" / "FAIL (Sum)"), summary ("3/4 rows pass."), error |

`InMemorySessionStore` (`data_access.session`) keeps the open circuit and its latest check
result. Opening another circuit clears the old check. Persons 3 and 4 read from it through their
own small interfaces (add `implements YourInterface` to it).

### 3.6 Things teammates should reuse

```java
// Build circuits in tests without files (src/test/java/entity/circuit/CircuitTestFactory.java)
Circuit c = CircuitTestFactory.halfAdder();
Circuit d = CircuitTestFactory.builder("mine")
        .input("A").input("B")
        .gate("g1", GateType.AND, "A", "!B")      // "!B" = negated input
        .output("Y", "g1")
        .build();

// Simulate and tabulate
Map<String, Logic> out = new CircuitSimulator().evaluate(c, Map.of("A", Logic.ONE, "B", Logic.ZERO));
TruthTable table = new TruthTableGenerator(new CircuitSimulator()).generate(c);
String text = new TruthTableTextFormat().format(table);

// Read a .circ file (only in data_access or app; use cases go through their interface)
Circuit fromFile = new LogisimCircuitParser().load("/path/to/file.circ");
```

---

## 4. Rules for everyone

1. **Never push to `main`.** One branch per issue (`feature/<issue#>-short-name`), one pull
   request, `Closes #<issue#>` in the description, one approving review, CI green.
2. **Run `mvn test` before every push.** The dependency rule test must pass.
3. **SOLID is the grade.** Small classes with one job; depend on interfaces you own; inject
   dependencies through constructors (only `app.AppBuilder` creates concrete classes); add
   behaviour with new classes rather than growing `if`/`switch` chains.
4. **Clean Architecture per use case** (like the course login example):
   `View -> Controller -> InputBoundary (Interactor) -> OutputBoundary (Presenter) -> ViewModel -> View`.
   Use cases define the interfaces they need (`...DataAccessInterface`, gateways); `data_access`
   implements them.
5. **Tests use fakes, never the network or real API keys.** Use `@TempDir` for files.
6. **Never commit API keys** (`.env`, `config.properties` and `data/` are git-ignored). If a key is
   pushed, revoke it at the provider immediately.
7. **No CSC258 material** (handouts, starter files, solutions) in the repo or in AI prompts.
8. **Java 21**: install JDK 21 or newer. In IntelliJ: File > Project Structure > SDK 21.
9. **Commit often** with clear messages. The TA reads the git history per person.

---

## 5. Person 2: AI connection, JSON format, Swing UI, AI settings

You build the bridge between the app and the AI, and every screen. Do steps 1 to 3 in the first
days: Persons 3 and 4 depend on them.

### Step 1: The circuit JSON format (do first)
The one format the app and the AI use to describe circuits.

```json
{
  "name": "half adder",
  "inputs": ["A", "B"],
  "outputs": ["Sum", "Cout"],
  "gates": [
    {"id": "g1", "type": "XOR", "inputs": ["A", "B"]},
    {"id": "g2", "type": "AND", "inputs": ["A", "!B"]}
  ],
  "connections": {"Sum": "g1", "Cout": "g2"}
}
```
- `type` is a `GateType` name; a gate input is an input label or a gate id; `!` negates it.
- `connections` maps each output label to the input label or gate id that drives it.
- Write it up in `docs/circuit-json.md` (rules, limits: at most 10 inputs, 8 outputs, 50 gates).
- **Classes:** `entity.design.CircuitDesign` (record: name, inputs, outputs, gates, connections)
  and `entity.design.GateSpec` (id, type, inputs). Immutable, validated on construction.

### Step 2: JSON conversion (`data_access.json.CircuitJsonCodec`)
- `String toJson(CircuitDesign design)` and `CircuitDesign fromJson(String json) throws DesignFormatException`.
- `CircuitDesign describe(Circuit circuit)` (put this one in `entity.design`, no JSON): turns a
  parsed circuit into a design, naming gates g1, g2, ... in position order. The tutor uses it to
  show the AI the student's circuit.
- Lenient reading of AI replies: take the text from the first `{` to the last `}`; reject missing
  keys, unknown gate types, undeclared signals, with clear messages.
- Tests: round trip, every rejection, `describe` on the half adder.

### Step 3: AI client interface (`data_access.llm`)
- `LlmClient` interface: `String complete(LlmRequest request) throws LlmException`.
- `LlmRequest` (system prompt, list of `LlmMessage` with role USER or ASSISTANT, temperature,
  max output tokens, whether JSON is expected). Multi-turn so the tutor can send the whole chat.
- `LlmException` with a kind: `NO_KEY`, `NETWORK`, `RATE_LIMIT`, `AUTH`, `BAD_RESPONSE`, `BLOCKED`.
- `FakeLlmClient` in test sources: returns canned replies or throws on demand. Everyone uses it.

### Step 4: Real AI clients
- `GeminiClient`: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`,
  header `x-goog-api-key`, body with `systemInstruction`, `contents` (one entry per message,
  roles `user`/`model`), `generationConfig` (temperature, `maxOutputTokens` >= 1024, optional
  `responseMimeType: application/json`). Reply text: `candidates[0].content.parts[*].text`.
  Map HTTP 400/403 -> AUTH, 429 -> RATE_LIMIT, no candidates/blocked -> BLOCKED.
- `GroqClient`: `POST https://api.groq.com/openai/v1/chat/completions`, header
  `Authorization: Bearer <key>`, OpenAI-style `messages`. Reply: `choices[0].message.content`.
- `FallbackLlmClient(primary, secondary)` (Decorator): tries Gemini, then Groq.
- `LlmClientFactory`: builds the right client from saved settings or environment variables
  (`GEMINI_API_KEY`, `GROQ_API_KEY`, `GEMINI_MODEL`, `GROQ_MODEL`); with no key, returns a
  client that always throws `NO_KEY` so offline fallbacks take over.
- Use `java.net.http.HttpClient` (10 s connect, 30 s request timeout), at most one call every 2
  seconds, never log keys or full prompts. Use the cheapest models (for example a Gemini
  "flash-lite" model and a small Groq Llama model), configurable.
- Tests: hand the client canned JSON responses through a small HTTP interface you inject; no
  network.

### Step 5: Main window (`app`, `view`)
- `app.Main` starts Swing on the event thread. `app.AppBuilder` creates every object and wires
  them, with **one private method per feature** (`addCircuitTab()`, `addTutorTab()`,
  `addBuilderTab()`, `addSettingsTab()`) so teammates do not collide in merges.
- `view.MainFrame`: about 1100 x 720, a `JTabbedPane` with tabs **Circuit**, **Tutor**,
  **Builder**, **Settings**.

### Step 6: Circuit tab (uses Person 1's finished use cases)
- "Open .circ..." button with a `JFileChooser` filtered to `.circ` -> `ImportCircuitController`.
- `view.CircuitCanvas` (custom `paintComponent`): draws `DrawableComponent`s (gates as labelled
  boxes, pins as circles with labels) and `DrawableWire`s, scaled to fit.
- Right side: component summary and warnings list. Errors in an inline red label, never a popup.
- Below: spec text area, "Load spec file..." button, "Check" button -> `CheckCircuitController`;
  results `JTable` with a PASS/FAIL text column; summary label.
- Long work (opening files, AI calls) runs in a `SwingWorker`; the window must never freeze.
- Views only import `interface_adapter` classes (enforced by the dependency rule test).

### Step 7: AI settings (your use case, `use_case.ai_settings`)
- Save settings: provider order and API keys -> `JsonSettingsStore` in `data_access.json`
  (file `data/settings.json`, git-ignored, atomic write via temp file + move, `"version": 1`).
- Test connection: sends a tiny request through `LlmClient` and reports "Connected to Gemini" or
  the error in plain words.
- Full template: boundaries, interactor, presenter, view model, controller, `SettingsView` tab.
- Tests: interactor with fakes, store with `@TempDir`.

### Step 8: Tutor and Builder tabs
Build the panels against Person 3's and Person 4's view models and controllers (agree on the
state fields early):
- **Tutor tab:** chat bubbles (student right, tutor left), text box, Send button (Enter key),
  "Hint 1 / 2 / 3" buttons, "thinking..." indicator, "Offline" badge, Clear button.
- **Builder tab:** description box, "Build" button, the generated circuit drawn with
  `CircuitCanvas`, its truth table, "Save as .circ..." and "Use as current circuit" buttons.

---

## 6. Person 3: AI tutor

The student chats with a tutor that knows their circuit and their failing rows. It guides with
questions and hints; it never gives the full answer.

### Step 1: Chat classes (`entity.tutor`)
- `ChatMessage` (role, text, time), `ChatRole` (STUDENT, TUTOR), `Conversation` (immutable;
  `append` returns a new conversation; keeps the last 20 messages).

### Step 2: The use case (`use_case.tutor_chat`)
- Input: the student's message (1 to 500 characters) and a hint level (0 = normal chat, 1 to 3
  = "Hint 1/2/3" buttons, where 1 is a nudge, 2 points at the likely faulty part, 3 explains the
  concept).
- `TutorChatDataAccessInterface`: current circuit, last check result, and get/save the
  conversation. Make `InMemorySessionStore` implement it.
- `TutorGateway` port: `TutorReply reply(TutorRequest request) throws TutorUnavailableException`.
  `TutorRequest` holds the conversation, the circuit (may be absent), up to 6 failing rows, and
  the hint level. `TutorReply` holds the text and whether it came from the AI or offline.
- Interactor steps: validate, build the request, call the gateway, run the safety filter, append
  both messages to the conversation, present.
- Works without an open circuit (general logic questions) and with a passing circuit ("your
  circuit already matches; want to try something harder?").
- Full template: boundaries, interactor, presenter, view model (chat bubbles, busy flag,
  offline badge, error), controller (`send`, `hint`, `clear`).

### Step 3: Safety filter (`TutorSafetyFilter`)
- Remove code blocks and headings, collapse whitespace, keep at most 6 sentences.
- If the reply contains a full Boolean expression for an output (like `Sum = A ^ B ^ Cin`),
  replace it with a generic nudge for that level.
- Empty reply -> treat as unavailable.

### Step 4: Offline tutor (`RuleBasedTutor`) and fallback
- Deterministic hints from the check result: names the failing outputs, says whether failures
  happen only when some input is 1 (or 0), level 2 names the gate type driving that output,
  level 3 explains how that gate type behaves. Never states a full expression.
- `FallbackTutorGateway(primary, offline)` (Decorator): if the AI fails, answer offline and mark
  the reply "Offline". Wired in `AppBuilder`.

### Step 5: AI tutor (`data_access.llm.tutor.LlmTutorGateway`)
- Uses Person 2's `LlmClient` and `CircuitJsonCodec`/`describe` to show the AI the circuit.
- `TutorPromptBuilder` makes the system prompt: patient tutor for an intro digital logic course;
  never give the corrected circuit, a full gate list or a full expression; at most 6 sentences,
  plain text; only use the given labels; match the hint level; ask one guiding question.
- Golden-string tests for the prompt; `FakeLlmClient` for the gateway; no network.

### Done when
A student can open a wrong half adder, check it, open the Tutor tab and have a useful
conversation; with no API key the Offline badge shows and hints still work.

---

## 7. Person 4: AI circuit builder

The student describes a circuit in words; the app builds it, shows it, verifies it, and saves a
`.circ` file that opens in Logisim. Label the tab "for practice and learning".

### Step 1: Boolean expressions (`entity.expression`)
- Parser and evaluator for `!` / `~` (NOT), `&` (AND), `^` (XOR), `|` (OR), parentheses, `0`, `1`
  and names. Precedence highest to lowest: NOT, AND, XOR, OR. Errors report the character
  position.
- Used to double-check what the AI claims (Step 3).

### Step 2: The use case (`use_case.build_circuit`)
- Input: a description (1 to 500 characters).
- `CircuitDesignGenerator` port: `GeneratedDesign generate(String description) throws DesignGenerationException`
  (design plus, optionally, one expression per output).
- `DesignValidator`: declared signals only, no loops, at most 10 inputs / 8 outputs / 50 gates,
  every output connected, every output depends on an input. If invalid, ask again (at most 2
  retries), then fail with a clear message.
- `DesignAssembler`: turns a `CircuitDesign` into a `Circuit` (assigns nets).
- Compute the truth table with Person 1's `TruthTableGenerator`; if the AI gave expressions,
  check them against the table and reject designs that disagree. The app never trusts the AI's
  own truth table.
- Output: the design, its truth table, and drawing data (reuse the `DrawableComponent` idea).
- Full template: boundaries, interactor, presenter, view model, controller.

### Step 3: AI designer (`data_access.llm.builder.LlmDesignGenerator`)
- Prompt asks for JSON only, in the shared format (Person 2), with original circuits, a 2 to 4
  sentence explanation, and labels that are valid names.
- Parse with `CircuitJsonCodec`; tests with `FakeLlmClient`.

### Step 4: Writing `.circ` files (`data_access.circ.LogisimCircuitWriter`)
- Implements a use case port `CircuitFileWriterInterface.write(Circuit circuit, String path)`.
- Lays out gates in columns by depth on Logisim's 10-pixel grid, places pins on the left and
  right, routes wires with horizontal and vertical segments, and writes Logisim XML.
- Reuse `GateGeometry` so wires land exactly on Logisim's ports (ask Person 1 if anything is
  unclear).
- **Round-trip test:** write a circuit, read it back with `LogisimCircuitParser`, and compare
  truth tables. They must be equal, with no warnings.

### Step 5: Save and use
- `save_built_circuit` use case: write to the chosen path, then re-read and verify.
- "Use as current circuit": puts the built circuit into the session so Check and Tutor work on it.

### Done when
"An alarm F that sounds when exactly one of doors A and B is open" builds a circuit, shows its
truth table, saves a `.circ` file, and that file opens in Logisim and behaves the same.

---

## 8. Person 1 (lead): remaining work

- Review Person 2's pull requests within 48 hours.
- Answer questions about the parser, simulator, checker and geometry.
- Help Person 4 with the `.circ` writer (the hardest geometry work).
- Run the weekly sync, keep the issue board moving, check that everyone is committing.
- Lead the integration pass and the SOLID audit at the end.

## 9. Order of work

| When | Person 2 | Person 3 | Person 4 |
|------|----------|----------|----------|
| Sprint 0 (first days) | JSON format + `CircuitDesign`; `LlmClient` + `FakeLlmClient`; main window with empty tabs | Chat classes; `TutorGateway` port | Boolean expression parser |
| Sprint 1 | `CircuitJsonCodec`; Gemini and Groq clients; Circuit tab | Use case + safety filter + offline tutor | Use case + validator + assembler |
| Sprint 2 | AI settings; Tutor and Builder tabs | AI tutor gateway and prompts | AI designer; `.circ` writer; save |
| Sprint 3 | Everyone: test with real circuits, fix bugs, SOLID review | | |
| Sprint 4 | Everyone: README, diagrams, presentation rehearsal (twice) | | |

Replace sprint dates with your TA's deadlines.

