package com.subgraph.orchid.stream;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.enums.RelayCellReason;
import com.subgraph.orchid.cells.impls.RelayCellImpl;
import com.subgraph.orchid.cells.io.CellReader;
import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.circuits.CircuitImpl;
import com.subgraph.orchid.circuits.CircuitNode;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.exceptions.TorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamImpl implements Stream {
    private static final Logger log = LoggerFactory.getLogger(StreamImpl.class);

    private final static int STREAMWINDOW_START = 500;
    private final static int STREAMWINDOW_INCREMENT = 50;
    private final static int STREAMWINDOW_MAX_UNFLUSHED = 10;

    private final int streamId;
    private final boolean autoclose;

    private final CircuitImpl circuit;
    private final CircuitNode targetNode;
    private final TorInputStream inputStream;
    private final TorOutputStream outputStream;

    private final Semaphore packageWindowSemaphore = new Semaphore(STREAMWINDOW_START);
    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicInteger packageWindow = new AtomicInteger(STREAMWINDOW_START);
    private final AtomicInteger deliverWindow = new AtomicInteger(STREAMWINDOW_START);
    private final AtomicBoolean relayConnectedReceived = new AtomicBoolean(false);

    public StreamImpl(CircuitImpl circuit, CircuitNode targetNode, int streamId, boolean autoclose) {
        this.circuit = circuit;
        this.targetNode = targetNode;
        this.streamId = streamId;
        this.autoclose = autoclose;
        this.inputStream = new TorInputStream(this);
        this.outputStream = new TorOutputStream(this);
    }

    public void addInputCell(RelayCell cell) {
        if (isClosed.get()) {
            return;
        }
        CellReader reader = cell.getCellReader();
        switch (cell.getRelayCommand()) {
            case END -> {
                int reason = reader.getByte();
                connectFuture.completeExceptionally(new StreamConnectFailedException("Stream connect failed", reason));
                inputStream.addEndCell(cell);
            }
            case CONNECTED -> {
                if (relayConnectedReceived.compareAndSet(false, true)) {
                    connectFuture.complete(null);
                }
            }
            case SENDME -> {
                int newVal = packageWindow.addAndGet(STREAMWINDOW_INCREMENT);
                if (newVal > STREAMWINDOW_START) {
                    throw new TorException("Protocol violation: package window exceeded max");
                }
                packageWindowSemaphore.release(STREAMWINDOW_INCREMENT);
            }
            default -> {
                inputStream.addInputCell(cell);
                int remaining = deliverWindow.decrementAndGet();
                if (remaining < 0) {
                    throw new TorException("Stream has negative delivery window");
                }
                considerSendingSendme();
            }
        }
    }

    private void waitForConnect(long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        try {
            connectFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new TorException(e);
        }
    }

    private void considerSendingSendme() {
        int current = deliverWindow.get();

        if (current > (STREAMWINDOW_START - STREAMWINDOW_INCREMENT)) {
            return;
        }
        if (inputStream.unflushedCellCount() >= STREAMWINDOW_MAX_UNFLUSHED) {
            return;
        }
        if (deliverWindow.compareAndSet(current, current + STREAMWINDOW_INCREMENT)) {
            RelayCell sendme = circuit.createRelayCell(RelayCellCommand.SENDME, streamId, targetNode);
            circuit.sendRelayCell(sendme);
        }
    }

    public Semaphore getPackageWindowSemaphore() {
        return packageWindowSemaphore;
    }

    @Override
    public int getStreamId() {
        return streamId;
    }

    @Override
    public Circuit getCircuit() {
        return circuit;
    }

    @Override
    public CircuitNode getTargetNode() {
        return targetNode;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void openDirectory(long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        RelayCell cell = new RelayCellImpl(circuit.getFinalCircuitNode(), circuit.getCircuitId(), streamId, RelayCellCommand.BEGIN_DIR);
        circuit.sendRelayCellToFinalNode(cell);
        waitForConnect(timeout);
    }

    public void openExit(String target, int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        RelayCell cell = new RelayCellImpl(circuit.getFinalCircuitNode(), circuit.getCircuitId(), streamId, RelayCellCommand.BEGIN);
        cell.getCellWriter().putString(target + ":" + port);
        circuit.sendRelayCellToFinalNode(cell);
        waitForConnect(timeout);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed.compareAndSet(false, true)) {
            return;
        }
        log.debug("Closing stream");

        inputStream.close();
        outputStream.close();
        circuit.removeStream(this);

        if (autoclose) {
            circuit.markForClose();
        }

        RelayCell cell = new RelayCellImpl(circuit.getFinalCircuitNode(), circuit.getCircuitId(), streamId, RelayCellCommand.END);
        cell.getCellWriter().putByte((byte) RelayCellReason.DONE.getReason());
        circuit.sendRelayCellToFinalNode(cell);
    }

    @Override
    public String toString() {
        return String.format("[Stream stream_id=%d, circuit=%s]", streamId, circuit.toString());
    }
}