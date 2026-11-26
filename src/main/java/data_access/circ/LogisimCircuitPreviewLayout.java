package data_access.circ;

import entity.circuit.Circuit;
import use_case.build_circuit.CircuitPreviewLayout;

import java.util.Objects;

/** Uses the Logisim layout and routing steps to prepare a circuit preview. */
public final class LogisimCircuitPreviewLayout implements CircuitPreviewLayout {
    private final CircuitLayout layout;
    private final CircuitWireRouter router;

    /**
     * Creates the preview layout with its component layout and wire router.
     *
     * @param layout the component layout
     * @param router the wire router
     */
    public LogisimCircuitPreviewLayout(CircuitLayout layout, CircuitWireRouter router) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    public Circuit layout(Circuit circuit) {
        Objects.requireNonNull(circuit, "circuit");
        Circuit positioned = layout.layout(circuit);
        return router.route(positioned);
    }
}
