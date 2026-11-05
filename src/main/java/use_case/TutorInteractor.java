package use_case;

public class TutorInteractor implements TutorInputBoundary {
    private final TutorOutputBoundary outputBoundary;

    public TutorInteractor(TutorOutputBoundary outputBoundary) {
        this.outputBoundary = outputBoundary;
    }

    @Override
    public void execute(TutorInputData inputData) {
        String studentMsg = inputData.getMessage();

        if (studentMsg == null || studentMsg.trim().isEmpty()) {
            outputBoundary.prepareFailView("Tin nhắn không được để trống!");
            return;
        }

        // Đóng vai AI Tutor (Socratic method): Nudge without giving direct answer
        String aiResponse = "AI Tutor: Bạn vừa hỏi '" + studentMsg + "'. Hãy thử kiểm tra lại giá trị đầu vào ở bảng chân lý dòng bị sai xem cổng logic đang cho ra tín hiệu gì nhé!";

        outputBoundary.prepareSuccessView(aiResponse);
    }
}
