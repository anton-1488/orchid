package com.subgraph.orchid.connections;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ConnectionCacheImpl implements ConnectionCache {
    private static final Logger log = LoggerFactory.getLogger(ConnectionCacheImpl.class);
    private final Map<Router, ConnectionImpl> activeConnections = new ConcurrentHashMap<>();
    private final Map<Router, CompletableFuture<ConnectionImpl>> pendingConnections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile boolean isClosed;
    private final TorConfig torConfig;
    private final TorInitializationTracker initializationTracker;

    public ConnectionCacheImpl(TorConfig config, TorInitializationTracker tracker) {
        this.torConfig = config;
        this.initializationTracker = tracker;

        scheduledExecutor.scheduleAtFixedRate(ConnectionTasksFactory.getCloseIdleConnectionCheckTask(activeConnections.values()), 5000, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public Connection getConnectionTo(Router router, boolean isDirectoryConnection) throws TorException {
        if (isClosed) {
            throw new IllegalStateException("ConnectionCache has been closed");
        }

        log.debug("Get connection to {}:{} ({})", router.getAddress(), router.getOnionPort(), router.getNickname());

        ConnectionImpl conn = activeConnections.get(router);
        if (conn != null && !conn.isClosed()) {
            return conn;
        }

        CompletableFuture<ConnectionImpl> future = pendingConnections.computeIfAbsent(router, r -> CompletableFuture.supplyAsync(() -> createConnection(r, isDirectoryConnection), virtualExecutor));

        try {
            ConnectionImpl newConn = future.get();
            pendingConnections.remove(router);
            activeConnections.put(router, newConn);
            return newConn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingConnections.remove(router);
            throw new TorException("Interrupted while connecting to " + router.getAddress(), e);
        } catch (ExecutionException e) {
            pendingConnections.remove(router);
            throw new TorException("Failed to connect to " + router.getAddress(), e.getCause());
        }
    }

    private @NotNull ConnectionImpl createConnection(Router router, boolean isDirectoryConnection) {
        try {
            log.debug("Creating connection to {}", router.getAddress());
            ConnectionImpl conn = new ConnectionImpl(torConfig, ConnectionSocketFactory.createSocket(), router, initializationTracker, isDirectoryConnection);
            conn.connect();
            return conn;
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    public List<Connection> getActiveConnections() {
        return List.copyOf(activeConnections.values());
    }

    @Override
    public void close() {
        if (isClosed) return;

        for (ConnectionImpl conn : activeConnections.values()) {
            try {
                conn.closeSocket();
            } catch (Exception e) {
                log.debug("Error closing connection", e);
            }
        }
        for (CompletableFuture<ConnectionImpl> future : pendingConnections.values()) {
            future.cancel(true);
        }

        activeConnections.clear();
        pendingConnections.clear();
        scheduledExecutor.close();

        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }
}