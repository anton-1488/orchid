package com.subgraph.orchid.stream;

import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.impls.RelayCellImpl;
import com.subgraph.orchid.cells.io.CellWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class TorOutputStream extends OutputStream {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final StreamImpl stream;
    private RelayCell currentOutputCell;
    private long bytesSent;
    private CellWriter writer;

    public TorOutputStream(@NotNull StreamImpl stream) {
        this.stream = stream;
        this.bytesSent = 0;
        currentOutputCell = new RelayCellImpl(stream.getTargetNode(), stream.getCircuit().getCircuitId(), stream.getStreamId(), RelayCellCommand.DATA);
        writer = currentOutputCell.getCellWriter();
    }

    private synchronized void flushCurrentOutputCell() throws IOException {
        if (currentOutputCell != null && writer.getPosition() > RelayCell.HEADER_SIZE) {
            try {
                stream.getPackageWindowSemaphore().acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrupted while waiting for send window");
            }

            stream.getCircuit().sendRelayCell(currentOutputCell);
            bytesSent += (writer.getPosition() - RelayCell.HEADER_SIZE);
        }

        currentOutputCell = new RelayCellImpl(stream.getTargetNode(), stream.getCircuit().getCircuitId(), stream.getStreamId(), RelayCellCommand.DATA);
        writer = currentOutputCell.getCellWriter();
    }

    public long getBytesSent() {
        return bytesSent;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        checkOpen();
        if (currentOutputCell == null || writer.remaining() == 0) {
            flushCurrentOutputCell();
        }
        writer.putByte((byte) b);
    }

    @Override
    public synchronized void write(byte @NotNull [] data, int offset, int length) throws IOException {
        checkOpen();
        if (currentOutputCell == null || writer.remaining() == 0) {
            flushCurrentOutputCell();
        }

        while (length > 0) {
            if (length < writer.remaining()) {
                writer.putByteArray(data, offset, length);
                return;
            }
            int writeCount = writer.remaining();
            writer.putByteArray(data, offset, writeCount);
            flushCurrentOutputCell();
            offset += writeCount;
            length -= writeCount;
        }
    }

    @Override
    public void flush() throws IOException {
        if (isClosed.get()) {
            return;
        }
        flushCurrentOutputCell();
    }

    private void checkOpen() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Output stream is closed");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (isClosed.get()) {
            return;
        }
        flush();
        currentOutputCell = null;
        stream.close();
        isClosed.set(true);
    }

    @Override
    public String toString() {
        return String.format("TorOutputStream stream=%d node=%s", stream.getStreamId(), stream.getTargetNode());
    }
}