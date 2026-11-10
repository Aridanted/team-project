package use_case.tutor_chat;

/** How the tutor chat use case reports back. */
public interface TutorChatOutputBoundary {

    /** The conversation changed (a new reply, or it was cleared). */
    void prepareSuccessView(TutorChatOutputData outputData);

    /** The message was not sent, or the tutor could not answer; the conversation is unchanged. */
    void prepareFailView(String errorMessage);
}
