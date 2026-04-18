package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.StreamImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents an established circuit through the Tor network.
 *
 */
public abstract class CircuitImpl implements Circuit {
    @Contract("_, _ -> new")
    public static @NotNull ExitCircuit createExitCircuit(CircuitManagerImpl circuitManager, Router exitRouter) {
        return new ExitCircuitImpl(circuitManager, exitRouter);
    }

    @Contract("_, _ -> new")
    public static @NotNull ExitCircuit createExitCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        return new ExitCircuitImpl(circuitManager, prechosenPath);
    }

    @Contract("_ -> new")
    public static @NotNull DirectoryCircuit createDirectoryCircuit(CircuitManagerImpl circuitManager) {
        return new DirectoryCircuitImpl(circuitManager, null);
    }

    @Contract("_, _ -> new")
    public static @NotNull DirectoryCircuit createDirectoryCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        return new DirectoryCircuitImpl(circuitManager, prechosenPath);
    }

    @Contract("_, _ -> new")
    public static @NotNull InternalCircuit createInternalCircuitTo(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        return new InternalCircuitImpl(circuitManager, prechosenPath);
    }

    private final CircuitManagerImpl circuitManager;
    protected final List<Router> prechosenPath;
    private final List<CircuitNode> nodeList;
    private final CircuitStatus status;

    private CircuitIO io;

    protected CircuitImpl(CircuitManagerImpl circuitManager) {
        this(circuitManager, null);
    }

    protected CircuitImpl(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        nodeList = new ArrayList<>();
        this.circuitManager = circuitManager;
        this.prechosenPath = prechosenPath;
        status = new CircuitStatus();
    }

    List<Router> choosePath(CircuitPathChooser pathChooser) throws InterruptedException, PathSelectionFailedException {
        if (prechosenPath != null) {
            return new ArrayList<>(prechosenPath);
        } else {
            return choosePathForCircuit(pathChooser);
        }
    }

    protected abstract List<Router> choosePathForCircuit(CircuitPathChooser pathChooser) throws InterruptedException, PathSelectionFailedException;

    public void bindToConnection(Connection connection) {
        if (io != null) {
            throw new IllegalStateException("Circuit already bound to a connection");
        }
        int id = connection.bindCircuit(this);
        io = new CircuitIO(this, connection, id);
    }

    @Override
    public void markForClose() {
        if (io != null) {
            io.markForClose();
        }
    }

    @Override
    public boolean isMarkedForClose() {
        if (io == null) {
            return false;
        } else {
            return io.isMarkedForClose();
        }
    }

    public CircuitStatus getStatus() {
        return status;
    }

    @Override
    public boolean isConnected() {
        return status.isConnected();
    }

    @Override
    public boolean isPending() {
        return status.isBuilding();
    }

    @Override
    public boolean isClean() {
        return !status.isDirty();
    }

    @Override
    public int getSecondsDirty() {
        return (int) (status.getMillisecondsDirty() / 1000);
    }

    public void notifyCircuitBuildStart() {
        if (!status.isUnconnected()) {
            throw new IllegalStateException("Can only connect UNCONNECTED circuits");
        }
        status.updateCreatedTimestamp();
        status.setStateBuilding();
        circuitManager.addActiveCircuit(this);
    }

    public void notifyCircuitBuildFailed() {
        status.setStateFailed();
        circuitManager.removeActiveCircuit(this);
    }

    public void notifyCircuitBuildCompleted() {
        status.setStateOpen();
        status.updateCreatedTimestamp();
    }

    @Override
    public Connection getConnection() {
        if (!isConnected()) {
            throw new TorException("Circuit is not connected.");
        }
        return io.getConnection();
    }

    @Override
    public int getCircuitId() {
        if (io == null) {
            return 0;
        } else {
            return io.getCircuitId();
        }
    }

    @Override
    public void sendRelayCell(RelayCell cell) {
        io.sendRelayCellTo(cell, cell.getCircuitNode());
    }

    public void sendRelayCellToFinalNode(RelayCell cell) {
        io.sendRelayCellTo(cell, getFinalCircuitNode());
    }

    @Override
    public void appendNode(CircuitNode node) {
        nodeList.add(node);
    }

    public List<CircuitNode> getNodeList() {
        return nodeList;
    }

    public int getCircuitLength() {
        return nodeList.size();
    }

    @Override
    public CircuitNode getFinalCircuitNode() {
        if (nodeList.isEmpty()) {
            throw new TorException("getFinalCircuitNode() called on empty circuit");
        }
        return nodeList.get(getCircuitLength() - 1);
    }

    @Override
    public RelayCell createRelayCell(int relayCommand, int streamId, CircuitNode targetNode) {
        return io.createRelayCell(relayCommand, streamId, targetNode);
    }

    @Override
    public RelayCell receiveRelayCell() {
        return io.dequeueRelayResponseCell();
    }

    public void sendCell(Cell cell) {
        io.sendCell(cell);
    }

    public Cell receiveControlCellResponse() {
        return io.receiveControlCellResponse();
    }

    /**
     * This is called by the cell reading thread in ConnectionImpl to deliver control cells
     * associated with this circuit (CREATED or CREATED_FAST).
     */
    @Override
    public void deliverControlCell(Cell cell) {
        io.deliverControlCell(cell);
    }

    /**
     * This is called by the cell reading thread in ConnectionImpl to deliver RELAY cells.
     */
    @Override
    public void deliverRelayCell(Cell cell) {
        io.deliverRelayCell(cell);
    }

    protected StreamImpl createNewStream(boolean autoclose) {
        return io.createNewStream(autoclose);
    }

    protected StreamImpl createNewStream() {
        return createNewStream(false);
    }

    public void setStateDestroyed() {
        status.setStateDestroyed();
        circuitManager.removeActiveCircuit(this);
    }

    @Override
    public void destroyCircuit() {
        // We might not have bound this circuit yet
        if (io != null) {
            io.destroyCircuit();
        }
        circuitManager.removeActiveCircuit(this);
    }

    public void removeStream(StreamImpl stream) {
        io.removeStream(stream);
    }

    @Override
    public List<Stream> getActiveStreams() {
        if (io == null) {
            return Collections.emptyList();
        } else {
            return io.getActiveStreams();
        }
    }

    @Override
    public String toString() {
        return String.format("Circuit[id=%d, nodes=%d]", getCircuitId(), nodeList.size());
    }
}