package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.data.exitpolicy.ExitTarget;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.StreamImpl;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class ExitCircuitImpl extends CircuitImpl implements ExitCircuit {
    private final Router exitRouter;
    private final Set<ExitTarget> failedExitRequests;

    public ExitCircuitImpl(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        super(circuitManager, prechosenPath);
        this.exitRouter = prechosenPath.getLast();
        this.failedExitRequests = new HashSet<>();
    }

    public ExitCircuitImpl(CircuitManagerImpl circuitManager, Router exitRouter) {
        super(circuitManager);
        this.exitRouter = exitRouter;
        this.failedExitRequests = new HashSet<>();
    }

    @Override
    public Stream openExitStream(@NotNull InetAddress address, int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        return openExitStream(address.toString(), port, timeout);
    }

    @Override
    public Stream openExitStream(String target, int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        StreamImpl stream = createNewStream();
        try {
            stream.openExit(target, port, timeout);
            return stream;
        } catch (Exception e) {
            removeStream(stream);
            throw e;
        }
    }

    public synchronized void recordFailedExitTarget(ExitTarget target) {
        failedExitRequests.add(target);
    }

    public synchronized boolean canHandleExitTo(ExitTarget target) {
        if (failedExitRequests.contains(target)) {
            return false;
        }

        if (isMarkedForClose()) {
            return false;
        }

        if (target.isAddressTarget()) {
            return exitRouter.exitPolicyAccepts(target.getAddress(), target.port());
        } else {
            return exitRouter.exitPolicyAccepts(target.port());
        }
    }

    @Override
    public boolean canHandleExitToPort(int port) {
        return exitRouter.exitPolicyAccepts(port);
    }

    @Override
    protected List<Router> choosePathForCircuit(CircuitPathChooser pathChooser) throws PathSelectionFailedException {
        return pathChooser.choosePathWithExit(exitRouter);
    }
}