package com.subgraph.orchid.socks;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.circuits.CircuitManager;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.exceptions.SocksRequestException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

public class SocksClientTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SocksClientTask.class);

    private final TorConfig config;
    private final Socket socket;
    private final CircuitManager circuitManager;

    SocksClientTask(TorConfig config, Socket socket, CircuitManager circuitManager) {
        this.config = config;
        this.socket = socket;
        this.circuitManager = circuitManager;
    }

    @Override
    public void run() {
        try {
            int version = socket.getInputStream().read();
            if (version == 5) {
                processRequest(new Socks5Request(config, socket));
            } else {
                log.warn("Unsupported SOCKS version: {}", version);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("IO error reading SOCKS request: {}", e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void processRequest(@NotNull SocksRequest request) {
        try {
            request.readRequest();

            if (!request.isConnectRequest()) {
                log.warn("Non-connect command: {}", request.getCommandCode());
                request.sendError(true);
                return;
            }

            try {
                Stream stream = openConnectStream(request);
                log.debug("SOCKS CONNECT to {} completed", request.getTarget());
                request.sendSuccess();
                SocksStreamConnection.runConnection(socket, stream);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("SOCKS CONNECT to {} interrupted", request.getTarget());
                request.sendError(false);
            } catch (TimeoutException e) {
                log.info("SOCKS CONNECT to {} timed out", request.getTarget());
                request.sendError(false);
            } catch (OpenFailedException e) {
                log.info("SOCKS CONNECT to {} failed: {}", request.getTarget(), e.getMessage());
                request.sendConnectionRefused();
            }

        } catch (SocksRequestException e) {
            log.warn("Failure reading SOCKS request: {}", e.getMessage());
            request.sendError(false);
        }
    }

    private Stream openConnectStream(@NotNull SocksRequest request) throws InterruptedException, TimeoutException, OpenFailedException, SocksRequestException {
        if (!request.hasHostname()) {
            throw new SocksRequestException("No hostname in SOCKS request");
        }
        log.debug("CONNECT to {}:{}", request.getHostname(), request.getPort());
        return circuitManager.openExitStreamTo(request.getHostname(), request.getPort());
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            log.warn("Error closing SOCKS socket: {}", e.getMessage());
        }
    }
}