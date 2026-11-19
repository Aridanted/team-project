package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorInteractorTest {

    @Test
    void testExecuteSuccess() {
        TutorInputData inputData = new TutorInputData("Where is our route", null);

        TutorOutputBoundary successPresenter = new TutorOutputBoundary() {
            @Override
            public void prepareSuccessView(String response) {
                assertNotNull(response);
                assertFalse(response.isBlank());
            }

            @Override
            public void prepareFailView(String error) {
                fail("Should not call prepareFailView in this case.");
            }
        };

        TutorInputBoundary interactor = new TutorInteractor(successPresenter, new FallbackTutorGateway(null, new RuleBasedTutor()));

        interactor.execute(inputData);
    }

    @Test
    void testExecuteEmptyMessageFail() {
        TutorInputData inputData = new TutorInputData("", null);

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

        TutorInputBoundary interactor = new TutorInteractor(failPresenter, new FallbackTutorGateway(null, new RuleBasedTutor()));

        interactor.execute(inputData);
    }
}