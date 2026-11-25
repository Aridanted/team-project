package data_access.llm;

import entity.settings.AiProvider;
import use_case.ai_settings.AiConnectionException;
import use_case.ai_settings.AiConnectionTester;
import use_case.ai_settings.ConnectionProblem;

import java.util.List;
import java.util.Objects;

/** Tests a key by asking that one provider for a one-word reply. */
public final class LlmConnectionTester implements AiConnectionTester {

    /**
     * Room for the one-word reply plus any thinking the model does first. Thinking models (such
     * as Claude Haiku 5.5) spend output tokens before replying, so a tiny limit can end the
     * reply before "OK" is written.
     */
    static final int TEST_OUTPUT_TOKENS = 1024;

    static final LlmRequest TEST_REQUEST = new LlmRequest("You are checking that a connection works.",
            List.of(LlmMessage.user("Reply with the single word OK.")), 0.0, TEST_OUTPUT_TOKENS, false);

    private final LlmClientFactory factory;

    public LlmConnectionTester(LlmClientFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public void test(AiProvider provider, String apiKey) throws AiConnectionException {
        try {
            factory.createFor(provider, apiKey).complete(TEST_REQUEST);
        } catch (LlmException e) {
            throw new AiConnectionException(problemFor(e.kind()), e.getMessage());
        }
    }

    static ConnectionProblem problemFor(LlmException.Kind kind) {
        return switch (kind) {
            case NO_KEY -> ConnectionProblem.NO_KEY;
            case NETWORK -> ConnectionProblem.NETWORK;
            case RATE_LIMIT -> ConnectionProblem.RATE_LIMIT;
            case AUTH -> ConnectionProblem.AUTH;
            case BAD_RESPONSE -> ConnectionProblem.BAD_RESPONSE;
            case BLOCKED -> ConnectionProblem.BLOCKED;
        };
    }
}
