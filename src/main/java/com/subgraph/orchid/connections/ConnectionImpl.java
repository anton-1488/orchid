package com.subgraph.orchid.connections;

import com.subgraph.orchid.BootstrapStatus;
import com.subgraph.orchid.Globals;
import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.circuits.TorInitializationTracker;
import com.subgraph.orchid.circuits.cells.Cell;
import com.subgraph.orchid.circuits.cells.enums.CellCommand;
import com.subgraph.orchid.circuits.cells.impls.CellImpl;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.directory.router.Router;
import com.subgraph.orchid.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class represents a transport link between two onion routers or
 * between an onion proxy and an entry router.
 *
 */
public class ConnectionImpl implements Connection {
    private final static int CONNECTION_IDLE_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    private final static int DEFAULT_CONNECT_TIMEOUT = 5000;
    private final static Cell CLOSED_SENTINEL = new CellImpl(0, CellCommand.PADDING);
    private static final Logger log = LoggerFactory.getLogger(ConnectionImpl.class);
    private final ExecutorService executor = Globals.VIRTUAL_EXECUTOR;

    private final TorConfig config;
    private final SSLSocket socket;
    private InputStream input;
    private OutputStream output;
    private final Router router;
    private final Map<Integer, Circuit> circuitMap = new ConcurrentHashMap<>();
    private final BlockingQueue<Cell> connectionControlCells = new LinkedBlockingQueue<>();
    private final TorInitializationTracker initializationTracker;
    private final boolean isDirectoryConnection;

    private int currentId = 1;
    private boolean isConnected;
    private volatile boolean isClosed;
    private final AtomicLong lastActivity = new AtomicLong();

    public ConnectionImpl(TorConfig config, SSLSocket socket, Router router, TorInitializationTracker tracker, boolean isDirectoryConnection) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(socket);
        Objects.requireNonNull(router);
        Objects.requireNonNull(tracker);

        this.config = config;
        this.socket = socket;
        this.router = router;
        this.initializationTracker = tracker;
        this.isDirectoryConnection = isDirectoryConnection;

        initializeCurrentCircuitId();
    }

    private void initializeCurrentCircuitId() {
        currentId = TorRandom.nextInt(0xFFFF) + 1;
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public synchronized int bindCircuit(Circuit circuit) {
        while (circuitMap.containsKey(currentId)) {
            incrementNextId();
        }
        int id = currentId;
        incrementNextId();
        circuitMap.put(id, circuit);
        return id;
    }

    private synchronized void incrementNextId() {
        currentId++;
        if (currentId > 0xFFFF) {
            currentId = 1;
        }
    }

    public synchronized void connect() throws ConnectionFailedException, ConnectionTimeoutException, ConnectionHandshakeException {
        if (isConnected) {
            return;
        }

        try {
            doConnect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionHandshakeException("Handshake interrupted");
        } catch (Exception e) {
            throw new ConnectionFailedException(e);
        }
        isConnected = true;
    }

    private void doConnect() throws IOException, InterruptedException, ConnectionIOException {
        connectSocket();
        ConnectionHandshake handshake = ConnectionHandshake.createHandshake(config, this, socket);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        executor.execute(createReadCellsRunnable());
        handshake.runHandshake();
        updateLastActivity();
    }

    private void connectSocket() throws IOException {
        if (isDirectoryConnection) {
            initializationTracker.notifyEvent(BootstrapStatus.CONN_DIR);
        } else {
            initializationTracker.notifyEvent(BootstrapStatus.CONN_OR);
        }

        socket.connect(routerToSocketAddress(router), DEFAULT_CONNECT_TIMEOUT);

        if (isDirectoryConnection) {
            initializationTracker.notifyEvent(BootstrapStatus.HANDSHAKE_DIR);
        } else {
            initializationTracker.notifyEvent(BootstrapStatus.HANDSHAKE_OR);
        }
    }

    private SocketAddress routerToSocketAddress(Router router) {
        InetAddress address = router.getAddress();
        return new InetSocketAddress(address, router.getOnionPort());
    }

    public synchronized void sendCell(Cell cell) throws ConnectionIOException {
        if (!socket.isConnected()) {
            throw new ConnectionIOException("Cannot send cell because connection is not connected");
        }

        updateLastActivity();
        try {
            output.write(cell.getCellBytes());
        } catch (IOException e) {
            log.error("IOException writing cell to connection ", e);
            closeSocket();
            throw new ConnectionIOException(e.getClass().getName() + " : " + e.getMessage());
        }
    }

    private Cell recvCell() throws ConnectionIOException {
        try {
            return new CellImpl(input.readAllBytes());
        } catch (EOFException e) {
            closeSocket();
            throw new ConnectionIOException();
        } catch (IOException e) {
            if (!isClosed) {
                log.error("IOException reading cell from this connection. ", e);
                closeSocket();
            }
            throw new ConnectionIOException(e.getClass().getName() + " " + e.getMessage());
        }
    }

    public synchronized void closeSocket() {
        try {
            log.debug("Closing connection to this");
            isClosed = true;
            socket.close();
            isConnected = false;
        } catch (IOException e) {
            log.error("Error closing socket: ", e);
        }
    }

    private Runnable createReadCellsRunnable() {
        return () -> {
            try {
                readCellsLoop();
            } catch (Exception e) {
                log.error("Unhandled exception processing incoming cells on connection: ", e);
            }
        };
    }

    private void readCellsLoop() {
        while (!Thread.interrupted()) {
            try {
                processCell(recvCell());
            } catch (ConnectionIOException e) {
                connectionControlCells.add(CLOSED_SENTINEL);
                return;
            } catch (TorException e) {
                log.error("Unhandled Tor exception reading and processing cells: ", e);
            }
        }
    }

    public Cell readConnectionControlCell() throws ConnectionIOException {
        try {
            return connectionControlCells.take();
        } catch (InterruptedException e) {
            closeSocket();
            throw new ConnectionIOException();
        }
    }

    private void processCell(Cell cell) {
        updateLastActivity();
        CellCommand command = cell.getCommand();

        if (command == CellCommand.RELAY) {
            CellProcessor.processRelayCell(cell, circuitMap);
            return;
        }

        switch (command) {
            case NETINFO, VERSIONS, CERTS, AUTH_CHALLENGE:
                connectionControlCells.add(cell);
                break;
            case CREATED, CREATED_FAST, DESTROY:
                CellProcessor.processControlCell(cell, circuitMap);
                break;
        }
    }

    public void idleCloseCheck() {
        boolean needClose = (!isClosed && circuitMap.isEmpty() && getIdleMilliseconds() > CONNECTION_IDLE_TIMEOUT);
        if (needClose) {
            log.debug("Closing this connection on idle timeout");
            closeSocket();
        }
    }

    private void updateLastActivity() {
        lastActivity.set(System.currentTimeMillis());
    }

    private long getIdleMilliseconds() {
        if (lastActivity.get() == 0) {
            return 0;
        }
        return System.currentTimeMillis() - lastActivity.get();
    }

    public void removeCircuit(Circuit circuit) {
        circuitMap.remove(circuit.getCircuitId());
    }

    public String toString() {
        return "!" + router.getNickname() + "!";
    }
}