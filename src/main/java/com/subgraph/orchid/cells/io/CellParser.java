package com.subgraph.orchid.cells.io;

import com.subgraph.orchid.cells.impls.CellImpl;
import com.subgraph.orchid.cells.enums.CellCommand;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.subgraph.orchid.cells.Cell.*;
import static com.subgraph.orchid.cells.enums.CellCommand.VERSIONS;

public class CellParser {
    public static CellImpl readFromInputStream(InputStream input) throws IOException {
        final ByteBuffer header = readHeaderFromInputStream(input);

        final int circuitId = header.getShort() & 0xFFFF;
        CellCommand command = CellCommand.ofCommand(header.get() & 0xFF);

        if (command == VERSIONS || command.getCommand() > 127) {
            return readVarCell(circuitId, command, input);
        }

        final CellImpl cell = new CellImpl(circuitId, command);
        readAll(input, cell.getCellBytes(), CELL_HEADER_LEN, CELL_PAYLOAD_LEN);

        return cell;
    }

    private static ByteBuffer readHeaderFromInputStream(InputStream input) throws IOException {
        byte[] cellHeader = new byte[CELL_HEADER_LEN];
        readAll(input, cellHeader);
        return ByteBuffer.wrap(cellHeader);
    }

    private static CellImpl readVarCell(int circuitId, CellCommand command, InputStream input) throws IOException {
        byte[] lengthField = new byte[2];
        readAll(input, lengthField);
        int length = ((lengthField[0] & 0xFF) << 8) | (lengthField[1] & 0xFF);

        CellImpl cell = new CellImpl(circuitId, command, length);
        readAll(input, cell.getCellBytes(), CELL_VAR_HEADER_LEN, length);
        return cell;
    }

    private static void readAll(InputStream input, byte[] buffer) throws IOException {
        readAll(input, buffer, 0, buffer.length);
    }

    private static void readAll(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        input.readNBytes(buffer, offset, length);
    }
}