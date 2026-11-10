# Code guide: start here

You do **not** need to read the whole codebase. This page tells you which ten files matter, which
folder you can skip, and exactly which files to write for your own use case.

## The app in one picture

```
 view/             CircuitView, TruthTableView        (Swing panels: buttons, text, drawing)
   │ calls                         ▲ listens to
   ▼                               │
 interface_adapter/  Controller ──► ... ──► Presenter ──► ViewModel (State)
   │ calls                         ▲ calls
   ▼                               │
 use_case/           Interactor (the rules of one feature)
   │ uses                          │ asks through an interface it owns
   ▼                               ▼
 entity/             Circuit, TruthTable, CircuitSimulator   data_access/  .circ parser, files, session
```

Arrows only point inward: `view` never imports `use_case`, `entity` never imports Swing, and so
on. `DependencyRuleTest` fails the build if anyone breaks this. `app/AppBuilder` is the one
place that creates every object and plugs them together.

## Read these first (about 30 minutes)

| # | File | Why |
|---|------|-----|
| 1 | `app/AppBuilder.java` | Shows every piece of the Circuit tab being created and connected. |
| 2 | `use_case/table_file/TableFileInteractor.java` | The smallest complete use case. Copy its shape. |
| 3 | `use_case/table_file/*` (the other 7 files) | Input data, output data, boundaries, the data access interface. All tiny. |
| 4 | `interface_adapter/table_file/*` | Controller, presenter, view model and state for that use case. |
| 5 | `view/TruthTableView.java` | How a Swing panel calls a controller and redraws when the view model changes. |
| 6 | `entity/circuit/Circuit.java` | The circuit model everyone shares. |
| 7 | `entity/simulation/CircuitSimulator.java` | Computes a circuit's outputs for given inputs. |
| 8 | `entity/truthtable/TruthTable.java` and `CheckResult.java` | A truth table, and the result of checking a circuit against one. |
| 9 | `data_access/session/InMemorySessionStore.java` | Where the open circuit and its last check live, shared between features. |
| 10 | `src/test/java/entity/circuit/CircuitTestFactory.java` | Build circuits in your tests in one line. |

## You can skip

- **`data_access/circ/`** (the Logisim `.circ` parser, about 1,300 lines). It turns a file into a
  `Circuit`, matching Logisim exactly, and is heavily tested. You only ever call
  `new LogisimCircuitParser().load(path)`, and only in `app` or `data_access`. If something in it
  looks wrong, ask Person 1.
- `tools/logisim/`: scripts that regenerate the test circuits from Logisim itself.

## One click, end to end

What happens when the student presses **Check**:

1. `TruthTableView` calls `checkController.execute(editorText)`.
2. `CheckCircuitController` wraps the text in `CheckCircuitInputData` and calls the interactor
   through `CheckCircuitInputBoundary`.
3. `CheckCircuitInteractor` gets the open circuit from the session
   (`CheckCircuitDataAccessInterface`), parses the text into a `TruthTable`, runs
   `TruthTableChecker`, stores the `CheckResult` in the session, and calls the presenter.
4. `CheckCircuitPresenter` turns the result into display text ("PASS", "FAIL (Sum)",
   "3/4 rows pass.") in a new `CheckCircuitState` and fires a change on `CheckCircuitViewModel`.
5. `TruthTableView` hears the change and fills the results table.

Every feature works this way. Opening a circuit (`import_circuit`) and saving / loading tables
(`table_file`) follow the same five steps.

## Words used in the code

| Word | Meaning |
|------|---------|
| circuit | An immutable `Circuit`: components plus wires. Format-independent. |
| component | One of `InputPin`, `OutputPin`, `Constant`, `Gate`, `UnsupportedComponent`. |
| net | A number for one electrical connection. A gate *reads* some nets and *drives* one. Wires only matter for drawing. |
| `Logic` | A signal: `ZERO`, `ONE` or `UNKNOWN` (written `X`; a floating wire, or "don't care" in a table). |
| expected table | The truth table the student types (sometimes called "spec" in older code). |
| session | `InMemorySessionStore`: the open circuit and its latest `CheckResult`, shared by all features. |
| interactor | The class that holds one use case's rules. |
| boundary | An interface between layers: input boundary (called by the controller), output boundary (implemented by the presenter). |

## Adding your use case: the checklist

Use `table_file` as your template. For a use case called `tutor_chat`:

1. **`use_case/tutor_chat/`**
   - `TutorChatInputData` (record): what the user typed or clicked.
   - `TutorChatInputBoundary` (interface): `void execute(TutorChatInputData)`.
   - `TutorChatOutputData` (record): what the screen needs, as plain values.
   - `TutorChatOutputBoundary` (interface): `prepareSuccessView`, `prepareFailView`.
   - `TutorChatDataAccessInterface` (interface): only the methods *your* interactor needs.
   - `TutorChatInteractor`: the rules. Takes everything through its constructor.
2. **`interface_adapter/tutor_chat/`**: `TutorChatController`, `TutorChatPresenter`,
   `TutorChatState`, `TutorChatViewModel` (extends `interface_adapter.ViewModel`).
3. **`data_access/...`**: the class that implements your data access interface. If you need the
   open circuit, add `implements TutorChatDataAccessInterface` to `InMemorySessionStore`.
4. **`view/TutorView.java`**: a `JPanel` that calls your controller and listens to your view model.
   Views may only import `interface_adapter` classes.
5. **`app/AppBuilder.java`**: replace the placeholder in your `addTutorTab()` or `addBuilderTab()`
   method with your view (wired like the Circuit tab). Don't edit the other private `addXxxTab()`
   methods.
6. **Tests**: test the interactor with small fake classes (see `TableFileInteractorTest`), and
   the presenter by reading the view model's state (see `TableFilePresenterTest`). Never call
   the network or use a real API key in a test.

## Before you push

```bash
mvn verify        # compiles, runs Checkstyle, runs every test (CI runs the same command)
```

- **Checkstyle** rules are in `checkstyle.xml`. To see problems while you type in IntelliJ,
  install the *CheckStyle-IDEA* plugin and add `checkstyle.xml` as a configuration.
- **`DependencyRuleTest`** tells you exactly which import broke the layer rules.
- To run the app: `mvn compile exec:java`.

View helpers: `view.Styles` (colours and labels), `view.OnEventThread` (safe view-model
listeners), and `view.BackgroundTask` (short background jobs with a busy state). For AI tests use
`data_access.llm.FakeLlmClient` and `FakeHttpTransport` in test sources (no network).
Circuit JSON rules: [`circuit-json.md`](circuit-json.md).

Design choices and the reasons behind them are in [`design-decisions.md`](design-decisions.md);
who builds what is in [`team-plan.md`](team-plan.md).
