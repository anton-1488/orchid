package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Globals;
import com.subgraph.orchid.circuits.CircuitManagerImpl.CircuitFilter;
import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.connections.ConnectionCache;
import com.subgraph.orchid.data.exitpolicy.ExitTarget;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.OpenExitStreamTask;
import com.subgraph.orchid.stream.StreamExitRequest;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class CircuitCreationTask implements Runnable {
    private static final Executor executor = Globals.VIRTUAL_EXECUTOR;
    private static final int MAX_CIRCUIT_DIRTINESS = 300; // seconds
    private static final int MAX_PENDING_CIRCUITS = 4;
    private static final Logger log = LoggerFactory.getLogger(CircuitCreationTask.class);

    private final Directory directory;
    private final ConnectionCache connectionCache;
    private final CircuitManagerImpl circuitManager;
    private final TorInitializationTracker initializationTracker;
    private final CircuitPathChooser pathChooser;
    private final CircuitBuildHandler buildHandler;
    private final CircuitBuildHandler internalBuildHandler;
    // To avoid obnoxiously printing a warning every second
    private int notEnoughDirectoryInformationWarningCounter = 0;

    private final CircuitPredictor predictor;
    private final AtomicLong lastNewCircuit;

    public CircuitCreationTask(Directory directory, ConnectionCache connectionCache, CircuitPathChooser pathChooser, CircuitManagerImpl circuitManager, TorInitializationTracker initializationTracker) {
        this.directory = directory;
        this.connectionCache = connectionCache;
        this.circuitManager = circuitManager;
        this.initializationTracker = initializationTracker;
        this.pathChooser = pathChooser;
        this.buildHandler = createCircuitBuildHandler();
        this.internalBuildHandler = createInternalCircuitBuildHandler();
        this.predictor = new CircuitPredictor();
        this.lastNewCircuit = new AtomicLong();
    }

    public CircuitPredictor getCircuitPredictor() {
        return predictor;
    }

    @Override
    public void run() {
        expireOldCircuits();
        assignPendingStreamsToActiveCircuits();
        checkExpiredPendingCircuits();
        checkCircuitsForCreation();
    }

    public void predictPort(int port) {
        predictor.addExitPortRequest(port);
    }

    private void assignPendingStreamsToActiveCircuits() {
        List<StreamExitRequest> pendingExitStreams = circuitManager.getPendingExitStreams();
        if (pendingExitStreams.isEmpty())
            return;

        for (ExitCircuit c : circuitManager.getRandomlyOrderedListOfExitCircuits()) {
            pendingExitStreams.removeIf(streamExitRequest -> attemptHandleStreamRequest(c, streamExitRequest));
        }
    }

    private boolean attemptHandleStreamRequest(@NotNull ExitCircuit c, @NotNull StreamExitRequest request) {
        if (c.canHandleExitToPort(request.port())) {
            if (request.reserveRequest()) {
                launchExitStreamTask(c, request);
            }
            // else request is reserved meaning another circuit is already trying to handle it
            return true;
        }
        return false;
    }

    private void launchExitStreamTask(ExitCircuit circuit, StreamExitRequest exitRequest) {
        OpenExitStreamTask task = new OpenExitStreamTask(circuit, exitRequest);
        executor.execute(task);
    }

    private void expireOldCircuits() {
        Set<Circuit> circuits = circuitManager.getCircuitsByFilter(circuit -> !circuit.isMarkedForClose() && circuit.getSecondsDirty() > MAX_CIRCUIT_DIRTINESS);
        for (Circuit c : circuits) {
            log.debug("Closing idle dirty circuit: {}", c);
            c.markForClose();
        }
    }

    private void checkExpiredPendingCircuits() {
        Set<Circuit> expiredPending = circuitManager.getCircuitsByFilter(Circuit::isPending);
        for (Circuit c : expiredPending) {
            log.debug("Pending circuit expired: {}", c);
            c.markForClose();
        }
    }

    private void checkCircuitsForCreation() {
        if (!directory.haveMinimumRouterInfo()) {
            if (notEnoughDirectoryInformationWarningCounter % 20 == 0) {
                log.info("Cannot build circuits because we don't have enough directory information");
            }
            notEnoughDirectoryInformationWarningCounter++;
            return;
        }

        buildCircuitIfNeeded();
        maybeBuildInternalCircuit();
    }

    private void buildCircuitIfNeeded() {
        if (connectionCache.isClosed()) {
            log.warn("Not building circuits, because connection cache is closed");
            return;
        }

        List<StreamExitRequest> pendingExitStreams = circuitManager.getPendingExitStreams();
        List<PredictedPortTarget> predictedPorts = predictor.getPredictedPortTargets();
        List<ExitTarget> exitTargets = new ArrayList<>();
        for (StreamExitRequest streamRequest : pendingExitStreams) {
            if (!streamRequest.isReserved() && countCircuitsSupportingTarget(streamRequest, false) == 0) {
                exitTargets.add(streamRequest);
            }
        }
        for (PredictedPortTarget ppt : predictedPorts) {
            if (countCircuitsSupportingTarget(ppt, true) < 2) {
                exitTargets.add(ppt);
            }
        }
        buildCircuitToHandleExitTargets(exitTargets);
    }

    private void maybeBuildInternalCircuit() {
        int needed = circuitManager.getNeededCleanCircuitCount(predictor.isInternalPredicted());
        if (needed > 0) {
            launchBuildTaskForInternalCircuit();
        }
    }

    private void launchBuildTaskForInternalCircuit() {
        log.debug("Launching new internal circuit");
        InternalCircuitImpl circuit = new InternalCircuitImpl(circuitManager);
        CircuitCreationRequest request = new CircuitCreationRequest(pathChooser, circuit, internalBuildHandler, false);
        CircuitBuildTask task = new CircuitBuildTask(request, connectionCache, circuitManager.isNtorEnabled());
        executor.execute(task);
        circuitManager.incrementPendingInternalCircuitCount();
    }

    private int countCircuitsSupportingTarget(final ExitTarget target, final boolean needClean) {
        CircuitFilter filter = circuit -> {
            if (!(circuit instanceof ExitCircuit ec)) {
                return false;
            }
            boolean pendingOrConnected = circuit.isPending() || circuit.isConnected();
            boolean isCleanIfNeeded = !(needClean && !circuit.isClean());
            return pendingOrConnected && isCleanIfNeeded && ec.canHandleExitToPort(target.port());
        };
        return circuitManager.getCircuitsByFilter(filter).size();
    }

    private void buildCircuitToHandleExitTargets(@NotNull List<ExitTarget> exitTargets) {
        if (exitTargets.isEmpty()) {
            return;
        }
        if (!directory.haveMinimumRouterInfo()) {
            return;
        }
        if (circuitManager.getPendingCircuitCount() >= MAX_PENDING_CIRCUITS) {
            return;
        }

        log.debug("Building new circuit to handle {} pending streams and predicted ports", exitTargets.size());
        launchBuildTaskForTargets(exitTargets);
    }

    private void launchBuildTaskForTargets(List<ExitTarget> exitTargets) {
        Router exitRouter = pathChooser.chooseExitNodeForTargets(exitTargets);
        if (exitRouter == null) {
            log.warn("Failed to select suitable exit node for targets");
            return;
        }

        Circuit circuit = circuitManager.createNewExitCircuit(exitRouter);
        CircuitCreationRequest request = new CircuitCreationRequest(pathChooser, circuit, buildHandler, false);
        CircuitBuildTask task = new CircuitBuildTask(request, connectionCache, circuitManager.isNtorEnabled(), initializationTracker);
        executor.execute(task);
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull CircuitBuildHandler createCircuitBuildHandler() {
        return new CircuitBuildHandler() {
            @Override
            public void circuitBuildCompleted(Circuit circuit) {
                log.debug("Circuit completed to: {}", circuit);
                circuitOpenedHandler(circuit);
                lastNewCircuit.set(System.currentTimeMillis());
            }

            @Override
            public void circuitBuildFailed(String reason) {
                log.debug("(BH) Circuit build failed: {}", reason);
                buildCircuitIfNeeded();
            }

            @Override
            public void connectionCompleted(Connection connection) {
                log.debug("(BH) Circuit connection completed to {}", connection);
            }

            @Override
            public void connectionFailed(String reason) {
                log.debug("(BH) Circuit connection failed: {}", reason);
                buildCircuitIfNeeded();
            }

            @Override
            public void nodeAdded(CircuitNode node) {
                log.debug("Node added to circuit: {}", node);
            }
        };
    }

    private void circuitOpenedHandler(Circuit circuit) {
        if (!(circuit instanceof ExitCircuit ec)) {
            return;
        }
        List<StreamExitRequest> pendingExitStreams = circuitManager.getPendingExitStreams();
        for (StreamExitRequest req : pendingExitStreams) {
            if (ec.canHandleExitToPort(req.port()) && req.reserveRequest()) {
                launchExitStreamTask(ec, req);
            }
        }
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull CircuitBuildHandler createInternalCircuitBuildHandler() {
        return new CircuitBuildHandler() {
            @Override
            public void nodeAdded(CircuitNode node) {
                log.debug("Node added to internal circuit: {}", node);
            }

            @Override
            public void connectionFailed(String reason) {
                log.debug("Circuit connection failed: {}", reason);
                circuitManager.decrementPendingInternalCircuitCount();
            }

            @Override
            public void connectionCompleted(Connection connection) {
                log.debug("Circuit connection completed to {}", connection);
            }

            @Override
            public void circuitBuildFailed(String reason) {
                log.debug("Circuit build failed: {}", reason);
                circuitManager.decrementPendingInternalCircuitCount();
            }

            @Override
            public void circuitBuildCompleted(Circuit circuit) {
                log.debug("Internal circuit build completed: {}", circuit);
                lastNewCircuit.set(System.currentTimeMillis());
                circuitManager.addCleanInternalCircuit((InternalCircuit) circuit);
            }
        };
    }
}