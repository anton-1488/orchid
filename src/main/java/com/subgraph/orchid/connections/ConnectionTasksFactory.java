package com.subgraph.orchid.connections;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.util.Collection;
import java.util.concurrent.Callable;

public final class ConnectionTasksFactory {
    private static final Logger log = LoggerFactory.getLogger(ConnectionTasksFactory.class);

    private ConnectionTasksFactory() {
        throw new UnsupportedOperationException();
    }

    @Contract(pure = true)
    public static @NotNull Callable<ConnectionImpl> getConnectionTask(TorConfig config, Router router, TorInitializationTracker initializationTracker, boolean isDirectoryConnection) {
        return () -> {
            SSLSocket socket = ConnectionSocketFactory.createSocket();
            ConnectionImpl conn = new ConnectionImpl(config, socket, router, initializationTracker, isDirectoryConnection);
            conn.connect();
            return conn;
        };
    }

    @Contract(pure = true)
    public static @NotNull Runnable getCloseIdleConnectionCheckTask(Collection<ConnectionImpl> activeConnections) {
        return () -> {
            for (ConnectionImpl connection : activeConnections) {
                connection.closeSocket();
            }
        };
    }
}