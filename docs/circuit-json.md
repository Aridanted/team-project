# Circuit JSON format

The one format the app and the AI use to describe a circuit's logic. It has no positions or
wires; the `.circ` writer lays those out. In code it is `entity.design.CircuitDesign` (with
`GateSpec`), converted by `data_access.json.CircuitJsonCodec`.

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

## Keys

| Key | Type | Meaning |
|-----|------|---------|
| `name` | string | Display name, 1 to 60 characters (surrounding spaces are removed). |
| `inputs` | list of names | Input labels, in order. |
| `outputs` | list of names | Output labels, in order. |
| `gates` | list of gates | Each gate has `id` (a name), `type` and `inputs`. |
| `connections` | object | For every output label, the input label, gate id or constant that drives it. |

Other keys (for example an AI's `explanation`) are ignored when reading.

## Rules

- **Names** (input labels, output labels, gate ids): a letter, then letters, digits or `_`, at
  most 32 characters. The same rule as truth table labels. All names in a design must differ,
  **ignoring case** (`a` and `A` clash), because Logisim treats labels case-insensitively.
  References must use the exact spelling.
- **Gate types** are `GateType` names: `AND`, `OR`, `NAND`, `NOR`, `XOR`, `XNOR`, `XOR_ONE`,
  `XNOR_ONE`, `NOT`, `BUFFER`. Reading accepts any case (`xor`); writing uses upper case.
  `XOR`/`XNOR` are parity; `XOR_ONE`/`XNOR_ONE` mean "exactly one input is 1" (same as XOR for
  two inputs).
- **Gate inputs**: `NOT` and `BUFFER` take exactly 1; every other type takes 2 to 32. Each input
  is an input label, a gate id, or the constant `0` or `1`. Prefix it with `!` to negate it
  (`"!B"`). A gate may use a gate listed later. A gate may **not** read an output label; use
  whatever drives that output instead.
- **Connections**: every output appears exactly once; no other keys. The driver is an input label,
  a gate id, `0` or `1`. Outputs cannot be negated (`"!g1"` is rejected): add a `NOT` gate.

## Limits

| What | Limit |
|------|-------|
| Inputs | 1 to 10 (the truth table generator's limit, 1024 rows) |
| Outputs | 1 to 8 |
| Gates | 0 to 50 |
| Inputs per gate | 2 to 32 (`NOT`/`BUFFER`: exactly 1) |
| Name length | 32 characters (circuit name: 60) |

## Not checked by the format

The format checks structure only. The builder's `DesignValidator` (Person 4) also rejects
feedback loops and outputs that do not depend on any input.

## Reading AI replies

`CircuitJsonCodec.fromJson` takes the text from the first `{` to the last `}`, so replies like
"Here is your circuit: ```json {...} ``` Hope this helps" are fine. Every problem is reported as a
`DesignFormatException` with a message that names the gate or key, for example:

- `The circuit is missing "connections".`
- `Gate g1 has unknown type "MAJORITY". Use one of: AND, OR, ...`
- `Gate g2 uses C, which is not a declared input or gate.`
- `Output Cout is not connected to anything.`

These messages are written so they can be sent back to the AI when asking it to try again.

## Describing a parsed circuit

`entity.design.CircuitDescriber.describe(Circuit)` turns a circuit read from a `.circ` file into
this format, for example so the tutor can show the AI the student's circuit:

- Gates are named `g1`, `g2`, ... from left to right, then top to bottom; a name already used by a
  pin is skipped.
- Constants become `0` and `1`; negated gate inputs keep their `!`.
- It throws `NotDescribableException` (with a message for the student) if the circuit has
  unsupported components, an unconnected gate input or output, a wire driven by several
  components, labels that are not valid names, or exceeds the limits.

`toJson` writes compact JSON with keys always in the order above, so prompt tests can compare
exact strings.
