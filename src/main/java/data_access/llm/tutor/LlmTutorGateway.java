package data_access.llm.tutor;

import data_access.llm.LlmClient;
import data_access.llm.LlmException;
import data_access.llm.LlmMessage;
import data_access.llm.LlmRequest;
import entity.tutor.ChatMessage;
import entity.tutor.ChatRole;
import use_case.tutor_chat.TutorGateway;
import use_case.tutor_chat.TutorRequest;
import use_case.tutor_chat.TutorUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The AI tutor: sends the chat, with the circuit and its failing rows as context, to an {@link LlmClient}. */
public final class LlmTutorGateway implements TutorGateway {

    static final double TEMPERATURE = 0.4;
    static final int MAX_OUTPUT_TOKENS = 600;

    private final LlmClient llm;
    private final TutorPromptBuilder prompts;

    public LlmTutorGateway(LlmClient llm, TutorPromptBuilder prompts) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
    }

    @Override
    public String reply(TutorRequest request) throws TutorUnavailableException {
        LlmRequest llmRequest = new LlmRequest(prompts.systemPrompt(request.circuit(), request.lastCheck()),
                toLlmMessages(request.conversation().messages()), TEMPERATURE, MAX_OUTPUT_TOKENS, false);
        try {
            return llm.complete(llmRequest);
        } catch (LlmException e) {
            throw new TutorUnavailableException(e.getMessage(), e);
        }
    }

    /** Providers expect the student to speak first, so tutor messages before the first question are dropped. */
    static List<LlmMessage> toLlmMessages(List<ChatMessage> messages) {
        List<LlmMessage> result = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message.role() == ChatRole.STUDENT) {
                result.add(LlmMessage.user(message.text()));
            } else if (!result.isEmpty()) {
                result.add(LlmMessage.assistant(message.text()));
            }
        }
        return result;
    }
}
