package entity.truthtable;

import entity.simulation.Logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final Pattern LABEL = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * Parses truth table text.
     *
     * @param text the table text
     * @return the parsed table
     * @throws TruthTableFormatException if the text is malformed
     */
    public TruthTable parse(String text) throws TruthTableFormatException {
        String[] rawLines = text.split("\\R", -1);
        int lineCount = Math.max(1, rawLines.length);

        List<String> inputLabels = null;
        List<String> outputLabels = null;
        List<TruthTable.Row> rows = new ArrayList<>();
        Set<String> labelsSeen = new HashSet<>();
        Set<String> inputCombinations = new HashSet<>();

        for (int i = 0; i < rawLines.length; i++) {
            int lineNumber = i + 1;
            String trimmed = rawLines[i].trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("inputs:")) {
                if (inputLabels != null) {
                    throw new TruthTableFormatException(lineNumber, "duplicate 'inputs:' line");
                }
                inputLabels = parseHeaderLabels(trimmed.substring(trimmed.indexOf(':') + 1), lineNumber,
                        labelsSeen, true);
                continue;
            }
            if (lower.startsWith("outputs:")) {
                if (inputLabels == null) {
                    throw new TruthTableFormatException(lineNumber, "missing 'inputs:' line");
                }
                if (outputLabels != null) {
                    throw new TruthTableFormatException(lineNumber, "duplicate 'outputs:' line");
                }
                outputLabels = parseHeaderLabels(trimmed.substring(trimmed.indexOf(':') + 1), lineNumber,
                        labelsSeen, false);
                continue;
            }

            if (inputLabels == null) {
                throw new TruthTableFormatException(lineNumber, "missing 'inputs:' line");
            }
            if (outputLabels == null) {
                throw new TruthTableFormatException(lineNumber, "missing 'outputs:' line");
            }
            rows.add(parseRow(trimmed, lineNumber, inputLabels.size(), outputLabels.size(),
                    inputCombinations));
        }

        if (inputLabels == null) {
            throw new TruthTableFormatException(lineCount, "missing 'inputs:' line");
        }
        if (outputLabels == null) {
            throw new TruthTableFormatException(lineCount, "missing 'outputs:' line");
        }
        if (rows.isEmpty()) {
            throw new TruthTableFormatException(lineCount, "the table has no rows");
        }

        return new TruthTable(inputLabels, outputLabels, rows);
    }

    /**
     * Formats a truth table as text.
     *
     * @param table the table to format
     * @return the text representation
     */
    public String format(TruthTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append("inputs: ").append(String.join(", ", table.inputLabels())).append('\n');
        sb.append("outputs: ").append(String.join(", ", table.outputLabels())).append('\n');
        for (TruthTable.Row row : table.rows()) {
            appendValues(sb, row.inputs());
            sb.append(" | ");
            appendValues(sb, row.outputs());
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void appendValues(StringBuilder sb, List<Logic> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(values.get(i).symbol());
        }
    }

    private List<String> parseHeaderLabels(String afterColon, int lineNumber, Set<String> labelsSeen,
                                           boolean inputs) throws TruthTableFormatException {
        List<String> labels = new ArrayList<>();
        for (String token : splitTokens(afterColon)) {
            if (!LABEL.matcher(token).matches()) {
                throw new TruthTableFormatException(lineNumber, "invalid label \"" + token + "\"");
            }
            String key = token.toLowerCase(Locale.ROOT);
            if (!labelsSeen.add(key)) {
                throw new TruthTableFormatException(lineNumber, "duplicate label \"" + token + "\"");
            }
            labels.add(token);
        }
        if (inputs && labels.isEmpty()) {
            throw new TruthTableFormatException(lineNumber, "no input labels");
        }
        if (!inputs && labels.isEmpty()) {
            throw new TruthTableFormatException(lineNumber, "no output labels");
        }
        return labels;
    }

    private TruthTable.Row parseRow(String line, int lineNumber, int inputCount, int outputCount,
                                    Set<String> inputCombinations) throws TruthTableFormatException {
        int bar = line.indexOf('|');
        if (bar < 0) {
            throw new TruthTableFormatException(lineNumber, "expected '|' between inputs and outputs");
        }
        List<Logic> inputs = parseInputValues(splitTokens(line.substring(0, bar)), lineNumber, inputCount);
        List<Logic> outputs = parseOutputValues(splitTokens(line.substring(bar + 1)), lineNumber, outputCount);

        String combinationKey = combinationKey(inputs);
        if (!inputCombinations.add(combinationKey)) {
            throw new TruthTableFormatException(lineNumber,
                    "duplicate row for inputs " + combinationKey);
        }
        return new TruthTable.Row(inputs, outputs);
    }

    private static String combinationKey(List<Logic> inputs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(inputs.get(i).symbol());
        }
        return sb.toString();
    }

    private List<Logic> parseInputValues(List<String> tokens, int lineNumber, int expectedCount)
            throws TruthTableFormatException {
        if (tokens.size() != expectedCount) {
            throw new TruthTableFormatException(lineNumber,
                    "expected " + expectedCount + " input values, found " + tokens.size());
        }
        List<Logic> values = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            values.add(parseInputValue(token, lineNumber));
        }
        return values;
    }

    private List<Logic> parseOutputValues(List<String> tokens, int lineNumber, int expectedCount)
            throws TruthTableFormatException {
        if (tokens.size() != expectedCount) {
            throw new TruthTableFormatException(lineNumber,
                    "expected " + expectedCount + " output values, found " + tokens.size());
        }
        List<Logic> values = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            values.add(parseOutputValue(token, lineNumber));
        }
        return values;
    }

    private Logic parseInputValue(String token, int lineNumber) throws TruthTableFormatException {
        if (token.equals("0")) {
            return Logic.ZERO;
        }
        if (token.equals("1")) {
            return Logic.ONE;
        }
        throw new TruthTableFormatException(lineNumber, "invalid input value \"" + token + "\"");
    }

    private Logic parseOutputValue(String token, int lineNumber) throws TruthTableFormatException {
        if (token.equals("0")) {
            return Logic.ZERO;
        }
        if (token.equals("1")) {
            return Logic.ONE;
        }
        if (token.equals("X") || token.equals("x")) {
            return Logic.UNKNOWN;
        }
        throw new TruthTableFormatException(lineNumber, "invalid output value \"" + token + "\"");
    }

    private static List<String> splitTokens(String part) {
        String[] raw = part.trim().split("[\\s,]+");
        List<String> tokens = new ArrayList<>();
        for (String token : raw) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
