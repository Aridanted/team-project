import interface_adapter.TutorController;
import interface_adapter.TutorPresenter;
import interface_adapter.TutorViewModel;
import use_case.FallbackTutorGateway;
import use_case.RuleBasedTutor;
import use_case.TutorGateway;
import use_case.TutorInteractor;
import view.TutorChatPanel;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TutorViewModel viewModel = new TutorViewModel();
            TutorPresenter presenter = new TutorPresenter(viewModel);

            TutorGateway primaryAI = null;
            RuleBasedTutor offlineTutor = new RuleBasedTutor();
            TutorGateway fallbackGateway = new FallbackTutorGateway(primaryAI, offlineTutor);

            TutorInteractor interactor = new TutorInteractor(presenter, fallbackGateway);
            TutorController controller = new TutorController(interactor);

            JFrame frame = new JFrame("LogiCoach - AI Tutor Chat");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new TutorChatPanel(controller, viewModel));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}