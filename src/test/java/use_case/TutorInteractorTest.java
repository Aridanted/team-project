package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TutorInteractorTest {

    @Test
    void testExecuteSuccess() {
        TutorInputData inputData = new TutorInputData("Where is our route");

        TutorOutputBoundary successPresenter = new TutorOutputBoundary() {
            @Override
            public void prepareSuccessView(String response) {
                assertNotNull(response);
                assertTrue(response.contains("AI Tutor"));
            }

            @Override
            public void prepareFailView(String error) {
                fail("Should not call prepareFailView in this case.");
            }
        };

        TutorInputBoundary interactor = new TutorInteractor(successPresenter);

        interactor.execute(inputData);
    }

    @Test
    void testExecuteEmptyMessageFail() {
        TutorInputData inputData = new TutorInputData("");

        TutorOutputBoundary failPresenter = new TutorOutputBoundary() {
            @Override
            public void prepareSuccessView(String response) {
                fail("Should not call prepareSuccessView when message is empty.");
            }

            @Override
            public void prepareFailView(String error) {
                assertEquals("Message cannot be empty!", error);
            }
        };

        TutorInputBoundary interactor = new TutorInteractor(failPresenter);

        interactor.execute(inputData);
    }
}
