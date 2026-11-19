package use_case;

public class TutorPromptBuilder {

    public static String buildPrompt(String studentMessage, int hintLevel) {
        switch (hintLevel) {
            case 1:
                return "Give a gentle nudge (Hint 1) for the following problem without giving away the answer: " + studentMessage;
            case 2:
                return "Point out the likely faulty part or gate in the circuit (Hint 2) for: " + studentMessage;
            case 3:
                return "Explain the theoretical concept behind this logic circuit (Hint 3) for: " + studentMessage;
            default:
                return studentMessage;

        }
    }
}