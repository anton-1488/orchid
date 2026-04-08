package com.subgraph.orchid.circuits.cells.io;

import java.nio.charset.StandardCharsets;

public interface CellWriter {
    /**
     * Store a byte at the current pointer location and increment the pointer by one byte.
     *
     * @param value The byte value to store.
     */
    void putByte(byte value);

    /**
     * Store a byte at the specified offset into the cell.
     *
     * @param index The offset in bytes into the cell.
     * @param value The byte value to store.
     */
    void putByteAt(int index, byte value);

    /**
     * Store a 16-bit short value in big endian order at the current pointer location and
     * increment the pointer by two bytes.
     *
     * @param value The 16-bit short value to store.
     */
    void putShort(short value);

    /**
     * Store a 16-bit short value in big endian byte order at the specified offset into the cell
     * and increment the pointer by two bytes.
     *
     * @param index The offset in bytes into the cell.
     * @param value The 16-bit short value to store.
     */
    void putShortAt(int index, short value);

    /**
     * Store a 32-bit integer value in big endian order at the current pointer location and
     * increment the pointer by 4 bytes.
     *
     * @param value The 32-bit integer value to store.
     */
    void putInt(int value);

    /**
     * Store the entire array <code>data</code> at the current pointer location and increment
     * the pointer by <code>data.length</code> bytes.
     *
     * @param data The array of bytes to store in the cell.
     */
    void putByteArray(byte[] data);

    void putByteArray(int index, byte[] data);

    /**
     * Store <code>length</code> bytes of the byte array <code>data</code> starting from
     * <code>offset</code> into the array at the current pointer location and increment
     * the pointer by <code>length</code> bytes.
     *
     * @param data   The source array of bytes.
     * @param offset The offset into the source array.
     * @param length The number of bytes from the source array to store.
     */
    void putByteArray(byte[] data, int offset, int length);

    default void putString(String string) {
        putByteArray(string.getBytes(StandardCharsets.UTF_8));
    }

    int getPosition();

    int remaining();

    void reset();
}