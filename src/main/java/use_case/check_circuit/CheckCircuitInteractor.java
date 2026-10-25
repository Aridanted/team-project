package use_case.check_circuit;

import entity.circuit.Circuit;
import entity.simulation.Logic;
import entity.simulation.NotSimulatableException;
import entity.truthtable.CheckResult;
import entity.truthtable.LabelMismatchException;
import entity.truthtable.TruthTable;
import entity.truthtable.TruthTableChecker;
import entity.truthtable.TruthTableFormatException;
import entity.truthtable.TruthTableTextFormat;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parses the student's expected truth table, checks the open circuit against it, stores the
 * result for later use, and presents a row-by-row report.
 */
public final class CheckCircuitInteractor implements CheckCircuitInputBoundary {

    private final CheckCircuitDataAccessInterface session;
    private final TruthTableTextFormat textFormat;
    private final TruthTableChecker checker;
    private final CheckCircuitOutputBoundary presenter;

    public CheckCircuitInteractor(CheckCircuitDataAccessInterface session, TruthTableTextFormat textFormat,
                                  TruthTableChecker checker, CheckCircuitOutputBoundary presenter) {
        this.session = Objects.requireNonNull(session, "session");
        this.textFormat = Objects.requireNonNull(textFormat, "textFormat");
        this.checker = Objects.requireNonNull(checker, "checker");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
    }

    @Override
    public void execute(CheckCircuitInputData inputData) {
        Optional<Circuit> circuit = session.getCurrentCircuit();
        if (circuit.isEmpty()) {
            presenter.prepareFailView("Open a circuit before checking it.");
            return;
        }
        String specText = inputData.specText();
        if (specText == null || specText.isBlank()) {
            presenter.prepareFailView("Enter the expected truth table first.");
            return;
        }
        try {
            TruthTable expected = textFormat.parse(specText);
            CheckResult result = checker.check(circuit.get(), expected);
            session.setLastCheck(result);
            presenter.prepareSuccessView(toOutputData(result));
        } catch (TruthTableFormatException | LabelMismatchException | NotSimulatableException e) {
            presenter.prepareFailView(e.getMessage());
        }
    }

    private static CheckCircuitOutputData toOutputData(CheckResult result) {
        List<CheckedRow> rows = result.rows().stream().map(CheckCircuitInteractor::toCheckedRow).toList();
        return new CheckCircuitOutputData(result.expected().inputLabels(), result.expected().outputLabels(),
                rows, result.passedRows(), result.totalRows(), result.failingOutputs());
    }

    private static CheckedRow toCheckedRow(CheckResult.RowResult row) {
        return new CheckedRow(symbols(row.inputs()), symbols(row.expected()), symbols(row.actual()),
                row.failingOutputs());
    }

    private static List<String> symbols(List<Logic> values) {
        return values.stream().map(value -> String.valueOf(value.symbol())).toList();
    }
}
