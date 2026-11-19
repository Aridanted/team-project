package interface_adapter.ai_settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import use_case.ai_settings.AiSettingsInputBoundary;
import use_case.ai_settings.AiSettingsOutputData;
import use_case.ai_settings.ConnectionProblem;
import use_case.ai_settings.ConnectionTestOutputData;
import use_case.ai_settings.ProviderSettings;
import use_case.ai_settings.SaveAiSettingsInputData;
import use_case.ai_settings.TestConnectionInputData;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSettingsPresenterTest {

    private AiSettingsViewModel viewModel;
    private AiSettingsPresenter presenter;
    private List<PropertyChangeEvent> events;

    @BeforeEach
    void setUp() {
        viewModel = new AiSettingsViewModel();
        presenter = new AiSettingsPresenter(viewModel);
        events = new ArrayList<>();
        viewModel.addPropertyChangeListener(events::add);
    }

    @Test
    void settingsViewListsProvidersAndSaysWhenSaved() {
        var providers = List.of(
                new ProviderSettings("groq", "Groq", "gsk"),
                new ProviderSettings("gemini", "Gemini", ""));
        presenter.prepareSettingsView(new AiSettingsOutputData(providers, true));

        AiSettingsState state = viewModel.getState();
        assertEquals(List.of(new ProviderRow("groq", "Groq", "gsk"), new ProviderRow("gemini", "Gemini", "")),
                state.getProviders());
        assertEquals("Settings saved.", state.getStatus());
        assertEquals(1, events.size());
    }

    @Test
    void loadingDoesNotClaimToHaveSaved() {
        presenter.prepareSettingsView(new AiSettingsOutputData(List.of(), false));

        assertEquals("", viewModel.getState().getStatus());
    }

    @Test
    void connectionSuccessAndFailureInPlainWords() {
        presenter.prepareConnectionView(ConnectionTestOutputData.connected("Gemini"));
        assertTrue(viewModel.getState().isConnected());
        assertEquals("Connected to Gemini.", viewModel.getState().getConnectionMessage());

        presenter.prepareConnectionView(new ConnectionTestOutputData("Groq", ConnectionProblem.AUTH));
        assertFalse(viewModel.getState().isConnected());
        assertEquals("Groq rejected the API key. Check that you copied all of it and that it is still active.",
                viewModel.getState().getConnectionMessage());
    }

    @Test
    void everyProblemHasAnExplanationNamingTheProvider() {
        for (ConnectionProblem problem : ConnectionProblem.values()) {
            assertTrue(AiSettingsPresenter.explain("Gemini", problem).contains("Gemini"), problem.name());
        }
    }

    @Test
    void failKeepsProvidersAndClearsStatus() {
        presenter.prepareSettingsView(new AiSettingsOutputData(List.of(new ProviderSettings("gemini", "Gemini", "k")),
                true));

        presenter.prepareFailView("Could not save.");

        AiSettingsState state = viewModel.getState();
        assertEquals("Could not save.", state.getError());
        assertEquals("", state.getStatus());
        assertEquals(1, state.getProviders().size());
    }

    @Test
    void controllerPassesInputThrough() {
        List<Object> calls = new ArrayList<>();
        AiSettingsController controller = new AiSettingsController(new AiSettingsInputBoundary() {
            @Override
            public void load() {
                calls.add("load");
            }

            @Override
            public void save(SaveAiSettingsInputData inputData) {
                calls.add(inputData);
            }

            @Override
            public void testConnection(TestConnectionInputData inputData) {
                calls.add(inputData);
            }
        });

        controller.load();
        controller.save(List.of("groq"), Map.of("groq", "k"));
        controller.testConnection("gemini", "x");

        assertEquals(List.of("load", new SaveAiSettingsInputData(List.of("groq"), Map.of("groq", "k")),
                new TestConnectionInputData("gemini", "x")), calls);
    }

    @Test
    void rowsNeverPrintKeys() {
        assertFalse(new ProviderRow("gemini", "Gemini", "secret").toString().contains("secret"));
    }
}
