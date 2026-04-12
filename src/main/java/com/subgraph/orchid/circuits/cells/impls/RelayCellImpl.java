package com.subgraph.orchid.circuits.cells.impls;

import com.subgraph.orchid.circuits.CircuitNode;
import com.subgraph.orchid.circuits.cells.RelayCell;
import com.subgraph.orchid.circuits.cells.enums.CellCommand;
import com.subgraph.orchid.circuits.cells.enums.RelayCellCommand;
import com.subgraph.orchid.exceptions.TorException;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class RelayCellImpl extends CellImpl implements RelayCell {
    private final int streamId;
    private final RelayCellCommand relayCellCommand;
    private final CircuitNode circuitNode;
    private final boolean isOutgoing;

    /*
     * The payload of each unencrypted RELAY cell consists of:
     *     Relay command           [1 byte]
     *     'Recognized'            [2 bytes]
     *     StreamID                [2 bytes]
     *     Digest                  [4 bytes]
     *     Length                  [2 bytes]
     *     Data                    [CELL_LEN-14 bytes]
     */

    public RelayCellImpl(CircuitNode node, int circuit, int stream, RelayCellCommand relayCommand) {
        this(node, circuit, stream, relayCommand, false);
    }

    public RelayCellImpl(CircuitNode node, int circuit, int stream, RelayCellCommand rcc, boolean isRelayEarly) {
        super(circuit, (isRelayEarly) ? (CellCommand.RELAY_EARLY) : (CellCommand.RELAY));
        this.circuitNode = node;
        this.relayCellCommand = rcc;
        this.streamId = stream;
        this.isOutgoing = true;

        writer.putByte((byte) rcc.getCommand());    // Command
        writer.putShort((short) 0);            // 'Recognized'
        writer.putShort((short) stream);        // Stream
        writer.putInt(0);                // Digest
        writer.putShort((short) 0);            // Length
    }

    public RelayCellImpl(CircuitNode node, byte[] rawCell) {
        super(rawCell);
        this.circuitNode = node;
        this.relayCellCommand = RelayCellCommand.ofCommand(reader.getByte());
        reader.getShort();

        this.streamId = reader.getShort();
        this.isOutgoing = false;
        reader.getInt();
        int payloadLength = reader.getShort();

        cellBuffer.mark(); // End of header
        cellBuffer.limit(RelayCell.HEADER_SIZE + payloadLength);
        if (RelayCell.HEADER_SIZE + payloadLength > rawCell.length) {
            throw new TorException("Header length field exceeds total size of cell");
        }
    }

    public int getStreamId() {
        return streamId;
    }

    public RelayCellCommand getRelayCommand() {
        return relayCellCommand;
    }

    public void setLength() {
        writer.putShortAt(LENGTH_OFFSET, (short) (writer.getPosition() - HEADER_SIZE));
    }

    public void setDigest(byte[] digest) {
        getCellWriter().putByteArray(DIGEST_OFFSET, digest);
    }

    public ByteBuffer getPayloadBuffer() {
        return cellBuffer.duplicate().reset().slice();
    }

    public CircuitNode getCircuitNode() {
        return circuitNode;
    }

    @Override
    public boolean isOutgoing() {
        return isOutgoing;
    }

    @Override
    public @NotNull String toString() {
        return "RelayCellImpl{" +
                "streamId=" + streamId +
                ", relayCellCommand=" + relayCellCommand +
                ", circuitNode=" + circuitNode +
                ", isOutgoing=" + isOutgoing +
                '}';
    }
}
