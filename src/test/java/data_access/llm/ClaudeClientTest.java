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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeClientTest {

    private static final String KEY = "sk-ant-secret";
    private static final String OK = "{\"type\":\"message\",\"role\":\"assistant\",\"content\":["
            + "{\"type\":\"text\",\"text\":\"Hi \"},{\"type\":\"text\",\"text\":\"there!\"}],"
            + "\"stop_reason\":\"end_turn\"}";

    private static LlmRequest chat(boolean json) {
        return new LlmRequest("Be kind.", List.of(LlmMessage.user("Hi"), LlmMessage.assistant("Hello!"),
                LlmMessage.user("Why?")), 0.2, 300, json);
    }

    @Test
    void sendsMessagesApiRequest() throws LlmException {
        FakeHttpTransport transport = FakeHttpTransport.answering(200, OK);

        assertEquals("Hi there!", new ClaudeClient(transport, KEY, "claude-haiku-5-5").complete(chat(false)));

        assertEquals("https://api.anthropic.com/v1/messages", transport.url);
        assertEquals(Map.of("x-api-key", KEY, "anthropic-version", "2023-06-01"), transport.headers);
        JSONObject body = new JSONObject(transport.body);
        assertEquals("claude-haiku-5-5", body.getString("model"));
        assertEquals("Be kind.", body.getString("system"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals(List.of("user", "assistant", "user"), List.of(
                messages.getJSONObject(0).getString("role"), messages.getJSONObject(1).getString("role"),
                messages.getJSONObject(2).getString("role")));
        assertEquals("Why?", messages.getJSONObject(2).getString("content"));
        assertEquals(300, body.getInt("max_tokens"));
        assertEquals(0.2, body.getDouble("temperature"));
    }

    @Test
    void jsonRequestsAskForJsonInTheSystemPrompt() {
        JSONObject body = ClaudeClient.requestBody("m", chat(true));

        assertTrue(body.getString("system").startsWith("Be kind."));
        assertTrue(body.getString("system").endsWith(ClaudeClient.JSON_INSTRUCTION));
    }

    @Test
    void emptySystemPromptIsLeftOut() {
        JSONObject body = ClaudeClient.requestBody("m",
                new LlmRequest("", List.of(LlmMessage.user("Hi")), 1, 10, false));

        assertFalse(body.has("system"));
        assertEquals(1, body.getJSONArray("messages").length());
    }

    @Test
    void mapsErrors() {
        assertKind(LlmException.Kind.AUTH, 401, "{\"type\":\"error\",\"error\":{\"message\":\"invalid x-api-key\"}}");
        assertKind(LlmException.Kind.AUTH, 403, "");
        assertKind(LlmException.Kind.RATE_LIMIT, 429, "");
        assertKind(LlmException.Kind.RATE_LIMIT, 529, "{\"type\":\"error\",\"error\":{\"message\":\"Overloaded\"}}");
        assertKind(LlmException.Kind.NETWORK, 500, "");
        assertKind(LlmException.Kind.BAD_RESPONSE, 400, "");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "{\"content\":[]}");
        assertKind(LlmException.Kind.BLOCKED, 200, "{\"content\":[],\"stop_reason\":\"refusal\"}");
        assertKind(LlmException.Kind.BAD_RESPONSE, 200, "nope");
    }

    @Test
    void messagesNeverContainTheKey() {
        String body = "{\"type\":\"error\",\"error\":{\"message\":\"bad key " + KEY + "\"}}";
        LlmException e = assertThrows(LlmException.class, () -> new ClaudeClient(
                FakeHttpTransport.answering(401, body), KEY, "m").complete(chat(false)));

        assertEquals("Claude: HTTP 401: bad key ***", e.getMessage());
    }

    @Test
    void transportFailuresAreNetworkErrors() {
        LlmException e = assertThrows(LlmException.class, () -> new ClaudeClient(
                FakeHttpTransport.failing(new IOException("connection refused")), KEY, "m").complete(chat(false)));

        assertEquals(LlmException.Kind.NETWORK, e.kind());
    }

    private static void assertKind(LlmException.Kind kind, int status, String body) {
        LlmException e = assertThrows(LlmException.class,
                () -> new ClaudeClient(FakeHttpTransport.answering(status, body), KEY, "m").complete(chat(false)));
        assertEquals(kind, e.kind(), e.getMessage());
    }
}
