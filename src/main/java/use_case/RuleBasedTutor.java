package use_case;

public class RuleBasedTutor {

    public String generateOfflineResponse(String studentMsg, String lastCheckResult) {
        if (lastCheckResult == null || lastCheckResult.isEmpty()) {
            return "Offline Tutor: It looks like you haven't checked your circuit yet. Try running a check against the truth table first, and let me know which row fails!";
        }

        // Rule-based logic based on the last check result
        if (lastCheckResult.contains("FAIL")) {
            return "Offline Tutor (Rule-based): I noticed some failing rows in your truth table. Check the inputs for the failing gate—are the control signals or logic inputs wired correctly?";
        } else {
            return "Offline Tutor (Rule-based): Your circuit passed the recent checks! If you have any specific questions about the design, feel free to ask.";
        }
    }
}