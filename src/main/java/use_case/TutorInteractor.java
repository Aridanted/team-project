package use_case;

/** Answers the student's message with a fixed hint. */
public class TutorInteractor implements TutorInputBoundary {
    private final TutorOutputBoundary outputBoundary;

    public TutorInteractor(TutorOutputBoundary outputBoundary) {
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute(TutorInputData inputData) {
        String studentMsg = inputData.getMessage();

        if (studentMsg == null || studentMsg.trim().isEmpty()) {
            outputBoundary.prepareFailView("Message cannot be empty!");
            return;
        }

        // AI Tutor role (Socratic method): Nudge without giving direct answer
        String aiResponse = "AI Tutor: You asked '" + studentMsg + "'. Try checking the input values at the "
                + "failing row in the truth table to see what output the logic gate produces!";

        outputBoundary.prepareSuccessView(aiResponse);
    }
}
