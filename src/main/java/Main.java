import interface_adapter.TutorController;
import interface_adapter.TutorPresenter;
import interface_adapter.TutorViewModel;
import use_case.TutorInteractor;
import view.TutorChatPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Opens a window with only the tutor chat. The full app starts from {@code app.Main}. */
public class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TutorViewModel viewModel = new TutorViewModel();
            TutorPresenter presenter = new TutorPresenter(viewModel);
            TutorInteractor interactor = new TutorInteractor(presenter);
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
