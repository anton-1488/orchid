package com.subgraph.orchid.circuits;

import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;

import java.util.Collections;
import java.util.List;

public class CircuitCreationRequest implements CircuitBuildHandler {
    private final CircuitImpl circuit;
    private final CircuitPathChooser pathChooser;
    private final CircuitBuildHandler buildHandler;
    private final boolean isDirectoryCircuit;

    private List<Router> path;

    public CircuitCreationRequest(CircuitPathChooser pathChooser, Circuit circuit, CircuitBuildHandler buildHandler, boolean isDirectoryCircuit) {
        this.pathChooser = pathChooser;
        this.circuit = (CircuitImpl) circuit;
        this.buildHandler = buildHandler;
        this.path = Collections.emptyList();
        this.isDirectoryCircuit = isDirectoryCircuit;
    }

    public void choosePath() throws InterruptedException, PathSelectionFailedException {
        if (circuit == null) {
            throw new IllegalArgumentException();
        }
        path = circuit.choosePath(pathChooser);
    }

    public CircuitImpl getCircuit() {
        return circuit;
    }

    public List<Router> getPath() {
        return path;
    }

    public int getPathLength() {
        return path.size();
    }

    public Router getPathElement(int idx) {
        return path.get(idx);
    }

    public CircuitBuildHandler getBuildHandler() {
        return buildHandler;
    }

    public boolean isDirectoryCircuit() {
        return isDirectoryCircuit;
    }

    @Override
    public void connectionCompleted(Connection connection) {
        if (buildHandler != null) {
            buildHandler.connectionCompleted(connection);
        }
    }

    @Override
    public void connectionFailed(String reason) {
        if (buildHandler != null) {
            buildHandler.connectionFailed(reason);
        }
    }

    @Override
    public void nodeAdded(CircuitNode node) {
        if (buildHandler != null) {
            buildHandler.nodeAdded(node);
        }
    }

    @Override
    public void circuitBuildCompleted(Circuit circuit) {
        if (buildHandler != null) {
            buildHandler.circuitBuildCompleted(circuit);
        }
    }

    @Override
    public void circuitBuildFailed(String reason) {
        if (buildHandler != null) {
            buildHandler.circuitBuildFailed(reason);
        }
    }
}