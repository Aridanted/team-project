package use_case;

import java.util.Locale;

/**
 * Provides rule-based hints from the latest circuit check result without giving full expressions.
 */
public class RuleBasedTutor {

    /**
     * Generates an offline hint message based on the student's message and last check result.
     *
     * @param studentMsg the student's question or prompt
     * @param lastCheckResult the result of the most recent circuit check
     * @return a rule-based hint string
     */
    public String generateOfflineResponse(String studentMsg, String lastCheckResult) {
        final String topicNote = buildTopicNote(studentMsg);

        if (lastCheckResult == null || lastCheckResult.isBlank()) {
            return "Offline Tutor: Run a check against the truth table first, "
                    + "then tell me which output row fails." + topicNote;
        }

        if (lastCheckResult.contains("FAIL")) {
            return "Offline Tutor (Rule-based): Some rows failed in your check. "
                    + "Inspect the inputs and control signals of the gates driving the failing outputs."
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
