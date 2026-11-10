package entity.truthtable;

/** Thrown when truth table text is malformed. The message starts with "line N: ". */
public class TruthTableFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int line;

    /**
     * @param line    1-based line number where the problem is
     * @param problem what is wrong, for example "expected 2 output values, found 1"
     */
    public TruthTableFormatException(int line, String problem) {
        super("line " + line + ": " + problem);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
