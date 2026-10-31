package use_case;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TutorInteractorTest {

    @Test
    void testExecuteSuccess() {
        // 1. Arrange: Chuẩn bị dữ liệu đầu vào (tin nhắn hợp lệ)
        TutorInputData inputData = new TutorInputData("Mạch của mình bị sai ở đâu thế?");

        // Tạo một OutputBoundary giả lập (Anonymous class) để hứng kết quả trả về
        TutorOutputBoundary successPresenter = new TutorOutputBoundary() {
            @Override
            public void prepareSuccessView(String response) {
                // Kiểm tra xem AI có trả về phản hồi không và có đúng định dạng không
                assertNotNull(response);
                assertTrue(response.contains("AI Tutor"));
            }

            @Override
            public void prepareFailView(String error) {
                fail("Không nên gọi prepareFailView ở trường hợp này.");
            }
        };

        TutorInputBoundary interactor = new TutorInteractor(successPresenter);

        // 2. Act: Thực thi use case
        interactor.execute(inputData);
    }

    @Test
    void testExecuteEmptyMessageFail() {
        // 1. Arrange: Chuẩn bị dữ liệu đầu vào (tin nhắn rỗng)
        TutorInputData inputData = new TutorInputData("");

        TutorOutputBoundary failPresenter = new TutorOutputBoundary() {
            @Override
            public void prepareSuccessView(String response) {
                fail("Không nên gọi prepareSuccessView khi tin nhắn rỗng.");
            }

            @Override
            public void prepareFailView(String error) {
                // Kiểm tra xem thông báo lỗi có chuẩn xác không
                assertEquals("Tin nhắn không được để trống!", error);
            }
        };

        TutorInputBoundary interactor = new TutorInteractor(failPresenter);

        // 2. Act: Thực thi use case
        interactor.execute(inputData);
    }
}
