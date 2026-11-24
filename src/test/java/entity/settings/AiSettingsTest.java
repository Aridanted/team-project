package entity.settings;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSettingsTest {

    @Test
    void defaultsTryClaudeFirstAndHaveNoKeys() {
        AiSettings settings = AiSettings.defaults();

        assertEquals(List.of(AiProvider.CLAUDE, AiProvider.GEMINI, AiProvider.CHATGPT), settings.providerOrder());
        assertTrue(settings.apiKeys().isEmpty());
    }

    @Test
    void orderIsCompletedAndDeduplicated() {
        AiSettings settings = new AiSettings(List.of(AiProvider.CHATGPT, AiProvider.CHATGPT), Map.of());

        assertEquals(List.of(AiProvider.CHATGPT, AiProvider.CLAUDE, AiProvider.GEMINI), settings.providerOrder());
    }

    @Test
    void keysAreStrippedAndBlankKeysDropped() {
        Map<AiProvider, String> keys = new HashMap<>();
        keys.put(AiProvider.GEMINI, "  AIza  ");
        keys.put(AiProvider.CHATGPT, null);

        AiSettings settings = new AiSettings(List.of(), keys);

        assertEquals("AIza", settings.apiKey(AiProvider.GEMINI).orElseThrow());
        assertTrue(settings.apiKey(AiProvider.CHATGPT).isEmpty());
    }

    @Test
    void rejectsKeysThatWerePastedWrong() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "AIza\nmore")));
        assertThrows(IllegalArgumentException.class,
                () -> new AiSettings(List.of(), Map.of(AiProvider.CHATGPT, "k".repeat(201))));
    }

    @Test
    void toStringNeverShowsKeys() {
        String text = new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "secret")).toString();

        assertFalse(text.contains("secret"));
        assertTrue(text.contains("GEMINI"));
    }

    @Test
    void providerIdsAreLenient() {
        assertEquals(AiProvider.CHATGPT, AiProvider.fromId(" ChatGPT ").orElseThrow());
        assertTrue(AiProvider.fromId("mistral").isEmpty());
        assertTrue(AiProvider.fromId(null).isEmpty());
    }
}
