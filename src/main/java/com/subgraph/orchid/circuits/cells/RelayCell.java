package com.subgraph.orchid.circuits.cells;

import com.subgraph.orchid.CircuitNode;
import com.subgraph.orchid.circuits.cells.enums.RelayCellCommand;

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

    void setLength();

    void setDigest(byte[] digest);
}
