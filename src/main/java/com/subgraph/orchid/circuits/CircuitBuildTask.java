package com.subgraph.orchid.circuits;

import com.subgraph.orchid.BootstrapStatus;
import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.connections.ConnectionCache;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.ConnectionFailedException;
import com.subgraph.orchid.exceptions.ConnectionHandshakeException;
import com.subgraph.orchid.exceptions.ConnectionTimeoutException;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBuildTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CircuitBuildTask.class);
    private final CircuitCreationRequest creationRequest;
    private final ConnectionCache connectionCache;
    private final TorInitializationTracker initializationTracker;
    private final CircuitImpl circuit;
    private final CircuitExtender extender;

    private Connection connection = null;

    public CircuitBuildTask(CircuitCreationRequest request, ConnectionCache connectionCache, boolean ntorEnabled) {
        this(request, connectionCache, ntorEnabled, null);
    }

    public CircuitBuildTask(@NotNull CircuitCreationRequest request, ConnectionCache connectionCache, boolean ntorEnabled, TorInitializationTracker initializationTracker) {
        this.creationRequest = request;
        this.connectionCache = connectionCache;
        this.initializationTracker = initializationTracker;
        this.circuit = request.getCircuit();
        this.extender = new CircuitExtender(request.getCircuit(), ntorEnabled);
    }

    @Override
    public void run() {
        Router firstRouter = null;
        try {
            circuit.notifyCircuitBuildStart();
            creationRequest.choosePath();
            log.debug("Opening a new circuit to {}", pathToString(creationRequest));
            firstRouter = creationRequest.getPathElement(0);
            openEntryNodeConnection(firstRouter);
            buildCircuit(firstRouter);
            circuit.notifyCircuitBuildCompleted();
        } catch (ConnectionTimeoutException | ConnectionFailedException | ConnectionHandshakeException e) {
            connectionFailed("Error to connection " + firstRouter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            circuitBuildFailed("Circuit building thread interrupted");
        } catch (Exception e) {
            circuitBuildFailed(e.getMessage());
        }
    }

    private @NotNull String pathToString(@NotNull CircuitCreationRequest ccr) {
        StringBuilder sb = new StringBuilder("[");
        for (Router r : ccr.getPath()) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(r.getNickname());
        }
        sb.append("]");
        return sb.toString();
    }

    private void connectionFailed(String message) {
        creationRequest.connectionFailed(message);
        circuit.notifyCircuitBuildFailed();
    }

    private void circuitBuildFailed(String message) {
        creationRequest.circuitBuildFailed(message);
        circuit.notifyCircuitBuildFailed();
        if (connection != null) {
            connection.removeCircuit(circuit);
        }
    }

    private void openEntryNodeConnection(Router firstRouter) throws ConnectionTimeoutException, ConnectionFailedException, ConnectionHandshakeException, InterruptedException {
        connection = connectionCache.getConnectionTo(firstRouter, creationRequest.isDirectoryCircuit());
        circuit.bindToConnection(connection);
        creationRequest.connectionCompleted(connection);
    }

    private void buildCircuit(Router firstRouter) throws TorException {
        notifyInitialization();
        CircuitNode firstNode = extender.createFastTo(firstRouter);
        creationRequest.nodeAdded(firstNode);

        for (int i = 1; i < creationRequest.getPathLength(); i++) {
            CircuitNode extendedNode = extender.extendTo(creationRequest.getPathElement(i));
            creationRequest.nodeAdded(extendedNode);
        }
        creationRequest.circuitBuildCompleted(circuit);
        notifyDone();
    }

    private void notifyInitialization() {
        if (initializationTracker != null) {
            BootstrapStatus status = creationRequest.isDirectoryCircuit() ? BootstrapStatus.ONEHOP_CREATE : BootstrapStatus.CIRCUIT_CREATE;
            initializationTracker.notifyEvent(status);
        }
    }

    private void notifyDone() {
        if (initializationTracker != null && !creationRequest.isDirectoryCircuit()) {
            initializationTracker.notifyEvent(BootstrapStatus.DONE);
        }
    }
}