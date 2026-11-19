package data_access.llm;

import entity.settings.AiProvider;
import use_case.ai_settings.AiConnectionException;
import use_case.ai_settings.AiConnectionTester;
import use_case.ai_settings.ConnectionProblem;

import java.util.List;
import java.util.Objects;

/** Tests a key by asking that one provider for a one-word reply. */
public final class LlmConnectionTester implements AiConnectionTester {

    static final LlmRequest TEST_REQUEST = new LlmRequest("You are checking that a connection works.",
            List.of(LlmMessage.user("Reply with the single word OK.")), 0.0, 16, false);

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
