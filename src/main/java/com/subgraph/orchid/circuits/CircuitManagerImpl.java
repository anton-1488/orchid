package com.subgraph.orchid.circuits;

import com.subgraph.orchid.BootstrapStatus;
import com.subgraph.orchid.Stream;
import com.subgraph.orchid.bridge.EntryGuards;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.connections.Connection;
import com.subgraph.orchid.connections.ConnectionCache;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.data.InetAddressUtils;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.downloader.DirectoryDownloaderImpl;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.hiddenservice.HiddenServiceManager;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.PendingExitStreams;
import com.subgraph.orchid.stream.StreamExitRequest;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

public class CircuitManagerImpl implements CircuitManager {
    private final static int OPEN_DIRECTORY_STREAM_RETRY_COUNT = 5;
    private final static int OPEN_DIRECTORY_STREAM_TIMEOUT = 10 * 1000;

    public interface CircuitFilter {
        boolean filter(Circuit circuit);
    }

    private final Directory directory;
    private final ConnectionCache connectionCache;
    private final Set<CircuitImpl> activeCircuits = new CopyOnWriteArraySet<>();
    private final Queue<InternalCircuit> cleanInternalCircuits = new LinkedList<>();
    private final PendingExitStreams pendingExitStreams;
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final CircuitCreationTask circuitCreationTask;
    private final TorInitializationTracker initializationTracker;
    private final CircuitPathChooser pathChooser;
    private final HiddenServiceManager hiddenServiceManager;

    private volatile boolean isBuilding = false;
    private int requestedInternalCircuitCount = 0;
    private int pendingInternalCircuitCount = 0;

    public CircuitManagerImpl(TorConfig config, DirectoryDownloaderImpl directoryDownloader, Directory directory, ConnectionCache connectionCache, TorInitializationTracker initializationTracker) {
        this.directory = directory;
        this.connectionCache = connectionCache;
        this.pathChooser = CircuitPathChooser.create(config, directory);
        if (config.useEntryGuards() || config.useBridges()) {
            this.pathChooser.enableEntryGuards(new EntryGuards(config, connectionCache, directoryDownloader, directory));
        }
        this.pendingExitStreams = new PendingExitStreams(config);
        this.circuitCreationTask = new CircuitCreationTask(directory, connectionCache, pathChooser, this, initializationTracker);
        this.initializationTracker = initializationTracker;
        this.hiddenServiceManager = new HiddenServiceManager(config, directory, this);
        directoryDownloader.setCircuitManager(this);
    }

