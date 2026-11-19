package data_access.llm;

import java.util.List;
import java.util.Objects;

/**
 * Everything an {@link LlmClient} needs for one call. Multi-turn, so the tutor can send the whole
 * chat; the last message must come from the user.
 *
 * @param systemPrompt    instructions that frame the whole conversation; may be empty
 * @param messages        the conversation so far, oldest first
 * @param temperature     randomness, from 0 (deterministic) to 2
 * @param maxOutputTokens upper bound on the reply length; providers may raise it to their minimum
 * @param expectJson      whether the reply must be a JSON object (providers that support it are
 *                        told to answer in JSON mode)
 */
public record LlmRequest(String systemPrompt, List<LlmMessage> messages, double temperature,
                         int maxOutputTokens, boolean expectJson) {

    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_OUTPUT_TOKENS = 1024;
    public static final int MAX_OUTPUT_TOKENS_LIMIT = 8192;

    public LlmRequest {
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        messages = List.copyOf(messages);
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("a request needs at least one message");
        }
        if (messages.get(messages.size() - 1).role() != LlmRole.USER) {
            throw new IllegalArgumentException("the last message must come from the user");
        }
        if (!(temperature >= 0 && temperature <= 2)) {
            throw new IllegalArgumentException("temperature must be between 0 and 2, not " + temperature);
        }
        if (maxOutputTokens < 1 || maxOutputTokens > MAX_OUTPUT_TOKENS_LIMIT) {
            throw new IllegalArgumentException("maxOutputTokens must be between 1 and " + MAX_OUTPUT_TOKENS_LIMIT
                    + ", not " + maxOutputTokens);
        }
    }

    /** @return a one-message request with default temperature and length, expecting plain text */
    public static LlmRequest single(String systemPrompt, String userMessage) {
        return new LlmRequest(systemPrompt, List.of(LlmMessage.user(userMessage)), DEFAULT_TEMPERATURE,
                DEFAULT_MAX_OUTPUT_TOKENS, false);
    }
}
