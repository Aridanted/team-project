package use_case.tutor_chat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import entity.simulation.Logic;
import entity.truthtable.CheckResult;

/**
 * Provides rule-based hints from the latest circuit check result without giving full expressions.
 * Hint 1 is a gentle nudge, Hint 2 points at the failing part, and Hint 3 explains the concept.
 */
public class RuleBasedTutor {

    private static final String PREFIX = "Offline Tutor (Rule-based): ";
    private static final int HINT_NUDGE = 1;
    private static final int HINT_FAULTY_PART = 2;
    private static final int HINT_CONCEPT = 3;

    /**
     * Generates an offline hint message for normal chat (no hint level).
     *
     * @param studentMsg the student's question or prompt
     * @param lastCheck the result of the most recent circuit check, if any
     * @return a rule-based hint string
     */
    public String generateOfflineResponse(String studentMsg, Optional<CheckResult> lastCheck) {
        return generateOfflineResponse(studentMsg, lastCheck, 0);
    }

    /**
     * Generates an offline hint message based on the student's message, last check result and hint level.
     *
     * @param studentMsg the student's question or prompt
     * @param lastCheck the result of the most recent circuit check, if any
     * @param hintLevel 0 for normal chat, 1 for a nudge, 2 for the faulty part, 3 for the concept
     * @return a rule-based hint string; each hint level gives a different message
     */
    public String generateOfflineResponse(String studentMsg, Optional<CheckResult> lastCheck, int hintLevel) {
        final String topicNote = buildTopicNote(studentMsg);
        final Optional<CheckResult> check = lastCheck == null ? Optional.empty() : lastCheck;

        return switch (hintLevel) {
            case HINT_NUDGE -> PREFIX + "Hint 1: " + nudge(check) + topicNote;
            case HINT_FAULTY_PART -> PREFIX + "Hint 2: " + faultyPart(check) + topicNote;
            case HINT_CONCEPT -> PREFIX + "Hint 3: " + concept(studentMsg, check);
            default -> normalChat(check, topicNote);
        };
    }

    private String normalChat(Optional<CheckResult> lastCheck, String topicNote) {
        if (lastCheck.isEmpty()) {
            return "Offline Tutor: Run a check against the truth table first, "
                    + "then tell me which output row fails." + topicNote;
        }

        final CheckResult check = lastCheck.get();
        if (!check.allPassed()) {
            final int failing = check.totalRows() - check.passedRows();
            return PREFIX + failing + " of " + check.totalRows()
                    + " rows failed, on output(s) " + String.join(", ", check.failingOutputs()) + ". "
                    + "Inspect the inputs of the gates driving those outputs."
                    + topicNote;
        }

        return PREFIX + "Recent checks passed. "
                + "Ask about a specific gate or subcircuit if you want to explore further."
                + topicNote;
    }

    /** Hint 1: only says where to look first. */
    private String nudge(Optional<CheckResult> lastCheck) {
        if (lastCheck.isEmpty()) {
            return "Start by running a check against your truth table. "
                    + "Then look for the output column that does not match.";
        }
        if (lastCheck.get().allPassed()) {
            return "Every row passes. Pick one output and explain to yourself why each input row gives that value.";
        }
        return "Some rows fail. Which output column is wrong, and in which rows? Start there.";
    }

    /** Hint 2: names the failing outputs and one failing row to trace. */
    private String faultyPart(Optional<CheckResult> lastCheck) {
        if (lastCheck.isEmpty()) {
            return "I can't point at a row yet. Run the check, then trace the first failing row "
                    + "through your gates one at a time.";
        }
        final CheckResult check = lastCheck.get();
        if (check.allPassed()) {
            return "Nothing fails, so look at your gate choices: could any gate be removed "
                    + "or replaced without changing the table?";
        }
        final CheckResult.RowResult row = check.failingRows().get(0);
        return (check.totalRows() - check.passedRows()) + " of " + check.totalRows()
                + " rows fail, on output(s) " + String.join(", ", check.failingOutputs()) + ". "
                + "Trace row " + (row.index() + 1) + " (" + describeInputs(check, row.inputs()) + ") "
                + "through the gates that drive " + String.join(", ", row.failingOutputs()) + ", "
                + "and compare each gate's output with what you expect.";
    }

    /** Hint 3: explains the idea behind the circuit, never the answer. */
    private String concept(String studentMsg, Optional<CheckResult> lastCheck) {
        final String topic = buildTopicNote(studentMsg).strip();
        final String idea = topic.isEmpty()
                ? "Every output is a function of the inputs: for each row of the table, decide which inputs "
                        + "make the output 1, then ask which gates can detect exactly that situation."
                : topic;
        final String next = lastCheck.isPresent() && !lastCheck.get().allPassed()
                ? " Then re-run the check and see which rows still fail."
                : " Then run the check to confirm it.";
        return idea + next;
    }

    private String describeInputs(CheckResult check, List<Logic> inputs) {
        final List<String> labels = check.expected().inputLabels();
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            final String label = i < labels.size() ? labels.get(i) : "input " + (i + 1);
            text.append(label).append(" is ").append(inputs.get(i).symbol());
        }
        return text.toString();
    }

    private String buildTopicNote(String studentMsg) {
        if (studentMsg == null || studentMsg.isBlank()) {
            return "";
        }
        final String lower = studentMsg.toLowerCase(Locale.ROOT);
        if (lower.contains("xor") || lower.contains("xnor")) {
            return " Trace when inputs differ versus match before checking downstream wires.";
        }
        if (lower.contains("mux") || lower.contains("select")) {
            return " Verify which data line is selected for each combination of selector bits.";
        }
        if (lower.contains("adder") || lower.contains("carry") || lower.contains("sum")) {
            return " Check a single bit position first and trace how carry propagates.";
        }
        return "";
    }
}
