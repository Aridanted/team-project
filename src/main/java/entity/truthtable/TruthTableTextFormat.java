package entity.truthtable;

/**
 * Reads and writes truth tables in the text format students type:
 * <pre>
 * # comment
 * inputs: A, B
 * outputs: Sum, Cout
 * 0 0 | 0 0
 * 1 1 | 0 1
 * </pre>
 */
public final class TruthTableTextFormat {

    public TruthTable parse(String text) throws TruthTableFormatException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public String format(TruthTable table) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
