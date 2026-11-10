package entity.settings;

import java.util.Arrays;
import java.util.Optional;

/** An AI service LogiCoach can talk to. */
public enum AiProvider {
    GEMINI("gemini", "Gemini"),
    GROQ("groq", "Groq");

    private final String id;
    private final String displayName;

    AiProvider(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** @return a stable lower-case id, used in the settings file and between layers */
    public String id() {
        return id;
    }

    /** @return the name shown to the user, for example "Gemini" */
    public String displayName() {
        return displayName;
    }

    /** @return the provider with this id, ignoring surrounding spaces and case */
    public static Optional<AiProvider> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.strip();
        return Arrays.stream(values()).filter(provider -> provider.id.equalsIgnoreCase(wanted)).findFirst();
    }
}
