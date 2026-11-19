import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import interface_adapter.tutor_chat.TutorChatController;
import interface_adapter.tutor_chat.TutorChatViewModel;
import use_case.FallbackTutorGateway;
import use_case.RuleBasedTutor;
import use_case.tutor_chat.TutorChatInteractor;
import use_case.tutor_chat.TutorGateway;
import view.TutorChatView;

/**
 * Application entry point for launching the LogiCoach AI Tutor chat interface.
 */
public final class Main {

    private static final String WINDOW_TITLE = "LogiCoach - AI Tutor Chat";

    private Main() {
    }

    /**
     * Starts the LogiCoach AI Tutor Swing frame.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final TutorChatViewModel viewModel = new TutorChatViewModel();
            final TutorGateway primaryAi = null;
            final RuleBasedTutor offlineTutor = new RuleBasedTutor();
            final TutorGateway fallbackGateway = new FallbackTutorGateway(primaryAi, offlineTutor);

            // Replace null arguments with real session/presenter implementations when running standalone.
            final TutorChatInteractor interactor = new TutorChatInteractor(null, fallbackGateway, null);
            final TutorChatController controller = new TutorChatController(interactor);

            final JFrame frame = new JFrame(WINDOW_TITLE);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new TutorChatView(controller, viewModel));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
