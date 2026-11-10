package data_access.json;

import entity.settings.AiProvider;
import entity.settings.AiSettings;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.ai_settings.SettingsStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSettingsStoreTest {

    @TempDir
    Path folder;

    private static AiSettings groqFirst() {
        return new AiSettings(List.of(AiProvider.GROQ), Map.of(AiProvider.GEMINI, "AIza-test", AiProvider.GROQ,
                "gsk_test"));
    }

    @Test
    void missingFileGivesDefaults() throws SettingsStorageException {
        assertEquals(AiSettings.defaults(), new JsonSettingsStore(folder.resolve("settings.json")).load());
    }

    @Test
    void savesAndLoadsAgain() throws SettingsStorageException {
        JsonSettingsStore store = new JsonSettingsStore(folder.resolve("data").resolve("settings.json"));

        store.save(groqFirst());

        assertEquals(groqFirst(), new JsonSettingsStore(folder.resolve("data").resolve("settings.json")).load());
    }

    @Test
    void writesVersionedJsonAndLeavesNoTemporaryFiles() throws Exception {
        Path file = folder.resolve("settings.json");
        new JsonSettingsStore(file).save(groqFirst());
        new JsonSettingsStore(file).save(AiSettings.defaults());

        JSONObject json = new JSONObject(Files.readString(file));
        assertEquals(1, json.getInt("version"));
        assertEquals("gemini", json.getJSONArray("providerOrder").getString(0));
        assertTrue(json.getJSONObject("apiKeys").isEmpty());
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void damagedFileIsReportedNotIgnored() throws IOException {
        Path file = folder.resolve("settings.json");
        Files.writeString(file, "{not json");

        SettingsStorageException e = assertThrows(SettingsStorageException.class, () -> new JsonSettingsStore(file).load());

        assertTrue(e.getMessage().contains("is damaged"));
    }

    @Test
    void otherVersionsAreRejected() throws IOException {
        Path file = folder.resolve("settings.json");
        Files.writeString(file, "{\"version\": 2, \"apiKeys\": {}}");

        assertThrows(SettingsStorageException.class, () -> new JsonSettingsStore(file).load());
    }

    @Test
    void unknownProvidersAreIgnored() throws Exception {
        Path file = folder.resolve("settings.json");
        Files.writeString(file, "{\"version\": 1, \"providerOrder\": [\"openai\", \"groq\"],"
                + " \"apiKeys\": {\"openai\": \"x\", \"groq\": \"gsk_1\"}}");

        AiSettings settings = new JsonSettingsStore(file).load();

        assertEquals(List.of(AiProvider.GROQ, AiProvider.GEMINI), settings.providerOrder());
        assertEquals(Map.of(AiProvider.GROQ, "gsk_1"), settings.apiKeys());
    }

    @Test
    void unwritableLocationIsReported() throws IOException {
        Path blocker = folder.resolve("blocker");
        Files.writeString(blocker, "a file, not a folder");

        assertThrows(SettingsStorageException.class,
                () -> new JsonSettingsStore(blocker.resolve("settings.json")).save(groqFirst()));
    }
}
