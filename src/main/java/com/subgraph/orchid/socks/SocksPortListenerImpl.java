package com.subgraph.orchid.socks;

import com.subgraph.orchid.CircuitManager;
import com.subgraph.orchid.Globals;
import com.subgraph.orchid.SocksPortListener;
import com.subgraph.orchid.config.TorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class SocksPortListenerImpl implements SocksPortListener {
    private static final Logger log = LoggerFactory.getLogger(SocksPortListenerImpl.class);

    private final TorConfig config;
    private final CircuitManager circuitManager;
    private final ExecutorService executor = Globals.VIRTUAL_EXECUTOR;
    private final ConcurrentHashMap<Integer, AcceptTask> acceptTasks = new ConcurrentHashMap<>();
    private volatile boolean stopped;

    public SocksPortListenerImpl(TorConfig config, CircuitManager circuitManager) {
        this.config = config;
        this.circuitManager = circuitManager;
    }

    @Override
    public void addListeningPort(int port) {
        if (stopped) {
            throw new IllegalStateException("SOCKS proxy has been stopped");
        }
        if (acceptTasks.containsKey(port)) {
            return;
        }

        try {
            AcceptTask task = new AcceptTask(port);
            acceptTasks.put(port, task);
            executor.execute(task);
            log.info("Listening for SOCKS connections on port {}", port);
        } catch (IOException e) {
            acceptTasks.remove(port);
            throw new RuntimeException("Failed to listen on port " + port, e);
        }
    }

    @Override
    public void stop() {
        stopped = true;
        for (AcceptTask task : acceptTasks.values()) {
            task.stop();
        }
        acceptTasks.clear();
        executor.shutdown();
    }

    private class AcceptTask implements Runnable {
        private final ServerSocket serverSocket;
        private final int port;
        private volatile boolean stopped;

        AcceptTask(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.port = port;
        }

        void stop() {
            stopped = true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Error closing server socket on port {}", port, e);
            }
        }

        @Override
        public void run() {
            log.debug("Accepting SOCKS connections on port {}", port);
            try {
                while (!stopped && !Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    executor.execute(new SocksClientTask(config, socket, circuitManager));
                }
            } catch (IOException e) {
                if (!stopped) {
                    log.warn("Error accepting SOCKS connection on port {}: {}", port, e.getMessage());
                }
            } finally {
                acceptTasks.remove(port);
            }
        }
    }
}