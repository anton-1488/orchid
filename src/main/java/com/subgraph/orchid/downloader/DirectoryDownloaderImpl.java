package com.subgraph.orchid.downloader;

import com.subgraph.orchid.Globals;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.circuits.CircuitManager;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.directory.DirectoryDownloader;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.document.ConsensusDocument.RequiredCertificate;
import com.subgraph.orchid.document.Descriptor;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.router.RouterMicrodescriptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class DirectoryDownloaderImpl implements DirectoryDownloader {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DirectoryDownloaderImpl.class);
    private final TorConfig config;
    private final TorInitializationTracker initializationTracker;

    private CircuitManager circuitManager;
    private boolean isStarted;
    private boolean isStopped;
    private static final ExecutorService loadExecutor = Globals.VIRTUAL_EXECUTOR;
    private DirectoryDownloadTask downloadTask;

    public DirectoryDownloaderImpl(TorConfig config, TorInitializationTracker initializationTracker) {
        this.config = config;
        this.initializationTracker = initializationTracker;
    }

    public void setCircuitManager(CircuitManager circuitManager) {
        this.circuitManager = circuitManager;
    }

    @Override
    public synchronized void start(Directory directory) {
        if (isStarted) {
            log.warn("Directory downloader already running");
            return;
        }
        if (circuitManager == null) {
            throw new IllegalStateException("Must set CircuitManager instance with setCircuitManager() before starting.");
        }

        downloadTask = new DirectoryDownloadTask(config, directory, this);
        loadExecutor.execute(downloadTask);
        isStarted = true;
    }

    @Override
    public synchronized void stop() {
        if (!isStarted || isStopped) {
            return;
        }
        downloadTask.stop();
    }

    @Override
    public RouterDescriptor downloadBridgeDescriptor(Router bridge) throws DirectoryRequestFailedException {
        DirectoryDocumentRequestor requestor = new DirectoryDocumentRequestor(initializationTracker);
        return requestor.downloadBridgeDescriptor(bridge);
    }

    @Override
    public ConsensusDocument downloadCurrentConsensus(boolean useMicrodescriptors) throws DirectoryRequestFailedException {
        return downloadCurrentConsensus(useMicrodescriptors, openCircuit());
    }

    @Override
    public ConsensusDocument downloadCurrentConsensus(boolean useMicrodescriptors, DirectoryCircuit circuit) throws DirectoryRequestFailedException {
        DirectoryDocumentRequestor requestor = new DirectoryDocumentRequestor(initializationTracker);
        return requestor.downloadCurrentConsensus(useMicrodescriptors);
    }

    @Override
    public List<KeyCertificate> downloadKeyCertificates(Set<RequiredCertificate> required) throws DirectoryRequestFailedException {
        return downloadKeyCertificates(required, openCircuit());
    }

    @Override
    public List<KeyCertificate> downloadKeyCertificates(Set<RequiredCertificate> required, DirectoryCircuit circuit) throws DirectoryRequestFailedException {
        final DirectoryDocumentRequestor requestor = new DirectoryDocumentRequestor(initializationTracker);
        return requestor.downloadKeyCertificates(required);
    }

    @Override
    public List<RouterDescriptor> downloadRouterDescriptors(Set<HexDigest> fingerprints) throws DirectoryRequestFailedException {
        return downloadRouterDescriptors(fingerprints, openCircuit());
    }

    @Override
    public List<RouterDescriptor> downloadRouterDescriptors(Set<HexDigest> fingerprints, DirectoryCircuit circuit) throws DirectoryRequestFailedException {
        DirectoryDocumentRequestor requestor = new DirectoryDocumentRequestor(initializationTracker);
        List<RouterDescriptor> ds = requestor.downloadRouterDescriptors(fingerprints);
        return removeUnrequestedDescriptors(fingerprints, ds);
    }

    @Override
    public List<RouterMicrodescriptor> downloadRouterMicrodescriptors(Set<HexDigest> fingerprints) throws DirectoryRequestFailedException {
        return downloadRouterMicrodescriptors(fingerprints, openCircuit());
    }

    @Override
    public List<RouterMicrodescriptor> downloadRouterMicrodescriptors(Set<HexDigest> fingerprints, DirectoryCircuit circuit) throws DirectoryRequestFailedException {
        DirectoryDocumentRequestor requestor = new DirectoryDocumentRequestor(initializationTracker);
        List<RouterMicrodescriptor> ds = requestor.downloadRouterMicrodescriptors(fingerprints);
        return removeUnrequestedDescriptors(fingerprints, ds);
    }

    private <T extends Descriptor> @NotNull List<T> removeUnrequestedDescriptors(Set<HexDigest> requested, @NotNull List<T> received) {
        List<T> result = new ArrayList<>();
        int unrequestedCount = 0;
        for (T d : received) {
            if (requested.contains(d.getDescriptorDigest())) {
                result.add(d);
            } else {
                unrequestedCount += 1;
            }
        }

        if (unrequestedCount > 0) {
            log.warn("Discarding {} received descriptor(s) with fingerprints that did not match requested descriptors", unrequestedCount);
        }
        return result;
    }

    private DirectoryCircuit openCircuit() throws DirectoryRequestFailedException {
        try {
            return circuitManager.openDirectoryCircuit();
        } catch (OpenFailedException e) {
            throw new DirectoryRequestFailedException("Failed to open directory circuit", e);
        }
    }

    private DirectoryCircuit openBridgeCircuit(Router bridge) throws DirectoryRequestFailedException {
        try {
            return circuitManager.openDirectoryCircuitTo(List.of(bridge));
        } catch (OpenFailedException e) {
            throw new DirectoryRequestFailedException("Failed to open directory circuit to bridge " + bridge, e);
        }
    }
}