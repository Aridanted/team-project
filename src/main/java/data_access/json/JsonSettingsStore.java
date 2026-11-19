package data_access.json;

import entity.settings.AiProvider;
import entity.settings.AiSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.ai_settings.AiSettingsDataAccessInterface;
import use_case.ai_settings.SettingsStorageException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps AI settings in a JSON file (normally {@code data/settings.json}, which is git-ignored):
 * <pre>
 * {"version": 1, "providerOrder": ["gemini", "groq"], "apiKeys": {"gemini": "..."}}
 * </pre>
 * Writes go to a temporary file in the same folder that is then moved over the old one, so a
 * crash never leaves a half-written file. Where the file system supports it, only the owner may
 * read the file. Unknown providers in the file are ignored.
 */
public final class JsonSettingsStore implements AiSettingsDataAccessInterface {

    static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String ORDER_KEY = "providerOrder";
    private static final String KEYS_KEY = "apiKeys";

    private final Path file;

    public JsonSettingsStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    @Override
    public AiSettings load() throws SettingsStorageException {
        if (!Files.exists(file)) {
            return AiSettings.defaults();
        }
        try {
            return fromJson(new JSONObject(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new SettingsStorageException("Could not read the settings file " + file + ".", e);
        } catch (JSONException | IllegalArgumentException e) {
            throw new SettingsStorageException("The settings file " + file
                    + " is damaged. Enter your keys again and press Save to replace it.", e);
        }
    }

    @Override
    public void save(AiSettings settings) throws SettingsStorageException {
        Objects.requireNonNull(settings, "settings");
        Path folder = file.toAbsolutePath().getParent();
        try {
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, "settings", ".tmp");
            try {
                restrictToOwner(temporary);
                Files.writeString(temporary, toJson(settings).toString(2), StandardCharsets.UTF_8);
                move(temporary);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new SettingsStorageException("Could not save the settings to " + file + ".", e);
        }
    }

    static JSONObject toJson(AiSettings settings) {
        JSONArray order = new JSONArray();
        settings.providerOrder().forEach(provider -> order.put(provider.id()));
        JSONObject keys = new JSONObject();
        settings.apiKeys().forEach((provider, key) -> keys.put(provider.id(), key));
        return new JSONObject().put(VERSION_KEY, VERSION).put(ORDER_KEY, order).put(KEYS_KEY, keys);
    }

    static AiSettings fromJson(JSONObject json) {
        int version = json.getInt(VERSION_KEY);
        if (version != VERSION) {
            throw new IllegalArgumentException("unsupported settings version " + version);
        }
        List<AiProvider> order = new ArrayList<>();
        JSONArray savedOrder = json.optJSONArray(ORDER_KEY);
        if (savedOrder != null) {
            for (int i = 0; i < savedOrder.length(); i++) {
                AiProvider.fromId(savedOrder.optString(i, "")).ifPresent(order::add);
            }
        }
        Map<AiProvider, String> keys = new EnumMap<>(AiProvider.class);
        JSONObject savedKeys = json.optJSONObject(KEYS_KEY);
        if (savedKeys != null) {
            for (String id : savedKeys.keySet()) {
                Optional<AiProvider> provider = AiProvider.fromId(id);
                provider.ifPresent(p -> keys.put(p, savedKeys.optString(id, "")));
            }
        }
        return new AiSettings(order, keys);
    }

    private void move(Path temporary) throws IOException {
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void restrictToOwner(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Windows: the file inherits the user's folder permissions.
        }
    }
}
