package interface_adapter.ai_settings;

import java.util.List;

/** What the Settings tab shows. */
public final class AiSettingsState {

    private List<ProviderRow> providers = List.of();
    private String status = "";
    private String connectionMessage = "";
    private boolean connected;
    private String error = "";

    public AiSettingsState() {
    }

    public AiSettingsState(AiSettingsState copy) {
        providers = copy.providers;
        status = copy.status;
        connectionMessage = copy.connectionMessage;
        connected = copy.connected;
        error = copy.error;
    }

    /** @return every provider, in the order they are tried */
    public List<ProviderRow> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderRow> providers) {
        this.providers = List.copyOf(providers);
    }

    /** @return for example "Settings saved.", or "" */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** @return the result of the last connection test in plain words, or "" if none was run */
    public String getConnectionMessage() {
        return connectionMessage;
    }

    public void setConnectionMessage(String connectionMessage) {
        this.connectionMessage = connectionMessage;
    }

    /** @return whether the last connection test succeeded */
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /** @return the error to show, or "" if there is none */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
