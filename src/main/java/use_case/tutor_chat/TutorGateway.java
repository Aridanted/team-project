package use_case.tutor_chat;

/** Produces the tutor's next reply. The AI tutor implements it; tests use fakes. */
public interface TutorGateway {

    /**
     * @return the reply text, never blank
     * @throws TutorUnavailableException if no reply could be obtained
     */
    String reply(TutorRequest request) throws TutorUnavailableException;

    /**
     * Like {@link #reply} but also says whether the reply came from the offline tutor. Gateways that
     * never fall back keep this default, which marks every reply as online.
     *
     * @param request the chat and circuit context
     * @return the reply and its source
     * @throws TutorUnavailableException if no reply could be obtained
     */
    default TutorReply answer(TutorRequest request) throws TutorUnavailableException {
        return TutorReply.online(reply(request));
    }
}
