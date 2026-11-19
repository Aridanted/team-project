package data_access.llm.builder;

/** Builds the instructions for generating a circuit in the shared JSON format. */
public final class CircuitDesignPromptBuilder {

    /**
     * Returns the circuit generation instructions.
     *
     * @return the system prompt sent to the AI
     */
    public String systemPrompt() {
        return """
                Design an original combinational logic circuit for practice and learning.
                Follow the student's requested behavior.

                Return exactly one JSON object with these keys:
                - "name": a circuit name between 1 and 60 characters.
                - "inputs": an array of input labels.
                - "outputs": an array of output labels.
                - "gates": an array of objects with "id", "type", and "inputs".
                - "connections": an object mapping every output label to its driver.
                - "explanation": a string containing 2 to 4 sentences explaining the circuit.
                - "expressions": an optional object mapping output labels to Boolean expressions.

                Use between 1 and 10 inputs, between 1 and 8 outputs, and at most 50 gates.
                Signal names must start with an ASCII letter and contain only ASCII letters,
                digits, or underscores, with at most 32 characters.
                Input labels, output labels, and gate IDs must all differ ignoring case.
                References must use the exact spelling of the declared name.

                Supported gate types are AND, OR, NAND, NOR, XOR, XNOR, XOR_ONE,
                XNOR_ONE, NOT, and BUFFER.
                NOT and BUFFER take exactly one input. Other gates take 2 to 32 inputs.
                XOR and XNOR use parity. XOR_ONE and XNOR_ONE use exactly-one semantics.

                A gate input may reference an input label, a gate ID, or the constants "0"
                and "1". Prefix a gate input with "!" to negate it.
                Gates may reference gates listed later.
                Gates must never reference output labels.
                Every output must connect to an input label, gate ID, or constant.
                Output connections cannot contain negation. Add a NOT gate when needed.

                The circuit must contain no feedback loops.
                Every output must have a path back to at least one circuit input.

                Output expressions may use only declared input labels, 0, 1,
                parentheses, ! or ~ for NOT, & for AND, ^ for XOR, and | for OR.
                Operator precedence is NOT, AND, XOR, then OR.
                Expressions must agree with the circuit for every input combination.

                Return JSON only. Put the explanation inside the JSON object.
                """;
    }
}
