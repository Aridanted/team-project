package use_case;

public class TutorInteractor implements TutorInputBoundary {
    private final TutorOutputBoundary outputBoundary;
    private final TutorGateway tutorGateway;
    private final TutorSafetyFilter safetyFilter;

    public TutorInteractor(TutorOutputBoundary outputBoundary, TutorGateway tutorGateway) {
        this(outputBoundary, tutorGateway, new TutorSafetyFilter());
    }

    public TutorInteractor(
            TutorOutputBoundary outputBoundary,
            TutorGateway tutorGateway,
            TutorSafetyFilter safetyFilter
    ) {
        this.outputBoundary = outputBoundary;
        this.tutorGateway = tutorGateway;
        this.safetyFilter = safetyFilter != null ? safetyFilter : new TutorSafetyFilter();
    }

    @Override
    public void execute(TutorInputData inputData) {
        String studentMsg = inputData.getMessage();

        if (studentMsg == null || studentMsg.trim().isEmpty()) {
            outputBoundary.prepareFailView("Message cannot be empty!");
            return;
        }

        int hintLevel = inputData.getHintLevel();
        String tailoredPrompt = TutorPromptBuilder.buildPrompt(studentMsg, hintLevel);
        String lastCheckResult = inputData.getLastCheckResult();

        TutorResponse response = tutorGateway.getAITutorResponse(tailoredPrompt, lastCheckResult);

        String filteredMessage = safetyFilter.filter(response != null ? response.getMessage() : null);

        boolean isOffline = response != null && response.isOffline();
        String finalResponse = isOffline ? "[Offline] " + filteredMessage : filteredMessage;

        outputBoundary.prepareSuccessView(finalResponse);
    }
}