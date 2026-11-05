package data_access.llm;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiClientTest {

    private static final String KEY = "AIza-secret-key";
    private static final String OK = "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":"
            + "[{\"text\":\"Hello \"},{\"text\":\"there\"}]},\"finishReason\":\"STOP\"}]}";

    private static LlmRequest chat(boolean json) {
        return new LlmRequest("Be kind.", List.of(LlmMessage.user("Hi"), LlmMessage.assistant("Hello!"),
                LlmMessage.user("Why?")), 0.3, 200, json);
    }

    @Test
    void sendsTheDocumentedRequest() throws LlmException {
        FakeHttpTransport transport = FakeHttpTransport.answering(200, OK);

        new GeminiClient(transport, KEY, "gemini-2.5-flash-lite").complete(chat(true));

        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent",
                transport.url);
        assertEquals(Map.of("x-goog-api-key", KEY), transport.headers);
        JSONObject body = new JSONObject(transport.body);
        assertEquals("Be kind.", body.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0)
                .getString("text"));
        assertEquals(3, body.getJSONArray("contents").length());
        assertEquals("user", body.getJSONArray("contents").getJSONObject(0).getString("role"));
        assertEquals("model", body.getJSONArray("contents").getJSONObject(1).getString("role"));
        assertEquals("Why?", body.getJSONArray("contents").getJSONObject(2).getJSONArray("parts").getJSONObject(0)
                .getString("text"));
        JSONObject config = body.getJSONObject("generationConfig");
        assertEquals(0.3, config.getDouble("temperature"));
        assertEquals(GeminiClient.MIN_OUTPUT_TOKENS, config.getInt("maxOutputTokens"));
        assertEquals("application/json", config.getString("responseMimeType"));
        assertFalse(transport.body.contains(KEY));
    }

    @Test
    void plainTextRequestsHaveNoMimeTypeAndKeepLargerLimits() {
        LlmRequest request = new LlmRequest("", List.of(LlmMessage.user("Hi")), 0.7, 4000, false);

        JSONObject body = GeminiClient.requestBody(request);

        assertFalse(body.has("systemInstruction"));
        assertFalse(body.getJSONObject("generationConfig").has("responseMimeType"));
        assertEquals(4000, body.getJSONObject("generationConfig").getInt("maxOutputTokens"));
    }

    @Test
    void joinsTextPartsAndSkipsThoughts() throws LlmException {
        String reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"thinking...\",\"thought\":true},"
                + "{\"text\":\"Answer\"}]}}]}";

        assertEquals("Answer", client(200, reply).complete(chat(false)));
        assertEquals("Hello there", client(200, OK).complete(chat(false)));
    }

    @Test
    void mapsHttpErrors() {
        assertKind(LlmException.Kind.AUTH, 400,
                "{\"error\":{\"code\":400,\"message\":\"API key not valid. Please pass a valid API key.\","
                        + "\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 400, "{\"error\":{\"message\":\"Invalid JSON payload\"}}");
        assertKind(LlmException.Kind.AUTH, 403, "{\"error\":{\"message\":\"Permission denied\"}}");
        assertKind(LlmException.Kind.RATE_LIMIT, 429, "{\"error\":{\"message\":\"Resource exhausted\"}}");
        assertKind(LlmException.Kind.NETWORK, 503, "Service Unavailable");
        assertKind(LlmException.Kind.BAD_RESPONSE, 404, "{\"error\":{\"message\":\"model not found\"}}");
    }

    @Test
    void errorMessagesNameProviderAndStatusButNeverTheKey() {
        LlmException e = assertThrows(LlmException.class, () -> client(429,
                "{\"error\":{\"message\":\"quota for key " + KEY + " exceeded\"}}").complete(chat(false)));

        assertEquals("Gemini: HTTP 429: quota for key *** exceeded", e.getMessage());
    }

    @Test
    void transportFailuresAreNetworkErrors() {
        GeminiClient client = new GeminiClient(FakeHttpTransport.failing(new HttpTimeoutException("timed out")), KEY,
                "m");

        LlmException e = assertThrows(LlmException.class, () -> client.complete(chat(false)));

        assertEquals(LlmException.Kind.NETWORK, e.kind());
        assertEquals("Gemini: timed out", e.getMessage());
    }

    @Test
    void missingCandidatesMeanBlocked() {
        assertKind(LlmException.Kind.BLOCKED, 200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        assertKind(LlmException.Kind.BLOCKED, 200, "{\"candidates\":[]}");
        assertKind(LlmException.Kind.BLOCKED, 200, "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");
    }

    @Test
    void emptyOrInvalidRepliesAreBadResponses() {
        assertKind(LlmException.Kind.BAD_RESPONSE, 200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"  \"}]},\"finishReason\":\"MAX_TOKENS\"}]}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "<html>not json</html>");
    }

    @Test
    void rejectsModelNamesThatWouldBreakTheUrl() {
        assertThrows(IllegalArgumentException.class, () -> new GeminiClient(FakeHttpTransport.answering(200, OK), KEY,
                "../other?x=1"));
    }

    private static GeminiClient client(int status, String body) {
        return new GeminiClient(FakeHttpTransport.answering(status, body), KEY, "gemini-test");
    }

    private static void assertKind(LlmException.Kind kind, int status, String body) {
        LlmException e = assertThrows(LlmException.class, () -> client(status, body).complete(chat(false)));
        assertEquals(kind, e.kind(), e.getMessage());
        assertTrue(e.getMessage().startsWith("Gemini"));
    }

    @Test
    void ioExceptionMessageIsMaskedToo() {
        GeminiClient client = new GeminiClient(FakeHttpTransport.failing(new IOException("bad " + KEY)), KEY, "m");

        LlmException e = assertThrows(LlmException.class, () -> client.complete(chat(false)));

        assertFalse(e.getMessage().contains(KEY));
    }
}
