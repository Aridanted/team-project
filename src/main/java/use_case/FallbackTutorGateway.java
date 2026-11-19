package use_case;

public class FallbackTutorGateway implements TutorGateway {
    private final TutorGateway primaryAI;
    private final RuleBasedTutor offlineTutor;

    public FallbackTutorGateway(TutorGateway primaryAI, RuleBasedTutor offlineTutor) {
        this.primaryAI = primaryAI;
        this.offlineTutor = offlineTutor;
    }

    @Override
    public TutorResponse getAITutorResponse(String message, String lastCheckResult) {
        try {
            if (primaryAI != null) {
                TutorResponse response = primaryAI.getAITutorResponse(message, lastCheckResult);
                if (response != null && !response.getMessage().isEmpty()) {
                    return response;
                }
            }
        } catch (Exception e) {

        }

        String offlineMsg = offlineTutor.generateOfflineResponse(message, lastCheckResult);
        return new TutorResponse(offlineMsg, true);
    }
}