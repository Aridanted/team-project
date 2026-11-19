package use_case;

public class TutorInteractor implements TutorInputBoundary {
    private final TutorOutputBoundary outputBoundary;
    private final TutorGateway tutorGateway;

    public TutorInteractor(TutorOutputBoundary outputBoundary, TutorGateway tutorGateway) {
        this.outputBoundary = outputBoundary;
        this.tutorGateway = tutorGateway;
    }

    @Override
    public void execute(TutorInputData inputData) {
        String studentMsg = inputData.getMessage();

        if (studentMsg == null || studentMsg.trim().isEmpty()) {
            outputBoundary.prepareFailView("Message cannot be empty!");
            return;
        }

        String lastCheckResult = inputData.getLastCheckResult();

        TutorResponse response = tutorGateway.getAITutorResponse(studentMsg, lastCheckResult);

        String finalResponse = response.getMessage();
        if (response.isOffline()) {
            finalResponse = "[Offline] " + finalResponse;
        }

        outputBoundary.prepareSuccessView(finalResponse);
    }
}