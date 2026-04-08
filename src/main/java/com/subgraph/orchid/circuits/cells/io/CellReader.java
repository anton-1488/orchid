package com.subgraph.orchid.circuits.cells.io;

public interface CellReader {
    /**
     * Return the next byte from the cell and increment the internal pointer by one byte.
     *
     * @return The byte at the current pointer location.
     */
    byte getByte();

    /**
     * Return the byte at the specified offset into the cell.
     *
     * @param index The cell offset.
     * @return The byte at the specified offset.
     */
    byte getByteAt(int index);

    /**
     * Return the next 16-bit big endian value from the cell and increment the internal pointer by two bytes.
     *
     * @return The 16-bit short value at the current pointer location.
     */
    short getShort();

    /**
     * Return the 16-bit big endian value at the specified offset into the cell.
     *
     * @param index The cell offset.
     * @return The 16-bit short value at the specified offset.
     */
    short getShortAt(int index);

    /**
     * Return the next 32-bit big endian value from the cell and increment the internal pointer by four bytes.
     *
     * @return The 32-bit integer value at the current pointer location.
     */
    int getInt();

    /**
     * Copy <code>buffer.length</code> bytes from the cell into <code>buffer</code>.  The data is copied starting
     * from the current internal pointer location and afterward the internal pointer is incremented by <code>buffer.length</code>
     * bytes.
     *
     * @param buffer The array of bytes to copy the cell data into.
     */
    void getByteArray(byte[] buffer);

    void skip(int len);

    int getPosition();

    int remaining();

    void reset();
}