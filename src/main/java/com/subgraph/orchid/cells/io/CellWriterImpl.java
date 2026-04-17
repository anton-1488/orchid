package com.subgraph.orchid.cells.io;

import java.nio.ByteBuffer;

public class CellWriterImpl implements CellWriter {
    private final ByteBuffer cellBuffer;

    public CellWriterImpl(ByteBuffer cellBuffer) {
        this.cellBuffer = cellBuffer;
    }

    /**
     * Store a byte at the current pointer location and increment the pointer by one byte.
     *
     * @param value The byte value to store.
     */
    @Override
    public void putByte(byte value) {
        cellBuffer.put(value);
    }

    /**
     * Store a byte at the specified offset into the cell.
     *
     * @param index The offset in bytes into the cell.
     * @param value The byte value to store.
     */
    @Override
    public void putByteAt(int index, byte value) {
        cellBuffer.put(index, value);
    }

    /**
     * Store a 16-bit short value in big endian order at the current pointer location and
     * increment the pointer by two bytes.
     *
     * @param value The 16-bit short value to store.
     */
    @Override
    public void putShort(short value) {
        cellBuffer.putShort(value);
    }

    /**
     * Store a 16-bit short value in big endian byte order at the specified offset into the cell
     * and increment the pointer by two bytes.
     *
     * @param index The offset in bytes into the cell.
     * @param value The 16-bit short value to store.
     */
    @Override
    public void putShortAt(int index, short value) {
        cellBuffer.putShort(index, value);
    }

    /**
     * Store a 32-bit integer value in big endian order at the current pointer location and
     * increment the pointer by 4 bytes.
     *
     * @param value The 32-bit integer value to store.
     */
    @Override
    public void putInt(int value) {
        cellBuffer.putInt(value);
    }

    /**
     * Store the entire array <code>data</code> at the current pointer location and increment
     * the pointer by <code>data.length</code> bytes.
     *
     * @param data The array of bytes to store in the cell.
     */
    @Override
    public void putByteArray(byte[] data) {
        cellBuffer.put(data);
    }

    @Override
    public void putByteArray(int index, byte[] data) {
        cellBuffer.put(index, data);
    }

    /**
     * Store <code>length</code> bytes of the byte array <code>data</code> starting from
     * <code>offset</code> into the array at the current pointer location and increment
     * the pointer by <code>length</code> bytes.
     *
     * @param data   The source array of bytes.
     * @param offset The offset into the source array.
     * @param length The number of bytes from the source array to store.
     */
    @Override
    public void putByteArray(byte[] data, int offset, int length) {
        cellBuffer.put(data, offset, length);
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