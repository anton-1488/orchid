package com.subgraph.orchid.circuits.cells.io;

import java.nio.ByteBuffer;

public class CellReaderImpl implements CellReader {
    private final ByteBuffer cellBuffer;

    public CellReaderImpl(ByteBuffer cellBuffer) {
        this.cellBuffer = cellBuffer;
    }

    /**
     * Return the next byte from the cell and increment the internal pointer by one byte.
     *
     * @return The byte at the current pointer location.
     */
    @Override
    public byte getByte() {
        return (byte) (cellBuffer.get() & 0xFF);
    }

    /**
     * Return the byte at the specified offset into the cell.
     *
     * @param index The cell offset.
     * @return The byte at the specified offset.
     */
    @Override
    public byte getByteAt(int index) {
        return (byte) (cellBuffer.get(index) & 0xFF);
    }

    /**
     * Return the next 16-bit big endian value from the cell and increment the internal pointer by two bytes.
     *
     * @return The 16-bit short value at the current pointer location.
     */
    @Override
    public short getShort() {
        return (short) (cellBuffer.getShort() & 0xFFFF);
    }

    /**
     * Return the 16-bit big endian value at the specified offset into the cell.
     *
     * @param index The cell offset.
     * @return The 16-bit short value at the specified offset.
     */
    @Override
    public short getShortAt(int index) {
        return (short) (cellBuffer.getShort(index) & 0xFFFF);
    }

    /**
     * Return the next 32-bit big endian value from the cell and increment the internal pointer by four bytes.
     *
     * @return The 32-bit integer value at the current pointer location.
     */
    @Override
    public int getInt() {
        return cellBuffer.getInt();
    }

    /**
     * Copy <code>buffer.length</code> bytes from the cell into <code>buffer</code>.  The data is copied starting
     * from the current internal pointer location and afterward the internal pointer is incremented by <code>buffer.length</code>
     * bytes.
     *
     * @param buffer The array of bytes to copy the cell data into.
     */
    @Override
    public void getByteArray(byte[] buffer) {
        cellBuffer.get(buffer);
    }

    @Override
    public void skip(int len) {
        cellBuffer.position(cellBuffer.position() + len);
    }

    @Override
    public int getPosition() {
        return cellBuffer.position();
    }

    @Override
    public int remaining() {
        return cellBuffer.remaining();
    }

    @Override
    public void reset() {
        cellBuffer.reset();
    }
}