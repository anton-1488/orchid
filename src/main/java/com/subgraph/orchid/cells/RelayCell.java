package com.subgraph.orchid.cells;

import com.subgraph.orchid.circuits.CircuitNode;
import com.subgraph.orchid.cells.enums.CellCommand;
import com.subgraph.orchid.cells.enums.RelayCellCommand;

import java.nio.ByteBuffer;

public interface RelayCell extends Cell {
    int LENGTH_OFFSET = 12;
    int RECOGNIZED_OFFSET = 4;
    int DIGEST_OFFSET = 8;
    int HEADER_SIZE = 14;

    int getStreamId();

    RelayCellCommand getRelayCommand();

    /**
     * Return the circuit node this cell was received from for outgoing cells or the destination circuit node
     * for outgoing cells.
     */
    CircuitNode getCircuitNode();

    ByteBuffer getPayloadBuffer();

    /**
     * Check if this is a relay early cell
     */
    default boolean isRelayEarly() {
        return getCommand() == CellCommand.RELAY_EARLY;
    }

    /**
     * Check if this cell is outgoing (being sent) or incoming (received)
     */
    boolean isOutgoing();

    void setLength();

    void setDigest(byte[] digest);
}