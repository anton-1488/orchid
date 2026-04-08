package com.subgraph.orchid.circuits.cells.impls;

import com.subgraph.orchid.circuits.cells.Cell;
import com.subgraph.orchid.circuits.cells.enums.CellCommand;
import com.subgraph.orchid.circuits.cells.io.CellReader;
import com.subgraph.orchid.circuits.cells.io.CellReaderImpl;
import com.subgraph.orchid.circuits.cells.io.CellWriter;
import com.subgraph.orchid.circuits.cells.io.CellWriterImpl;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class CellImpl implements Cell {
    private final int circuitId;
    private final CellCommand command;

    protected final ByteBuffer cellBuffer;
    protected final CellReader reader;
    protected final CellWriter writer;

    /**
     * Variable length cell constructor (ie: VERSIONS cells only)
     */
    public CellImpl(int circuitId, CellCommand command, int payloadLength) {
        this.circuitId = circuitId;
        this.command = command;
        this.cellBuffer = ByteBuffer.wrap(new byte[CELL_VAR_HEADER_LEN + payloadLength]);
        cellBuffer.putShort((short) circuitId);
        cellBuffer.put((byte) command.getCommand());
        cellBuffer.putShort((short) payloadLength);
        cellBuffer.mark();

        reader = new CellReaderImpl(cellBuffer);
        writer = new CellWriterImpl(cellBuffer);
    }

    /**
     * Fixed length cell constructor
     */
    public CellImpl(int circuitId, CellCommand command) {
        this.circuitId = circuitId;
        this.command = command;
        this.cellBuffer = ByteBuffer.wrap(new byte[DEFAULT_CELL_LEN]);
        cellBuffer.putShort((short) circuitId);
        cellBuffer.put((byte) command.getCommand());
        cellBuffer.mark();

        reader = new CellReaderImpl(cellBuffer);
        writer = new CellWriterImpl(cellBuffer);
    }

    public CellImpl(byte[] rawCell) {
        this.cellBuffer = ByteBuffer.wrap(rawCell);
        this.circuitId = cellBuffer.getShort() & 0xFFFF;
        this.command = CellCommand.ofCommand(cellBuffer.get() & 0xFF);
        cellBuffer.mark();

        reader = new CellReaderImpl(cellBuffer);
        writer = new CellWriterImpl(cellBuffer);
    }

    @Override
    public int getCellLength() {
        return DEFAULT_CELL_LEN;
    }

    @Override
    public int getCircIdLength() {
        return DEFAULT_CELL_ID_LEN;
    }

    @Override
    public int getCircuitId() {
        return circuitId;
    }

    @Override
    public CellCommand getCommand() {
        return command;
    }

    @Override
    public byte[] getCellBytes() {
        return cellBuffer.array();
    }

    @Override
    public CellReader getCellReader() {
        return reader;
    }

    @Override
    public CellWriter getCellWriter() {
        return writer;
    }

    @Override
    @NotNull
    public String toString() {
        return String.format("Cell: circuitId=%s; command=%s; payload length=%s", circuitId, command.getCommand(), cellBuffer.capacity());
    }
}