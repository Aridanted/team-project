package interface_adapter;

import use_case.TutorOutputBoundary;

/** Shows the tutor's reply, or an error, in the {@link TutorViewModel}. */
public class TutorPresenter implements TutorOutputBoundary {
    private final TutorViewModel viewModel;

    public TutorPresenter(TutorViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(String response) {
        viewModel.setLastResponse(response);
    }

    @Override
    public void prepareFailView(String error) {
        viewModel.setLastResponse("Error: " + error);
    }
}
