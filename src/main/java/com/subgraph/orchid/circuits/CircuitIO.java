package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.cells.enums.CellCommand;
import com.subgraph.orchid.cells.enums.CellError;
import com.subgraph.orchid.cells.enums.RelayCellCommand;
import com.subgraph.orchid.cells.impls.CellImpl;
import com.subgraph.orchid.cells.impls.RelayCellImpl;
import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.exceptions.ConnectionIOException;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.stream.StreamImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.subgraph.orchid.cells.enums.RelayCellCommand.DATA;
import static com.subgraph.orchid.cells.enums.RelayCellCommand.SENDME;

public class CircuitIO {
    private final static long CIRCUIT_BUILD_TIMEOUT_MS = 30 * 1000;
    private final static long CIRCUIT_RELAY_RESPONSE_TIMEOUT = 20 * 1000;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CircuitIO.class);

    private final CircuitImpl circuit;
    private final Connection connection;
    private final int circuitId;
    private final BlockingQueue<RelayCell> relayCellResponseQueue;
    private final BlockingQueue<Cell> controlCellResponseQueue;
    private final Map<Integer, StreamImpl> streamMap;

    private volatile boolean isMarkedForClose;
    private volatile boolean isClosed;

    public CircuitIO(CircuitImpl circuit, Connection connection, int circuitId) {
        this.circuit = circuit;
        this.connection = connection;
        this.circuitId = circuitId;

        this.relayCellResponseQueue = new LinkedBlockingQueue<>();
        this.controlCellResponseQueue = new LinkedBlockingQueue<>();
        this.streamMap = new HashMap<>();
    }

    public Connection getConnection() {
        return connection;
    }

    public int getCircuitId() {
        return circuitId;
    }

    public RelayCell dequeueRelayResponseCell() {
        try {
            long timeout = getReceiveTimeout();
            return relayCellResponseQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Contract("_ -> new")
    private @NotNull RelayCell decryptRelayCell(Cell cell) {
        for (CircuitNode node : circuit.getNodeList()) {
            if (node.decryptBackwardCell(cell)) {
                return new RelayCellImpl(node, cell.getCellBytes());
            }
        }
        destroyCircuit();
        throw new TorException("Could not decrypt relay cell");
    }

    public Cell receiveControlCellResponse() {
        try {
            long timeout = getReceiveTimeout();
            return controlCellResponseQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private long getReceiveTimeout() {
        if (circuit.getStatus().isBuilding())
            return remainingBuildTime();
        else
            return CIRCUIT_RELAY_RESPONSE_TIMEOUT;
    }

    private long remainingBuildTime() {
        final long elapsed = circuit.getStatus().getMillisecondsElapsedSinceCreated();
        if (elapsed == 0 || elapsed >= CIRCUIT_BUILD_TIMEOUT_MS)
            return 0;
        return CIRCUIT_BUILD_TIMEOUT_MS - elapsed;
    }

    /**
     * This is called by the cell reading thread in ConnectionImpl to deliver control cells
     * associated with this circuit (CREATED, CREATED_FAST, or DESTROY).
     */
    public void deliverControlCell(@NotNull Cell cell) {
        if (cell.getCommand() == CellCommand.DESTROY) {
            processDestroyCell(cell.getCellReader().getByte());
        } else {
            controlCellResponseQueue.add(cell);
        }
    }

    private void processDestroyCell(int reason) {
        log.debug("DESTROY cell received on {}", circuit);
        destroyCircuit();
    }

    /**
     * This is called by the cell reading thread in ConnectionImpl to deliver RELAY cells.
     */
    public synchronized void deliverRelayCell(Cell cell) {
        circuit.getStatus().updateDirtyTimestamp();
        RelayCell relayCell = decryptRelayCell(cell);
        log.debug("Dispatching: {}", relayCell);
        switch (relayCell.getRelayCommand()) {
            case EXTENDED, EXTENDED2, RESOLVED, TRUNCATED, RENDEZVOUS_ESTABLISHED, INTRODUCE_ACK, RENDEZVOUS2:
                relayCellResponseQueue.add(relayCell);
                break;
            case DATA, END, CONNECTED:
                processRelayDataCell(relayCell);
                break;
            case SENDME:
                if (relayCell.getStreamId() != 0) {
                    processRelayDataCell(relayCell);
                } else {
                    processCircuitSendme(relayCell);
                }
                break;
            default:
                destroyCircuit();
                throw new TorException("Unexpected 'forward' direction relay cell type: " + relayCell.getRelayCommand());
        }
    }

    /**
     * Runs in the context of the connection cell reading thread
     */
    private void processRelayDataCell(@NotNull RelayCell cell) {
        if (cell.getRelayCommand() == DATA) {
            cell.getCircuitNode().decrementDeliverWindow();
            if (cell.getCircuitNode().considerSendingSendme()) {
                RelayCell sendme = createRelayCell(SENDME, 0, cell.getCircuitNode());
                sendRelayCellTo(sendme, sendme.getCircuitNode());
            }
        }
        StreamImpl stream = streamMap.get(cell.getStreamId());
        // It's not unusual for the stream to not be found.  For example, if a RELAY_CONNECTED arrives after
        // the client has stopped waiting for it, the stream will never be tracked and eventually the edge node
        // will send a RELAY_END for this stream.
        if (stream != null) {
            stream.addInputCell(cell);
        }
    }

    public RelayCell createRelayCell(RelayCellCommand relayCommand, int streamId, CircuitNode targetNode) {
        return new RelayCellImpl(targetNode, circuitId, streamId, relayCommand);
    }

    public synchronized void sendRelayCellTo(RelayCell cell, @NotNull CircuitNode targetNode) {
        log.debug("Sending: {}", cell);
        cell.setLength();
        targetNode.updateForwardDigest(cell);
        cell.setDigest(targetNode.getForwardDigestBytes());

        for (CircuitNode node = targetNode; node != null; node = node.getPreviousNode()) {
            node.encryptForwardCell(cell);
        }
        if (cell.getRelayCommand() == DATA) {
            targetNode.waitForSendWindowAndDecrement();
        }

        sendCell(cell);
    }

    public void sendCell(Cell cell) {
        CircuitStatus status = circuit.getStatus();
        if (!(status.isConnected() || status.isBuilding())) {
            return;
        }
        try {
            status.updateDirtyTimestamp();
            connection.sendCell(cell);
        } catch (ConnectionIOException e) {
            destroyCircuit();
        }
    }

    public synchronized void markForClose() {
        boolean shouldClose;
        if (isMarkedForClose) {
            return;
        }
        isMarkedForClose = true;
        shouldClose = streamMap.isEmpty();
        if (shouldClose) {
            closeCircuit();
        }
    }

    public synchronized boolean isMarkedForClose() {
        return isMarkedForClose;
    }

    private synchronized void closeCircuit() {
        log.debug("Closing circuit {}", circuit);
        sendDestroyCell(CellError.NONE);
        connection.removeCircuit(circuit);
        circuit.setStateDestroyed();
        isClosed = true;
    }

    public void sendDestroyCell(@NotNull CellError reason) {
        Cell destroy = new CellImpl(circuitId, CellCommand.DESTROY);
        destroy.getCellWriter().putByte((byte) reason.getError());
        try {
            connection.sendCell(destroy);
        } catch (ConnectionIOException e) {
            log.error("Connection IO error sending DESTROY cell: ", e);
        }
    }

    private void processCircuitSendme(@NotNull RelayCell cell) {
        cell.getCircuitNode().incrementSendWindow();
    }

    public synchronized void destroyCircuit() {
        if (isClosed) {
            return;
        }
        circuit.setStateDestroyed();
        connection.removeCircuit(circuit);
        List<StreamImpl> tmpList = new ArrayList<>(streamMap.values());
        for (StreamImpl s : tmpList) {
            try { s.close(); } catch (Exception ignored) {}
        }
        isClosed = true;
    }

    public synchronized StreamImpl createNewStream(boolean autoclose) {
        int streamId = circuit.getStatus().nextStreamId();
        StreamImpl stream = new StreamImpl(circuit, circuit.getFinalCircuitNode(), streamId, autoclose);
        streamMap.put(streamId, stream);
        return stream;
    }

    public synchronized void removeStream(@NotNull StreamImpl stream) {
        boolean shouldClose;
        streamMap.remove(stream.getStreamId());
        shouldClose = streamMap.isEmpty() && isMarkedForClose;
        if (shouldClose) {
            closeCircuit();
        }
    }

    public synchronized List<Stream> getActiveStreams() {
        return List.copyOf(streamMap.values());
    }
}