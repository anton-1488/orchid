package com.subgraph.orchid.connections;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.circuits.Circuit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class CellProcessor {
    private static final Logger log = LoggerFactory.getLogger(CellProcessor.class);

    private CellProcessor() {
    }

    public static synchronized void processRelayCell(@NotNull Cell cell, @NotNull Map<Integer, Circuit> circuitMap) {
        Circuit circuit = circuitMap.get(cell.getCircuitId());
        if (circuit == null) {
            log.warn("Could not deliver relay cell for circuit id = {} on this connection. Circuit not found.", cell.getCircuitId());
            return;
        }
        circuit.deliverRelayCell(cell);
    }

    public static synchronized void processControlCell(@NotNull Cell cell, @NotNull Map<Integer, Circuit> circuitMap) {
        Circuit circuit = circuitMap.get(cell.getCircuitId());
        if (circuit != null) {
            circuit.deliverControlCell(cell);
        }
    }
}