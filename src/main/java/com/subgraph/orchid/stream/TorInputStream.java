package com.subgraph.orchid.stream;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.impls.RelayCellImpl;
import com.subgraph.orchid.cells.io.CellReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TorInputStream extends InputStream {
    private final static RelayCell CLOSE_SENTINEL = new RelayCellImpl(null, 0, 0, RelayCellCommand.PADDING);
    private final static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private static final Logger log = LoggerFactory.getLogger(TorInputStream.class);

    private final Stream stream;

    /**
     * Queue of RelayCells that have been received on this stream
     */
    private final Queue<RelayCell> incomingCells = new LinkedList<>();

    /**
     * Number of unread data bytes in current buffer and in RELAY_DATA cells on queue
     */
    private final AtomicInteger availableBytes = new AtomicInteger();

    /**
     * Total number of data bytes received in RELAY_DATA cells on this stream
     */
    private final AtomicLong bytesReceived = new AtomicLong();

    /**
     * Bytes of data from the RELAY_DATA cell currently being consumed
     */
    private ByteBuffer currentBuffer;

    /**
     * Set when a RELAY_END cell is received
     */
    private final AtomicBoolean isEOF = new AtomicBoolean();

    /**
     * Set when close() is called on this stream
     */
    private final AtomicBoolean isClosed = new AtomicBoolean();

    public TorInputStream(Stream stream) {
        this.stream = stream;
        this.currentBuffer = EMPTY_BUFFER;
    }

    public long getBytesReceived() {
        return bytesReceived.get();
    }

    @Override
    public synchronized int read() throws IOException {
        checkIsOpened();
        refillBufferIfNeeded();
        if (isEOF.get()) {
            return -1;
        }
        availableBytes.decrementAndGet();
        return currentBuffer.get() & 0xFF;
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte @NotNull [] b, int off, int len) throws IOException {
        checkIsOpened();
        checkReadArguments(b, off, len);

        if (len == 0) {
            return 0;
        }

        refillBufferIfNeeded();
        if (isEOF.get()) {
            return -1;
        }

        int bytesRead = 0;
        int bytesRemaining = len;

        while (bytesRemaining > 0 && !isEOF.get()) {
            refillBufferIfNeeded();
            bytesRead += readFromCurrentBuffer(b, off + bytesRead, len - bytesRead);
            bytesRemaining = len - bytesRead;
            if (availableBytes.get() == 0) {
                return bytesRead;
            }
        }
        return bytesRead;
    }

    private int readFromCurrentBuffer(byte[] b, int off, int len) {
        int readLength = Math.min(currentBuffer.remaining(), len);
        currentBuffer.get(b, off, readLength);
        availableBytes.set(availableBytes.get() - readLength);
        return readLength;
    }

    private void checkReadArguments(byte[] b, int off, int len) {
        if (b == null) {
            throw new IllegalArgumentException("buffer can't be null");
        }

        if ((off < 0) || (off >= b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int available() {
        return availableBytes.get();
    }

    @Override
    public synchronized void close() {
        if (isClosed.get()) {
            return;
        }
        incomingCells.add(CLOSE_SENTINEL);
        try {
            stream.close();
        } catch (Exception e) {
            log.error("Error to close stream: ", e);
        }
        isClosed.set(true);
    }

    public void addEndCell(RelayCell cell) {
        if (isClosed.get()) {
            return;
        }
        incomingCells.add(cell);
    }

    public void addInputCell(RelayCell cell) {
        if (isClosed.get()) {
            return;
        }
        incomingCells.add(cell);
        CellReader reader = cell.getCellReader();
        bytesReceived.addAndGet(reader.remaining());
        availableBytes.addAndGet(reader.remaining());
    }

    /**
     * When this method (or fillBuffer()) returns either isEOF is set or currentBuffer has at least one byte to read
     *
     * @throws IOException if io operations faul
     */
    private void refillBufferIfNeeded() throws IOException {
        if (!isEOF.get()) {
            if (currentBuffer.hasRemaining()) {
                return;
            }
            fillBuffer();
        }
    }

    private void fillBuffer() throws IOException {
        while (true) {
            processIncomingCell(getNextCell());
            if (isEOF.get() || currentBuffer.hasRemaining()) {
                return;
            }
        }
    }

    private void processIncomingCell(RelayCell nextCell) throws IOException {
        if (isClosed.get() || nextCell == CLOSE_SENTINEL) {
            throw new IOException("Input stream closed");
        }

        switch (nextCell.getRelayCommand()) {
            case RelayCellCommand.DATA:
                currentBuffer = nextCell.getPayloadBuffer();
                break;
            case RelayCellCommand.END:
                currentBuffer = EMPTY_BUFFER;
                isEOF.set(true);
                break;
            default:
                throw new IOException("Unexpected RelayCell command type in TorInputStream queue: " + nextCell.getRelayCommand());
        }
    }

    private synchronized RelayCell getNextCell() throws IOException {
        return incomingCells.remove();
    }

    public int unflushedCellCount() {
        return incomingCells.size();
    }

    private void checkIsOpened() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Stream closed");
        }
    }

    @Override
    public String toString() {
        return "TorInputStream stream=" + stream.getStreamId() + " node=" + stream.getTargetNode();
    }
}