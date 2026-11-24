package use_case.tutor_chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import entity.simulation.Logic;
import entity.truthtable.CheckResult;
import entity.truthtable.TruthTable;

class RuleBasedTutorTest {

    private final RuleBasedTutor tutor = new RuleBasedTutor();

    @Test
    void asksForACheckWhenThereIsNone() {
        final String reply = tutor.generateOfflineResponse("help", Optional.empty());

        assertTrue(reply.contains("Run a check"));
    }

    @Test
    void namesTheFailingOutputsWhenSomeRowsFail() {
        final CheckResult check = new CheckResult(
                new TruthTable(List.of("A"), List.of("Y"), List.of()),
                List.of(
                        row(0, List.of()),
                        row(1, List.of("Y"))
                )
        );

        final String reply = tutor.generateOfflineResponse("help", Optional.of(check));

        assertTrue(reply.contains("1 of 2 rows failed"));
        assertTrue(reply.contains("Y"));
    }

    @Test
    void saysChecksPassedWhenEveryRowPasses() {
        final CheckResult check = new CheckResult(
                new TruthTable(List.of("A"), List.of("Y"), List.of()),
                List.of(row(0, List.of()))
        );

        final String reply = tutor.generateOfflineResponse("help", Optional.of(check));

        assertTrue(reply.contains("passed"));
    }

    @Test
    void addsATopicNoteForXorQuestions() {
        final String reply = tutor.generateOfflineResponse("How does XOR work?", Optional.empty());

        assertTrue(reply.contains("inputs differ"));
    }

    @Test
    void eachHintLevelGivesADifferentMessage() {
        final Optional<CheckResult> check = Optional.of(failingCheck());

        final String normal = tutor.generateOfflineResponse("help", check, 0);
        final String hint1 = tutor.generateOfflineResponse("help", check, 1);
        final String hint2 = tutor.generateOfflineResponse("help", check, 2);
        final String hint3 = tutor.generateOfflineResponse("help", check, 3);

        assertEquals(4, java.util.Set.of(normal, hint1, hint2, hint3).size());
        assertTrue(hint1.contains("Hint 1"));
        assertTrue(hint2.contains("Hint 2"));
        assertTrue(hint3.contains("Hint 3"));
    }

    @Test
    void hintTwoPointsAtTheFirstFailingRowAndItsInputs() {
        final String hint = tutor.generateOfflineResponse("help", Optional.of(failingCheck()), 2);

        assertTrue(hint.contains("row 2"));
        assertTrue(hint.contains("A is 1"));
        assertTrue(hint.contains("Y"));
    }

    @Test
    void hintLevelsAlsoDifferWithoutACheck() {
        final String hint1 = tutor.generateOfflineResponse("help", Optional.empty(), 1);
        final String hint2 = tutor.generateOfflineResponse("help", Optional.empty(), 2);
        final String hint3 = tutor.generateOfflineResponse("help", Optional.empty(), 3);

        assertEquals(3, java.util.Set.of(hint1, hint2, hint3).size());
    }

    @Test
    void hintThreeUsesTheTopicOfTheQuestion() {
        final String hint = tutor.generateOfflineResponse("How does an XOR work?", Optional.empty(), 3);

        assertTrue(hint.contains("inputs differ"));
    }

    @Test
    void twoArgumentOverloadIsNormalChat() {
        assertEquals(tutor.generateOfflineResponse("help", Optional.empty(), 0),
                tutor.generateOfflineResponse("help", Optional.empty()));
    }

    private static CheckResult failingCheck() {
        return new CheckResult(
                new TruthTable(List.of("A"), List.of("Y"), List.of()),
                List.of(
                        row(0, List.of()),
                        new CheckResult.RowResult(1, List.of(Logic.ONE), List.of(Logic.ONE), List.of(Logic.ZERO),
                                List.of("Y"))
                )
        );
    }

    private static CheckResult.RowResult row(int index, List<String> failingOutputs) {
        return new CheckResult.RowResult(index, List.of(Logic.ZERO), List.of(Logic.ONE), List.of(Logic.ONE),
                failingOutputs);
    }
}