    @Override
    public synchronized void startBuildingCircuits() {
        isBuilding = true;
        scheduledExecutor.scheduleAtFixedRate(circuitCreationTask, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void stopBuildingCircuits(boolean killCircuits) {
        isBuilding = false;
        scheduledExecutor.shutdownNow();

        if (killCircuits) {
            List<CircuitImpl> circuits = List.copyOf(activeCircuits);
            for (CircuitImpl c : circuits) {
                c.destroyCircuit();
            }
        }
    }

    public ExitCircuit createNewExitCircuit(Router exitRouter) {
        return CircuitImpl.createExitCircuit(this, exitRouter);
    }

    public synchronized void addActiveCircuit(CircuitImpl circuit) {
        activeCircuits.add(circuit);
        activeCircuits.notifyAll();

        boolean doDestroy = !isBuilding;
        if (doDestroy) {
            // we were asked to stop since this circuit was started
            circuit.destroyCircuit();
        }
    }

    public void removeActiveCircuit(CircuitImpl circuit) {
        activeCircuits.remove(circuit);
    }

    public int getActiveCircuitCount() {
        return activeCircuits.size();
    }

    public Set<Circuit> getPendingCircuits() {
        return getCircuitsByFilter(Circuit::isPending);
    }

    public synchronized int getPendingCircuitCount() {
        return getPendingCircuits().size();
    }

    public Set<Circuit> getCircuitsByFilter(CircuitFilter filter) {
        Set<Circuit> result = new HashSet<>();
        Set<CircuitImpl> circuits = new HashSet<>(activeCircuits);

        for (CircuitImpl c : circuits) {
            if (filter == null || filter.filter(c)) {
                result.add(c);
            }
        }
        return result;
    }

    public List<ExitCircuit> getRandomlyOrderedListOfExitCircuits() {
        Set<Circuit> notDirectory = getCircuitsByFilter(circuit -> (circuit instanceof ExitCircuit) && !circuit.isMarkedForClose() && circuit.isConnected());
        ArrayList<ExitCircuit> ac = new ArrayList<>();
        for (Circuit c : notDirectory) {
            if (c instanceof ExitCircuit) {
                ac.add((ExitCircuit) c);
            }
        }
        int sz = ac.size();
        for (int i = 0; i < sz; i++) {
            ExitCircuit tmp = ac.get(i);
            int swapIdx = TorRandom.nextInt(sz);
            ac.set(i, ac.get(swapIdx));
            ac.set(swapIdx, tmp);
        }
        return ac;
    }

    @Override
    public Stream openExitStreamTo(String hostname, int port) throws InterruptedException, TimeoutException {
        if (hostname.endsWith(".onion")) {
            return hiddenServiceManager.getStreamTo(hostname, port);
        }
        validateHostname(hostname);
        circuitCreationTask.predictPort(port);
        return pendingExitStreams.openExitStream(hostname, port);
    }

    private void validateHostname(String hostname) throws OpenFailedException {
        maybeRejectInternalAddress(hostname);
        if (hostname.toLowerCase().endsWith(".onion")) {
            throw new OpenFailedException("Hidden services not supported");
        } else if (hostname.toLowerCase().endsWith(".exit")) {
            throw new OpenFailedException(".exit addresses are not supported");
        }
    }

    private void maybeRejectInternalAddress(String hostname) {
        maybeRejectInternalAddress(Objects.requireNonNull(InetAddressUtils.createAddressFromString(hostname)));
    }

    private void maybeRejectInternalAddress(@NotNull InetAddress address) {
        if (address.isSiteLocalAddress()) {
            throw new OpenFailedException("Rejecting stream target with internal address: " + address);
        }
    }

    @Override
    public Stream openExitStreamTo(InetAddress address, int port) throws InterruptedException {
        maybeRejectInternalAddress(address);
        circuitCreationTask.predictPort(port);
        return pendingExitStreams.openExitStream(address, port);
    }

    public List<StreamExitRequest> getPendingExitStreams() {
        return pendingExitStreams.getUnreservedPendingRequests();
    }

    @Override
    public Stream openDirectoryStream() throws InterruptedException {
        return openDirectoryStream(0);
    }

    @Override
    public Stream openDirectoryStream(int purpose) throws InterruptedException {
        BootstrapStatus requestEventCode = CircuitManager.purposeToBootstrapStatus(purpose, false);
        BootstrapStatus loadingEventCode = CircuitManager.purposeToBootstrapStatus(purpose, true);

        int failCount = 0;
        while (failCount < OPEN_DIRECTORY_STREAM_RETRY_COUNT) {
            DirectoryCircuit circuit = openDirectoryCircuit();
            initializationTracker.notifyEvent(requestEventCode);
            try {
                Stream stream = circuit.openDirectoryStream(OPEN_DIRECTORY_STREAM_TIMEOUT, true);
                initializationTracker.notifyEvent(loadingEventCode);
                return stream;
            } catch (StreamConnectFailedException e) {
                circuit.markForClose();
                failCount += 1;
            } catch (TimeoutException e) {
                circuit.markForClose();
            }
        }
        throw new OpenFailedException("Retry count exceeded opening directory stream");
    }

    @Override
    public DirectoryCircuit openDirectoryCircuit() {
        int failCount = 0;
        while (failCount < OPEN_DIRECTORY_STREAM_RETRY_COUNT) {
            DirectoryCircuit circuit = CircuitImpl.createDirectoryCircuit(this);
            if (tryOpenCircuit(circuit, true, true)) {
                return circuit;
            }
            failCount += 1;
        }
        throw new OpenFailedException("Could not create circuit for directory stream");
    }

    private static class DirectoryCircuitResult implements CircuitBuildHandler {
        private boolean isFailed;

        @Override
        public void connectionCompleted(Connection connection) {
        }

        @Override
        public void nodeAdded(CircuitNode node) {
        }

        @Override
        public void circuitBuildCompleted(Circuit circuit) {
        }

        @Override
        public void connectionFailed(String reason) {
            isFailed = true;
        }

        @Override
        public void circuitBuildFailed(String reason) {
            isFailed = true;
        }

        boolean isSuccessful() {
            return !isFailed;
        }
    }

    @Override
    public synchronized InternalCircuit getCleanInternalCircuit() throws InterruptedException {
        try {
            requestedInternalCircuitCount += 1;
            while (cleanInternalCircuits.isEmpty()) {
                cleanInternalCircuits.wait();
            }
            return cleanInternalCircuits.remove();
        } finally {
            requestedInternalCircuitCount -= 1;
        }
    }

    public synchronized int getNeededCleanCircuitCount(boolean isPredicted) {
        int predictedCount = (isPredicted) ? 2 : 0;
        int needed = Math.max(requestedInternalCircuitCount, predictedCount) - (pendingInternalCircuitCount + cleanInternalCircuits.size());
        return Math.max(needed, 0);
    }

    public void incrementPendingInternalCircuitCount() {
        pendingInternalCircuitCount += 1;
    }

    public void decrementPendingInternalCircuitCount() {
        pendingInternalCircuitCount -= 1;
    }

    public synchronized void addCleanInternalCircuit(InternalCircuit circuit) {
        pendingInternalCircuitCount -= 1;
        cleanInternalCircuits.add(circuit);
        cleanInternalCircuits.notifyAll();
    }

    public boolean isNtorEnabled() {
        return true;
    }

    public boolean isNtorEnabledInConsensus() {
        ConsensusDocument consensus = directory.getCurrentConsensusDocument();
        return (consensus != null) && (consensus.getUseNTorHandshake());
    }

    @Override
    public DirectoryCircuit openDirectoryCircuitTo(List<Router> path) throws OpenFailedException {
        DirectoryCircuit circuit = CircuitImpl.createDirectoryCircuitTo(this, path);
        if (!tryOpenCircuit(circuit, true, false)) {
            throw new OpenFailedException("Could not create directory circuit for path");
        }
        return circuit;
    }

    @Override
    public ExitCircuit openExitCircuitTo(List<Router> path) throws OpenFailedException {
        ExitCircuit circuit = CircuitImpl.createExitCircuitTo(this, path);
        if (!tryOpenCircuit(circuit, false, false)) {
            throw new OpenFailedException("Could not create exit circuit for path");
        }
        return circuit;
    }

    @Override
    public InternalCircuit openInternalCircuitTo(List<Router> path) throws OpenFailedException {
        InternalCircuit circuit = CircuitImpl.createInternalCircuitTo(this, path);
        if (!tryOpenCircuit(circuit, false, false)) {
            throw new OpenFailedException("Could not create internal circuit for path");
        }
        return circuit;
    }

    private boolean tryOpenCircuit(Circuit circuit, boolean isDirectory, boolean trackInitialization) {
        DirectoryCircuitResult result = new DirectoryCircuitResult();
        CircuitCreationRequest req = new CircuitCreationRequest(pathChooser, circuit, result, isDirectory);
        CircuitBuildTask task = new CircuitBuildTask(req, connectionCache, isNtorEnabled(), (trackInitialization) ? (initializationTracker) : (null));
        task.run();
        return result.isSuccessful();
    }
}