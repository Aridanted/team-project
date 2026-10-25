# Circuit fixtures

All fixtures are our own practice circuits (no course material). They were saved by
**Logisim-evolution 3.7.2**, so they are real Logisim files, not hand-typed approximations.
Use the same version when adding fixtures so geometry stays consistent.

| File | Purpose |
|------|---------|
| `half_adder.circ` | default-size XOR + AND, T-junction on a wire interior, a crossing that must not connect |
| `full_adder.circ` | narrow (size 30) gates, several junctions and crossings |
| `mux2.circ` | NOT geometry, 2:1 multiplexer from basic gates |
| `xor_from_nand.circ` | NAND geometry, a net shared by three wires |
| `gate_sizes.circ` | every gate type at sizes 30/50/70 with 2 to 8 inputs, XOR "odd" and "exactly one" |
| `rotated.circ` | gates facing north, south and west; NOT and Buffer rotated |
| `negated_inputs.circ` | negated inputs on east, west, north and south facing gates |
| `constants.circ` | Constant (0 and default 1), Power, Ground |
| `unsupported.circ` | flip-flop, multiplexer, LED, tunnel, clock, subcircuit (W101); text and probe (ignored) |
| `floating.circ` | unconnected gate input and output pin (W102), wire stopping short of a port (W104) |
| `unlabelled_pins.circ` | auto-naming of unlabelled pins (W103) |
| `duplicate_labels.circ` | E003; hand-written, because Logisim itself blanks duplicate labels on load |
| `contention.circ` | two outputs driving one net (E004) |
| `multibit.circ` | 4-bit pins (E005) |

`<name>.table.txt` is the truth table printed by Logisim itself
(`java -jar logisim-evolution-3.7.2-all.jar -t table <name>.circ`).
`LogisimTruthTableAgreementTest` checks that every row matches what the parsed circuit computes.

## Regenerating

The scripts live in `tools/logisim/` (`JAR` is the Logisim-evolution jar, for example
`/Applications/Logisim-evolution.app/Contents/app/logisim-evolution-3.7.2-all.jar`):

```bash
cd tools/logisim
java GenerateFixtures.java                       # writes source files to build/
javac -cp "$JAR" Normalize.java Probe.java
java -Djava.awt.headless=true -cp "$JAR:." Normalize build/half_adder.circ ../../src/test/resources/circuits/half_adder.circ
java -Djava.awt.headless=true -jar "$JAR" -t table ../../src/test/resources/circuits/half_adder.circ \
    > ../../src/test/resources/circuits/half_adder.table.txt
java -Djava.awt.headless=true -cp "$JAR:." Probe  # prints Logisim's port positions (GateGeometryTest data)
```
