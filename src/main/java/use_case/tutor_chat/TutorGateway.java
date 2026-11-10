package use_case.tutor_chat;

/** Produces the tutor's next reply. The AI tutor implements it; tests use fakes. */
public interface TutorGateway {

    /**
     * @return the reply text, never blank
     * @throws TutorUnavailableException if no reply could be obtained
     */
    String reply(TutorRequest request) throws TutorUnavailableException;
}
