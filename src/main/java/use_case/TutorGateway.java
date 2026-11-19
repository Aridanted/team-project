package use_case;

public interface TutorGateway {
    TutorResponse getAITutorResponse(String message, String lastCheckResult);
}