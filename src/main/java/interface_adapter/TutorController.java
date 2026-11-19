package interface_adapter;

import use_case.TutorInputBoundary;
import use_case.TutorInputData;

public class TutorController {
    private final TutorInputBoundary interactor;

    public TutorController(TutorInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void sendMessage(String message) {
        sendMessage(message, null, 0);
    }

    public void sendMessage(String message, String lastCheckResult) {
        sendMessage(message, lastCheckResult, 0);
    }

    // --- BỔ SUNG CHO ISSUE #7: Hàm gọi khi sinh viên bấm nút Hint 1, 2 hoặc 3 ---
    public void sendMessage(String message, String lastCheckResult, int hintLevel) {
        TutorInputData inputData = new TutorInputData(message, lastCheckResult, hintLevel);
        interactor.execute(inputData);
    }
}