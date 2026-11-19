package use_case.ai_settings;

/** Thrown when settings cannot be read or written. The message is shown to the user. */
public class SettingsStorageException extends Exception {

    private static final long serialVersionUID = 1L;

    public SettingsStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
