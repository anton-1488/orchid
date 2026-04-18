package com.subgraph.orchid.sockets.sslengine;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class SSLEngineManager {
    private static final Logger log = LoggerFactory.getLogger(SSLEngineManager.class);

    private final SSLEngine engine;
    private final InputStream input;
    private final OutputStream output;
    private final HandshakeCallbackHandler handshakeCallback;

    private final ByteBuffer peerApplicationBuffer;
    private final ByteBuffer peerNetworkBuffer;
    private final ByteBuffer myApplicationBuffer;
    private final ByteBuffer myNetworkBuffer;

    private boolean handshakeStarted = false;
    private volatile boolean closed = false;

    SSLEngineManager(@NotNull SSLEngine engine, HandshakeCallbackHandler handshakeCallback, InputStream input, OutputStream output) {
        this.engine = engine;
        this.handshakeCallback = handshakeCallback;
        this.input = input;
        this.output = output;

        SSLSession session = engine.getSession();
        this.peerApplicationBuffer = createApplicationBuffer(session);
        this.peerNetworkBuffer = createPacketBuffer(session);
        this.myApplicationBuffer = createApplicationBuffer(session);
        this.myNetworkBuffer = createPacketBuffer(session);
    }

    @Contract("_ -> new")
    private static @NotNull ByteBuffer createApplicationBuffer(@NotNull SSLSession session) {
        return ByteBuffer.allocate(session.getApplicationBufferSize());
    }

    @Contract("_ -> new")
    private static @NotNull ByteBuffer createPacketBuffer(@NotNull SSLSession session) {
        return ByteBuffer.allocate(session.getPacketBufferSize());
    }

    public void startHandshake() throws IOException {
        log.debug("startHandshake()");
        handshakeStarted = true;
        engine.beginHandshake();
        runHandshake();
    }

    public ByteBuffer getSendBuffer() {
        return myApplicationBuffer;
    }

    public ByteBuffer getRecvBuffer() {
        return peerApplicationBuffer;
    }

    public synchronized int write() throws IOException {
        log.debug("write()");
        if (closed) return -1;

        if (!handshakeStarted) {
            startHandshake();
        }

        int pos = myApplicationBuffer.position();
        if (pos == 0) {
            return 0;
        }

        myNetworkBuffer.clear();
        myApplicationBuffer.flip();
        SSLEngineResult result = engine.wrap(myApplicationBuffer, myNetworkBuffer);
        myApplicationBuffer.compact();

        log.debug("wrap: {}", result.getStatus());

        switch (result.getStatus()) {
            case BUFFER_OVERFLOW:
                throw new BufferOverflowException();
            case BUFFER_UNDERFLOW:
                throw new BufferUnderflowException();
            case CLOSED:
                throw new SSLException("SSLEngine is closed");
            default:
                break;
        }

        flush();
        if (runHandshake()) {
            write();
        }

        return pos - myApplicationBuffer.position();
    }

    public synchronized int read() throws IOException {
        log.debug("read()");
        if (closed) return -1;

        if (!handshakeStarted) {
            startHandshake();
        }

        if (engine.isInboundDone()) {
            return -1;
        }

        if (peerNetworkBuffer.position() == 0) {
            int n = networkReadBuffer(peerNetworkBuffer);
            if (n < 0) {
                return -1;
            }
            if (n == 0) {
                return 0;
            }
        }

        int pos = peerApplicationBuffer.position();
        peerNetworkBuffer.flip();
        SSLEngineResult result = engine.unwrap(peerNetworkBuffer, peerApplicationBuffer);
        peerNetworkBuffer.compact();

        log.debug("unwrap: {}", result.getStatus());

        switch (result.getStatus()) {
            case BUFFER_OVERFLOW:
                throw new BufferOverflowException();
            case BUFFER_UNDERFLOW:
                return 0; // нужно больше данных
            case CLOSED:
                engine.closeInbound();
                return -1;
            case OK:
                break;
        }

        runHandshake();

        int read = peerApplicationBuffer.position() - pos;
        return read == 0 ? -1 : read;
    }

    public synchronized void close() throws IOException {
        if (closed) return;
        closed = true;

        try {
            if (!engine.isOutboundDone()) {
                engine.closeOutbound();
                runHandshake();
            }
            flush();
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                log.debug("Error closing output", e);
            }
            try {
                input.close();
            } catch (IOException e) {
                log.debug("Error closing input", e);
            }
        }
    }

    public synchronized void flush() throws IOException {
        if (myNetworkBuffer.position() == 0) return;

        myNetworkBuffer.flip();
        networkWriteBuffer(myNetworkBuffer);
        myNetworkBuffer.compact();
    }

    private boolean runHandshake() throws IOException {
        boolean handshakeRan = false;
        while (true) {
            if (!processHandshake()) {
                return handshakeRan;
            }
            handshakeRan = true;
        }
    }

    private boolean processHandshake() throws IOException {
        HandshakeStatus hs = engine.getHandshakeStatus();
        log.debug("processHandshake: {}", hs);

        return switch (hs) {
            case NEED_TASK -> {
                runDelegatedTasks();
                yield true;
            }
            case NEED_UNWRAP -> handshakeUnwrap();
            case NEED_WRAP -> handshakeWrap();
            case FINISHED -> {
                handshakeFinished();
                yield false;
            }
            default -> false;
        };
    }

    private void runDelegatedTasks() {
        log.debug("runDelegatedTasks()");
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            log.debug("Running delegated task");
            task.run();
        }
    }

    private boolean handshakeUnwrap() throws IOException {
        log.debug("handshakeUnwrap()");

        if (peerNetworkBuffer.position() == 0) {
            if (networkReadBuffer(peerNetworkBuffer) < 0) {
                return false;
            }
        }

        peerNetworkBuffer.flip();
        SSLEngineResult result = engine.unwrap(peerNetworkBuffer, peerApplicationBuffer);
        peerNetworkBuffer.compact();

        log.debug("unwrap result: {}", result.getStatus());

        if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            handshakeFinished();
        }

        return switch (result.getStatus()) {
            case OK, BUFFER_UNDERFLOW -> true;
            default -> false;
        };
    }

    private boolean handshakeWrap() throws IOException {
        log.debug("handshakeWrap()");

        myApplicationBuffer.flip();
        SSLEngineResult result = engine.wrap(myApplicationBuffer, myNetworkBuffer);
        myApplicationBuffer.compact();
        log.debug("wrap result: {}", result.getStatus());

        if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            handshakeFinished();
        }

        flush();
        return result.getStatus() != Status.CLOSED;
    }

    private void handshakeFinished() {
        log.debug("Handshake finished");
        if (handshakeCallback != null) {
            handshakeCallback.handshakeCompleted();
        }
    }

    private void networkWriteBuffer(@NotNull ByteBuffer buffer) throws IOException {
        byte[] data = buffer.array();
        int offset = buffer.position();
        int length = buffer.remaining();

        output.write(data, offset, length);
        output.flush();
        buffer.position(buffer.limit());
    }

    private int networkReadBuffer(@NotNull ByteBuffer buffer) throws IOException {
        byte[] data = buffer.array();
        int offset = buffer.position();
        int available = buffer.remaining();

        int read = input.read(data, offset, available);
        if (read > 0) {
            buffer.position(offset + read);
        }
        return read;
    }
}