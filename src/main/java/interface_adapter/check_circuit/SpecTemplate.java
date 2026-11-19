package interface_adapter.check_circuit;

import java.util.List;

/**
 * Starting text for the expected truth table of a newly opened circuit: the header lines and,
 * for small circuits, every input row with {@code ?} where the student fills in outputs.
 */
public final class SpecTemplate {

    /** Above this many inputs only the header is written (2^6 = 64 rows is still readable). */
    static final int MAX_INPUTS_WITH_ROWS = 6;

    private SpecTemplate() {
    }

    public static String forLabels(List<String> inputs, List<String> outputs) {
        StringBuilder text = new StringBuilder();
        text.append("# Replace each ? with the output you expect: 0, 1, or X for \"don't care\".\n");
        text.append("inputs: ").append(String.join(", ", inputs)).append('\n');
        text.append("outputs: ").append(String.join(", ", outputs)).append('\n');
        if (inputs.isEmpty() || inputs.size() > MAX_INPUTS_WITH_ROWS) {
            return text.toString();
        }
        String blanks = String.join(" ", outputs.stream().map(output -> "?").toList());
        int rows = 1 << inputs.size();
        for (int row = 0; row < rows; row++) {
            for (int bit = inputs.size() - 1; bit >= 0; bit--) {
                text.append((row >> bit) & 1).append(' ');
            }
            text.append("| ").append(blanks).append('\n');
        }
        return text.toString();
    }
}
