package data_access.llm;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroqClientTest {

    private static final String KEY = "gsk_secret";
    private static final String OK = "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
            + "\"content\":\"Hi!\"},\"finish_reason\":\"stop\"}]}";

    private static LlmRequest chat(boolean json) {
        return new LlmRequest("Be kind.", List.of(LlmMessage.user("Hi"), LlmMessage.assistant("Hello!"),
                LlmMessage.user("Why?")), 0.2, 300, json);
    }

    @Test
    void sendsOpenAiStyleRequest() throws LlmException {
        FakeHttpTransport transport = FakeHttpTransport.answering(200, OK);

        assertEquals("Hi!", new GroqClient(transport, KEY, "llama-3.1-8b-instant").complete(chat(true)));

        assertEquals("https://api.groq.com/openai/v1/chat/completions", transport.url);
        assertEquals(Map.of("Authorization", "Bearer " + KEY), transport.headers);
        JSONObject body = new JSONObject(transport.body);
        assertEquals("llama-3.1-8b-instant", body.getString("model"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals(List.of("system", "user", "assistant", "user"), List.of(
                messages.getJSONObject(0).getString("role"), messages.getJSONObject(1).getString("role"),
                messages.getJSONObject(2).getString("role"), messages.getJSONObject(3).getString("role")));
        assertEquals("Be kind.", messages.getJSONObject(0).getString("content"));
        assertEquals(300, body.getInt("max_tokens"));
        assertEquals(0.2, body.getDouble("temperature"));
        assertEquals("json_object", body.getJSONObject("response_format").getString("type"));
    }

    @Test
    void plainRequestsHaveNoResponseFormatOrEmptySystemMessage() {
        JSONObject body = GroqClient.requestBody("m", new LlmRequest("", List.of(LlmMessage.user("Hi")), 1, 10, false));

        assertFalse(body.has("response_format"));
        assertEquals(1, body.getJSONArray("messages").length());
    }

    @Test
    void mapsErrors() {
        assertKind(LlmException.Kind.AUTH, 401, "{\"error\":{\"message\":\"Invalid API Key\"}}");
        assertKind(LlmException.Kind.AUTH, 403, "");
        assertKind(LlmException.Kind.RATE_LIMIT, 429, "{\"error\":{\"message\":\"Rate limit reached\"}}");
        assertKind(LlmException.Kind.NETWORK, 502, "");
        assertKind(LlmException.Kind.BAD_RESPONSE, 400, "{\"error\":{\"message\":\"bad request\"}}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "{\"choices\":[]}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "{\"choices\":[{\"message\":{\"content\":null}}]}");
        assertKind(LlmException.Kind.BLOCKED, 200,
                "{\"choices\":[{\"message\":{\"content\":\"\"},\"finish_reason\":\"content_filter\"}]}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "nope");
    }

    @Test
    void messagesNeverContainTheKey() {
        LlmException e = assertThrows(LlmException.class, () -> new GroqClient(
                FakeHttpTransport.answering(401, "{\"error\":{\"message\":\"Invalid API Key " + KEY + "\"}}"), KEY, "m")
                .complete(chat(false)));

        assertEquals("Groq: HTTP 401: Invalid API Key ***", e.getMessage());
    }

    @Test
    void transportFailuresAreNetworkErrors() {
        LlmException e = assertThrows(LlmException.class, () -> new GroqClient(
                FakeHttpTransport.failing(new IOException("connection refused")), KEY, "m").complete(chat(false)));

        assertEquals(LlmException.Kind.NETWORK, e.kind());
    }

    private static void assertKind(LlmException.Kind kind, int status, String body) {
        LlmException e = assertThrows(LlmException.class,
                () -> new GroqClient(FakeHttpTransport.answering(status, body), KEY, "m").complete(chat(false)));
        assertEquals(kind, e.kind(), e.getMessage());
    }
}
