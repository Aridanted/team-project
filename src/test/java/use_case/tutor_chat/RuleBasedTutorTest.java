package use_case.tutor_chat;

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

    private static CheckResult.RowResult row(int index, List<String> failingOutputs) {
        return new CheckResult.RowResult(index, List.of(Logic.ZERO), List.of(Logic.ONE), List.of(Logic.ONE),
                failingOutputs);
    }
}
