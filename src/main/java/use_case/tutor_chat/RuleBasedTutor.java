package use_case.tutor_chat;

import java.util.Locale;
import java.util.Optional;

import entity.truthtable.CheckResult;

/**
 * Provides rule-based hints from the latest circuit check result without giving full expressions.
 */
public class RuleBasedTutor {

    /**
     * Generates an offline hint message based on the student's message and last check result.
     *
     * @param studentMsg the student's question or prompt
     * @param lastCheck the result of the most recent circuit check, if any
     * @return a rule-based hint string
     */
    public String generateOfflineResponse(String studentMsg, Optional<CheckResult> lastCheck) {
        final String topicNote = buildTopicNote(studentMsg);

        if (lastCheck == null || lastCheck.isEmpty()) {
            return "Offline Tutor: Run a check against the truth table first, "
                    + "then tell me which output row fails." + topicNote;
        }

        final CheckResult check = lastCheck.get();
        if (!check.allPassed()) {
            final int failing = check.totalRows() - check.passedRows();
            return "Offline Tutor (Rule-based): " + failing + " of " + check.totalRows()
                    + " rows failed, on output(s) " + String.join(", ", check.failingOutputs()) + ". "
                    + "Inspect the inputs of the gates driving those outputs."
                    + topicNote;
        }

        return "Offline Tutor (Rule-based): Recent checks passed. "
                + "Ask about a specific gate or subcircuit if you want to explore further."
                + topicNote;
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
