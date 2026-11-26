package data_access.llm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Anthropic Claude via the Messages API.
 *
 * <p>Requests follow the Claude Haiku 5.5 migration guide: no {@code temperature} (the model
 * is guided by the prompt instead), and enough {@code max_tokens} for adaptive thinking, which
 * is on by default and uses the same token budget as the reply.
 *
 * <p>HTTP 401/403 mean {@link LlmException.Kind#AUTH}; 429 (and Anthropic's 529 "overloaded")
 * mean {@code RATE_LIMIT}; other 5xx and transport failures mean {@code NETWORK}; a
 * {@code refusal} stop or an empty reply means {@code BLOCKED} or {@code BAD_RESPONSE}.
 */
public final class ClaudeClient implements LlmClient {

    static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    static final String API_VERSION = "2023-06-01";

    /** Claude has no JSON mode, so JSON replies are requested in the system prompt instead. */
    static final String JSON_INSTRUCTION = "Reply with a single JSON object and nothing else: no markdown "
            + "fences and no text before or after it.";

    /**
     * Thinking tokens count toward {@code max_tokens}, so a small limit can end the reply while
     * the model is still thinking. Requests never ask for fewer than this.
     */
    static final int MIN_OUTPUT_TOKENS = 2048;

    private static final String NAME = "Claude";

    private final HttpTransport transport;
    private final String apiKey;
    private final String model;

    public ClaudeClient(HttpTransport transport, String apiKey, String model) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public String complete(LlmRequest request) throws LlmException {
        HttpTransport.HttpReply reply;
        try {
            reply = transport.postJson(ENDPOINT,
                    Map.of("x-api-key", apiKey, "anthropic-version", API_VERSION),
                    requestBody(model, request).toString());
        } catch (IOException e) {
            throw new LlmException(LlmException.Kind.NETWORK, NAME + ": " + ProviderErrors.mask(
                    String.valueOf(e.getMessage()), apiKey), e);
        }
        if (!reply.isSuccess()) {
            throw new LlmException(kindFor(reply.status()),
                    ProviderErrors.describe(NAME, reply.status(), reply.body(), apiKey));
        }
        return replyText(reply.body());
    }

    static JSONObject requestBody(String model, LlmRequest request) {
        JSONArray messages = new JSONArray();
        for (LlmMessage message : request.messages()) {
            messages.put(new JSONObject()
                    .put("role", message.role() == LlmRole.USER ? "user" : "assistant")
                    .put("content", message.text()));
        }
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", Math.max(MIN_OUTPUT_TOKENS, request.maxOutputTokens()));
        String system = request.systemPrompt().isBlank() ? "" : request.systemPrompt();
        if (request.expectJson()) {
            system = system.isEmpty() ? JSON_INSTRUCTION : system + "\n\n" + JSON_INSTRUCTION;
        }
        if (!system.isEmpty()) {
            body.put("system", system);
        }
        return body;
    }

    private static LlmException.Kind kindFor(int status) {
        if (status == 401 || status == 403) {
            return LlmException.Kind.AUTH;
        }
        if (status == 429 || status == 529) {
            return LlmException.Kind.RATE_LIMIT;
        }
        if (status >= 500) {
            return LlmException.Kind.NETWORK;
        }
        return LlmException.Kind.BAD_RESPONSE;
    }

    private static String replyText(String body) throws LlmException {
        try {
            JSONObject reply = new JSONObject(body);
            JSONArray content = reply.optJSONArray("content");
            StringBuilder text = new StringBuilder();
            if (content != null) {
                for (int i = 0; i < content.length(); i++) {
                    JSONObject block = content.optJSONObject(i);
                    if (block != null && "text".equals(block.optString("type"))) {
                        text.append(block.optString("text", ""));
                    }
                }
            }
            if (text.toString().isBlank()) {
                String stopReason = reply.optString("stop_reason", "");
                if ("refusal".equals(stopReason)) {
                    throw new LlmException(LlmException.Kind.BLOCKED, NAME + ": the model declined to answer");
                }
                String detail = "max_tokens".equals(stopReason)
                        ? ": the reply ran out of tokens while the model was thinking" : ": empty reply";
                throw new LlmException(LlmException.Kind.BAD_RESPONSE, NAME + detail);
            }
            return text.toString();
        } catch (JSONException e) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE, NAME + ": the reply was not valid JSON", e);
        }
    }
}
