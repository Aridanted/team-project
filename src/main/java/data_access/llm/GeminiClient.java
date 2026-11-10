package data_access.llm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Google Gemini via the {@code generateContent} REST endpoint.
 *
 * <p>HTTP 401/403, and 400 when the error is about the key, mean {@link LlmException.Kind#AUTH};
 * 429 means {@code RATE_LIMIT}; 5xx and transport failures mean {@code NETWORK}; a reply without
 * candidates or stopped by a safety filter means {@code BLOCKED}.
 */
public final class GeminiClient implements LlmClient {

    static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    /**
     * Gemini's thinking models spend output tokens before writing; smaller limits often give
     * empty replies, so requests never ask for fewer than this.
     */
    static final int MIN_OUTPUT_TOKENS = 1024;

    private static final String NAME = "Gemini";
    private static final Pattern MODEL = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Set<String> BLOCKING_FINISH_REASONS =
            Set.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY");

    private final HttpTransport transport;
    private final String apiKey;
    private final String model;

    public GeminiClient(HttpTransport transport, String apiKey, String model) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.model = Objects.requireNonNull(model, "model");
        if (!MODEL.matcher(model).matches()) {
            throw new IllegalArgumentException("invalid Gemini model name: " + model);
        }
    }

    @Override
    public String complete(LlmRequest request) throws LlmException {
        HttpTransport.HttpReply reply;
        try {
            reply = transport.postJson(String.format(ENDPOINT, model), Map.of("x-goog-api-key", apiKey),
                    requestBody(request).toString());
        } catch (IOException e) {
            throw new LlmException(LlmException.Kind.NETWORK, NAME + ": " + ProviderErrors.mask(
                    String.valueOf(e.getMessage()), apiKey), e);
        }
        if (!reply.isSuccess()) {
            throw new LlmException(kindFor(reply), ProviderErrors.describe(NAME, reply.status(), reply.body(), apiKey));
        }
        return replyText(reply.body());
    }

    static JSONObject requestBody(LlmRequest request) {
        JSONObject body = new JSONObject();
        if (!request.systemPrompt().isBlank()) {
            body.put("systemInstruction", new JSONObject().put("parts", textParts(request.systemPrompt())));
        }
        JSONArray contents = new JSONArray();
        for (LlmMessage message : request.messages()) {
            contents.put(new JSONObject()
                    .put("role", message.role() == LlmRole.USER ? "user" : "model")
                    .put("parts", textParts(message.text())));
        }
        body.put("contents", contents);
        JSONObject config = new JSONObject()
                .put("temperature", request.temperature())
                .put("maxOutputTokens", Math.max(MIN_OUTPUT_TOKENS, request.maxOutputTokens()));
        if (request.expectJson()) {
            config.put("responseMimeType", "application/json");
        }
        body.put("generationConfig", config);
        return body;
    }

    private static JSONArray textParts(String text) {
        return new JSONArray().put(new JSONObject().put("text", text));
    }

    private static LlmException.Kind kindFor(HttpTransport.HttpReply reply) {
        int status = reply.status();
        if (status == 401 || status == 403) {
            return LlmException.Kind.AUTH;
        }
        if (status == 400) {
            String body = reply.body().toUpperCase(Locale.ROOT);
            boolean aboutKey = body.contains("API_KEY") || body.contains("API KEY");
            return aboutKey ? LlmException.Kind.AUTH : LlmException.Kind.BAD_RESPONSE;
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
            JSONObject root = new JSONObject(body);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                JSONObject feedback = root.optJSONObject("promptFeedback");
                String reason = feedback == null ? "" : feedback.optString("blockReason", "");
                throw new LlmException(LlmException.Kind.BLOCKED,
                        NAME + ": no reply" + (reason.isEmpty() ? "" : " (blocked: " + reason + ")"));
            }
            JSONObject candidate = candidates.getJSONObject(0);
            String text = joinParts(candidate.optJSONObject("content"));
            String finishReason = candidate.optString("finishReason", "");
            if (text.isBlank()) {
                LlmException.Kind kind = BLOCKING_FINISH_REASONS.contains(finishReason)
                        ? LlmException.Kind.BLOCKED : LlmException.Kind.BAD_RESPONSE;
                throw new LlmException(kind, NAME + ": empty reply"
                        + (finishReason.isEmpty() ? "" : " (finish reason " + finishReason + ")"));
            }
            return text;
        } catch (JSONException e) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE, NAME + ": the reply was not valid JSON", e);
        }
    }

    /** Joins the text of every part, skipping the model's "thought" parts. */
    private static String joinParts(JSONObject content) {
        if (content == null) {
            return "";
        }
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part != null && !part.optBoolean("thought", false)) {
                text.append(part.optString("text", ""));
            }
        }
        return text.toString();
    }
}
