package com.subgraph.orchid.cells;


import com.subgraph.orchid.cells.enums.CellCommand;
import com.subgraph.orchid.cells.io.CellReader;
import com.subgraph.orchid.cells.io.CellWriter;

public interface Cell {
    /**
     * The default size of a standard cell.
     */
    int DEFAULT_CELL_LEN = 512;

    /**
     * The default size of a standard cell id.
     */
    int DEFAULT_CELL_ID_LEN = 2;

    /**
     * The length of a standard cell header.
     */
    int CELL_HEADER_LEN = 3;

    /**
     * The header length for a variable length cell (ie: VERSIONS)
     */
    int CELL_VAR_HEADER_LEN = 5;

    /**
     * The length of the payload space in a standard cell.
     */
    int CELL_PAYLOAD_LEN = DEFAULT_CELL_LEN - CELL_HEADER_LEN;

    int getCellLength();

    int getCircIdLength();

    /**
     * Return the circuit id field from this cell.
     *
     * @return The circuit id field of this cell.
     */
    int getCircuitId();

    /**
     * Return the command field from this cell.
     *
     * @return The command field of this cell.
     */
    CellCommand getCommand();

    /**
     * Return the entire cell data as a raw array of bytes.  For all cells except
     * <code>VERSIONS</code>, this array will be exactly <code>CELL_LEN</code> bytes long.
     *
     * @return The cell data as an array of bytes.
     */
    byte[] getCellBytes();

    CellReader getCellReader();

    CellWriter getCellWriter();
}