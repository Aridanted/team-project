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
    void defaultsTryGeminiFirstAndHaveNoKeys() {
        AiSettings settings = AiSettings.defaults();

        assertEquals(List.of(AiProvider.GEMINI, AiProvider.GROQ), settings.providerOrder());
        assertTrue(settings.apiKeys().isEmpty());
    }

    @Test
    void orderIsCompletedAndDeduplicated() {
        AiSettings settings = new AiSettings(List.of(AiProvider.GROQ, AiProvider.GROQ), Map.of());

        assertEquals(List.of(AiProvider.GROQ, AiProvider.GEMINI), settings.providerOrder());
    }

    @Test
    void keysAreStrippedAndBlankKeysDropped() {
        Map<AiProvider, String> keys = new HashMap<>();
        keys.put(AiProvider.GEMINI, "  AIza  ");
        keys.put(AiProvider.GROQ, null);

        AiSettings settings = new AiSettings(List.of(), keys);

        assertEquals("AIza", settings.apiKey(AiProvider.GEMINI).orElseThrow());
        assertTrue(settings.apiKey(AiProvider.GROQ).isEmpty());
    }

    @Test
    void rejectsKeysThatWerePastedWrong() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "AIza\nmore")));
        assertThrows(IllegalArgumentException.class,
                () -> new AiSettings(List.of(), Map.of(AiProvider.GROQ, "k".repeat(201))));
    }

    @Test
    void toStringNeverShowsKeys() {
        String text = new AiSettings(List.of(), Map.of(AiProvider.GEMINI, "secret")).toString();

        assertFalse(text.contains("secret"));
        assertTrue(text.contains("GEMINI"));
    }

    @Test
    void providerIdsAreLenient() {
        assertEquals(AiProvider.GROQ, AiProvider.fromId(" Groq ").orElseThrow());
        assertTrue(AiProvider.fromId("openai").isEmpty());
        assertTrue(AiProvider.fromId(null).isEmpty());
    }
}
