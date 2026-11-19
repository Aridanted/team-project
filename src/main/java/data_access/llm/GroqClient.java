package data_access.llm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Groq via its OpenAI-compatible chat completions endpoint.
 *
 * <p>HTTP 401/403 mean {@link LlmException.Kind#AUTH}; 429 means {@code RATE_LIMIT}; 5xx and
 * transport failures mean {@code NETWORK}; a {@code content_filter} stop means {@code BLOCKED}.
 */
public final class GroqClient implements LlmClient {

    static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private static final String NAME = "Groq";

    private final HttpTransport transport;
    private final String apiKey;
    private final String model;

    public GroqClient(HttpTransport transport, String apiKey, String model) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public String complete(LlmRequest request) throws LlmException {
        HttpTransport.HttpReply reply;
        try {
            reply = transport.postJson(ENDPOINT, Map.of("Authorization", "Bearer " + apiKey),
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
        if (!request.systemPrompt().isBlank()) {
            messages.put(message("system", request.systemPrompt()));
        }
        for (LlmMessage message : request.messages()) {
            messages.put(message(message.role() == LlmRole.USER ? "user" : "assistant", message.text()));
        }
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("temperature", request.temperature())
                .put("max_tokens", request.maxOutputTokens());
        if (request.expectJson()) {
            body.put("response_format", new JSONObject().put("type", "json_object"));
        }
        return body;
    }

    private static JSONObject message(String role, String content) {
        return new JSONObject().put("role", role).put("content", content);
    }

    private static LlmException.Kind kindFor(int status) {
        if (status == 401 || status == 403) {
            return LlmException.Kind.AUTH;
        }
        if (status == 429) {
            return LlmException.Kind.RATE_LIMIT;
        }
        if (status >= 500) {
            return LlmException.Kind.NETWORK;
        }
        return LlmException.Kind.BAD_RESPONSE;
    }

    private static String replyText(String body) throws LlmException {
        try {
            JSONArray choices = new JSONObject(body).optJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LlmException(LlmException.Kind.BAD_RESPONSE, NAME + ": the reply had no choices");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.optJSONObject("message");
            String text = message == null ? "" : message.optString("content", "");
            if (text.isBlank()) {
                boolean filtered = "content_filter".equals(choice.optString("finish_reason", ""));
                throw new LlmException(filtered ? LlmException.Kind.BLOCKED : LlmException.Kind.BAD_RESPONSE,
                        NAME + ": empty reply");
            }
            return text;
        } catch (JSONException e) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE, NAME + ": the reply was not valid JSON", e);
        }
    }
}
