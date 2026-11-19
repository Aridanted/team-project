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
 * Input values are 0 or 1. Output values are 0, 1, or X for "don't care".
 */
public final class TruthTableTextFormat {

    private static final Pattern LABEL = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /**
     * @param text the table text
     * @return the parsed table
     * @throws TruthTableFormatException if the text is malformed; the message names the line
     */
    public TruthTable parse(String text) throws TruthTableFormatException {
        String[] lines = text.split("\\R", -1);
        Reader reader = new Reader();
        for (int i = 0; i < lines.length; i++) {
            reader.readLine(lines[i].trim(), i + 1);
        }
        return reader.finish(Math.max(1, lines.length));
    }

    /**
     * @param table the table to format
     * @return the table as text, readable again by {@link #parse(String)}
     */
    public String format(TruthTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append("inputs: ").append(String.join(", ", table.inputLabels())).append('\n');
        sb.append("outputs: ").append(String.join(", ", table.outputLabels())).append('\n');
        for (TruthTable.Row row : table.rows()) {
            sb.append(symbols(row.inputs())).append(" | ").append(symbols(row.outputs())).append('\n');
        }
        return sb.toString();
    }

    private static String symbols(List<Logic> values) {
        StringBuilder sb = new StringBuilder();
        for (Logic value : values) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(value.symbol());
        }
        return sb.toString();
    }

    /** Reads one table, line by line, remembering what it has seen so far. */
    private static final class Reader {

        private List<String> inputLabels;
        private List<String> outputLabels;
        private final List<TruthTable.Row> rows = new ArrayList<>();
        private final Set<String> labelsSeen = new HashSet<>();
        private final Set<List<Logic>> inputCombinationsSeen = new HashSet<>();

        void readLine(String line, int lineNumber) throws TruthTableFormatException {
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.isEmpty() || line.startsWith("#")) {
                return;
            }
            if (lower.startsWith("inputs:")) {
                readInputsHeader(line, lineNumber);
            } else if (lower.startsWith("outputs:")) {
                readOutputsHeader(line, lineNumber);
            } else {
                requireHeaders(lineNumber);
                readRow(line, lineNumber);
            }
        }

        TruthTable finish(int lastLine) throws TruthTableFormatException {
            requireHeaders(lastLine);
            if (rows.isEmpty()) {
                throw new TruthTableFormatException(lastLine, "the table has no rows");
            }
            return new TruthTable(inputLabels, outputLabels, rows);
        }

        private void readInputsHeader(String line, int lineNumber) throws TruthTableFormatException {
            if (inputLabels != null) {
                throw new TruthTableFormatException(lineNumber, "duplicate 'inputs:' line");
            }
            inputLabels = labelsAfterColon(line, lineNumber, "no input labels");
        }

        private void readOutputsHeader(String line, int lineNumber) throws TruthTableFormatException {
            if (inputLabels == null) {
                throw new TruthTableFormatException(lineNumber, "missing 'inputs:' line");
            }
            if (outputLabels != null) {
                throw new TruthTableFormatException(lineNumber, "duplicate 'outputs:' line");
            }
            outputLabels = labelsAfterColon(line, lineNumber, "no output labels");
        }

        private void requireHeaders(int lineNumber) throws TruthTableFormatException {
            if (inputLabels == null) {
                throw new TruthTableFormatException(lineNumber, "missing 'inputs:' line");
            }
            if (outputLabels == null) {
                throw new TruthTableFormatException(lineNumber, "missing 'outputs:' line");
            }
        }

        private List<String> labelsAfterColon(String line, int lineNumber, String problemIfNone)
                throws TruthTableFormatException {
            List<String> labels = tokens(line.substring(line.indexOf(':') + 1));
            for (String label : labels) {
                if (!LABEL.matcher(label).matches()) {
                    throw new TruthTableFormatException(lineNumber, "invalid label \"" + label + "\"");
                }
                if (!labelsSeen.add(label.toLowerCase(Locale.ROOT))) {
                    throw new TruthTableFormatException(lineNumber, "duplicate label \"" + label + "\"");
                }
            }
            if (labels.isEmpty()) {
                throw new TruthTableFormatException(lineNumber, problemIfNone);
            }
            return labels;
        }

        private void readRow(String line, int lineNumber) throws TruthTableFormatException {
            int bar = line.indexOf('|');
            if (bar < 0) {
                throw new TruthTableFormatException(lineNumber, "expected '|' between inputs and outputs");
            }
            List<Logic> inputs = values(line.substring(0, bar), inputLabels.size(), false, lineNumber);
            List<Logic> outputs = values(line.substring(bar + 1), outputLabels.size(), true, lineNumber);
            if (!inputCombinationsSeen.add(inputs)) {
                throw new TruthTableFormatException(lineNumber, "duplicate row for inputs " + symbols(inputs));
            }
            rows.add(new TruthTable.Row(inputs, outputs));
        }

        /** @param outputs whether these are output values, which may also be X (don't care) */
        private static List<Logic> values(String part, int expectedCount, boolean outputs, int lineNumber)
                throws TruthTableFormatException {
            String kind = outputs ? "output" : "input";
            List<String> tokens = tokens(part);
            if (tokens.size() != expectedCount) {
                throw new TruthTableFormatException(lineNumber,
                        "expected " + expectedCount + " " + kind + " values, found " + tokens.size());
            }
            List<Logic> values = new ArrayList<>();
            for (String token : tokens) {
                switch (token) {
                    case "0" -> values.add(Logic.ZERO);
                    case "1" -> values.add(Logic.ONE);
                    case "X", "x" -> {
                        if (!outputs) {
                            throw invalidValue(kind, token, lineNumber);
                        }
                        values.add(Logic.UNKNOWN);
                    }
                    default -> throw invalidValue(kind, token, lineNumber);
                }
            }
            return values;
        }

        private static TruthTableFormatException invalidValue(String kind, String token, int lineNumber) {
            return new TruthTableFormatException(lineNumber, "invalid " + kind + " value \"" + token + "\"");
        }

        private static List<String> tokens(String part) {
            List<String> tokens = new ArrayList<>();
            for (String token : part.trim().split("[\\s,]+")) {
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }
            return tokens;
        }
    }
}
