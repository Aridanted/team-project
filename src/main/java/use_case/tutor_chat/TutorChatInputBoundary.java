package use_case.tutor_chat;

/** What the tutor chat screen can ask for. */
public interface TutorChatInputBoundary {

    /** Sends the student's message to the tutor and presents the tutor's reply. */
    void send(TutorChatInputData inputData);

    /** Forgets the conversation. */
    void clear();
}
