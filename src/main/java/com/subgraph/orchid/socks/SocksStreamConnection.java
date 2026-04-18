package com.subgraph.orchid.socks;

import com.subgraph.orchid.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class SocksStreamConnection {
    private static final Logger log = LoggerFactory.getLogger(SocksStreamConnection.class);
    private static final int BUFFER_SIZE = 8192;

    private final Socket socket;
    private final Stream stream;
    private final InputStream torInput;
    private final OutputStream torOutput;
    private final CountDownLatch doneLatch = new CountDownLatch(2);

    private SocksStreamConnection(Socket socket, @NotNull Stream stream) {
        this.socket = socket;
        this.stream = stream;
        this.torInput = stream.getInputStream();
        this.torOutput = stream.getOutputStream();
    }

    public static void runConnection(Socket socket, Stream stream) {
        new SocksStreamConnection(socket, stream).run();
    }

    private void run() {
        log.debug("Starting bidirectional transfer for stream {}", stream);

        Thread.startVirtualThread(this::transferTorToSocket);
        Thread.startVirtualThread(this::transferSocketToTor);

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Transfer interrupted");
        } finally {
            closeAll();
        }
    }

    private void transferTorToSocket() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (true) {
                int n = torInput.read(buffer);
                if (n == -1) {
                    log.debug("EOF from Tor stream {}", stream);
                    socket.shutdownOutput();
                    break;
                }
                if (n > 0) {
                    log.trace("Transferring {} bytes from Tor to SOCKS", n);
                    socket.getOutputStream().write(buffer, 0, n);
                    socket.getOutputStream().flush();
                }
            }
        } catch (IOException e) {
            log.debug("Tor -> SOCKS transfer error: {}", e.getMessage());
        } finally {
            doneLatch.countDown();
        }
    }

    private void transferSocketToTor() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (true) {
                stream.waitForSendWindow();
                int n = socket.getInputStream().read(buffer);
                if (n == -1) {
                    log.debug("EOF from SOCKS socket");
                    torOutput.close();
                    break;
                }
                if (n > 0) {
                    log.trace("Transferring {} bytes from SOCKS to Tor", n);
                    torOutput.write(buffer, 0, n);
                    torOutput.flush();
                }
            }
        } catch (IOException e) {
            log.debug("SOCKS -> Tor transfer error: {}", e.getMessage());
        } finally {
            doneLatch.countDown();
        }
    }

    private void closeAll() {
        try {
            socket.close();
        } catch (IOException e) {
            log.debug("Error closing SOCKS socket: {}", e.getMessage());
        }
        try {
            torInput.close();
        } catch (IOException e) {
            log.debug("Error closing Tor input stream: {}", e.getMessage());
        }
        try {
            torOutput.close();
        } catch (IOException e) {
            log.debug("Error closing Tor output stream: {}", e.getMessage());
        }
    }
}