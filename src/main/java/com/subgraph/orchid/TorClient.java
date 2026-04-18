package com.subgraph.orchid;

import com.subgraph.orchid.circuits.CircuitManager;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.connections.ConnectionCache;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryStore;
import com.subgraph.orchid.downloader.DirectoryDownloaderImpl;
import com.subgraph.orchid.events.TorInitializationListener;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.sockets.OrchidSocketFactory;
import com.subgraph.orchid.socks.SocksPortListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.net.SocketFactory;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class is the main entry-point for running a Tor proxy
 * or client.
 */
public class TorClient {
    private static final Logger log = LoggerFactory.getLogger(TorClient.class);
    private final TorConfig config;
    private final Directory directory;
    private final TorInitializationTracker initializationTracker;
    private final ConnectionCache connectionCache;
    private final CircuitManager circuitManager;
    private final SocksPortListener socksListener;
    private final DirectoryDownloaderImpl directoryDownloader;

    private boolean isStarted = false;
    private boolean isStopped = false;

    private final CountDownLatch readyLatch;

    public TorClient() {
        this(null);
    }

    public TorClient(DirectoryStore customDirectoryStore) {
        config = TorConfig.builder().build();
        directory = Tor.createDirectory(config, customDirectoryStore);
        initializationTracker = new TorInitializationTracker();
        initializationTracker.addListener(createReadyFlagInitializationListener());
        connectionCache = Tor.createConnectionCache(config, initializationTracker);
        directoryDownloader = Tor.createDirectoryDownloader(config, initializationTracker);
        circuitManager = Tor.createCircuitManager(config, directoryDownloader, directory, connectionCache, initializationTracker);
        socksListener = Tor.createSocksPortListener(config, circuitManager);
        readyLatch = new CountDownLatch(1);
    }

    public TorConfig getConfig() {
        return config;
    }

    public SocketFactory getSocketFactory() {
        return new OrchidSocketFactory(this);
    }

    /**
     * Start running the Tor client service.
     */
    public synchronized void start() {
        if (isStarted) {
            return;
        }
        if (isStopped) {
            throw new IllegalStateException("Cannot restart a TorClient instance.  Create a new instance instead.");
        }
        log.info("Starting {} (version: {})", Tor.TOR_IMPLEMENTATION, Tor.TOR_VERSION);
        verifyUnlimitedStrengthPolicyInstalled();
        directoryDownloader.start(directory);
        circuitManager.startBuildingCircuits();
        enableSocksListener();
        isStarted = true;
    }

    public synchronized void stop() {
        if (!isStarted || isStopped) {
            return;
        }
        try {
            socksListener.stop();
            directoryDownloader.stop();
            circuitManager.stopBuildingCircuits(true);
            directory.close();
            connectionCache.close();
        } catch (Exception e) {
            log.error("Unexpected exception while shutting down TorClient instance: ", e);
        } finally {
            isStopped = true;
        }
    }

    public Directory getDirectory() {
        return directory;
    }

    public ConnectionCache getConnectionCache() {
        return connectionCache;
    }

    public CircuitManager getCircuitManager() {
        return circuitManager;
    }

    public void waitUntilReady() throws InterruptedException {
        readyLatch.await();
    }

    public void waitUntilReady(long timeout) throws InterruptedException, TimeoutException {
        if (!readyLatch.await(timeout, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }

    public Stream openExitStreamTo(String hostname, int port) throws InterruptedException, TimeoutException, OpenFailedException {
        ensureStarted();
        return circuitManager.openExitStreamTo(hostname, port);
    }

    private void ensureStarted() {
        if (!isStarted) {
            throw new IllegalStateException("Must call start() first");
        }
    }

    public void enableSocksListener(int port) {
        socksListener.addListeningPort(port);
    }

    public void enableSocksListener() {
        enableSocksListener(9150);
    }

    public void addInitializationListener(TorInitializationListener listener) {
        initializationTracker.addListener(listener);
    }

    public void removeInitializationListener(TorInitializationListener listener) {
        initializationTracker.removeListener(listener);
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull TorInitializationListener createReadyFlagInitializationListener() {
        return (status) -> {
        };
    }

    @Contract(value = " -> new", pure = true)
    private static @NotNull TorInitializationListener createInitalizationListner() {
        return (status) -> System.out.printf("\r>>> Init Tor[ %s ]", status.getPercent());
    }

    private void verifyUnlimitedStrengthPolicyInstalled() {
        try {
            if (Cipher.getMaxAllowedKeyLength("AES") < 256) {
                String message = "Unlimited Strength Jurisdiction Policy Files are required but not installed.";
                log.error(message);
                throw new TorException(message);
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("No AES provider found");
            throw new TorException(e);
        }
    }
}