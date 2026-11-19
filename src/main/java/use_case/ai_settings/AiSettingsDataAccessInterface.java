package use_case.ai_settings;

import entity.settings.AiSettings;

/** Where AI settings are kept between runs. */
public interface AiSettingsDataAccessInterface {

    /**
     * @return the saved settings, or {@link AiSettings#defaults()} if none were saved yet
     * @throws SettingsStorageException if saved settings exist but cannot be read
     */
    AiSettings load() throws SettingsStorageException;

    /** @throws SettingsStorageException if the settings could not be written */
    void save(AiSettings settings) throws SettingsStorageException;
}
