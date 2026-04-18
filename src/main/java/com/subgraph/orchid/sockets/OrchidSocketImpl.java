package com.subgraph.orchid.sockets;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.TorClient;
import org.jetbrains.annotations.NotNull;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class OrchidSocketImpl extends SocketImpl {
    private final TorClient torClient;
    private Stream stream;

    OrchidSocketImpl(TorClient torClient) {
        this.torClient = torClient;
        this.fd = new FileDescriptor();
    }

    @Override
    public void setOption(int optID, Object value) {
        // Ignored.
    }

    @Override
    public Object getOption(int optID) {
        if (optID == SocketOptions.SO_LINGER) {
            return 0;
        } else if (optID == SocketOptions.TCP_NODELAY) {
            return Boolean.TRUE;
        } else if (optID == SocketOptions.SO_TIMEOUT) {
            return 0;
        } else {
            return 0;
        }
    }

    @Override
    protected void create(boolean stream) {

    }

    @Override
    protected void connect(String host, int port) throws IOException {
        SocketAddress endpoint = InetSocketAddress.createUnresolved(host, port);
        connect(endpoint, 0);
    }

    @Override
    protected void connect(@NotNull InetAddress address, int port) throws IOException {
        SocketAddress endpoint = InetSocketAddress.createUnresolved(address.getHostAddress(), port);
        connect(endpoint, 0);
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        if (!(address instanceof InetSocketAddress inetAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        doConnect(addressToName(inetAddress), inetAddress.getPort());
    }

    private String addressToName(@NotNull InetSocketAddress address) {
        if (address.getAddress() != null) {
            return address.getAddress().getHostAddress();
        } else {
            return address.getHostName();
        }
    }

    private synchronized void doConnect(String host, int port) throws IOException {
        if (stream != null) {
            throw new SocketException("Already connected");
        }

        try {
            stream = torClient.openExitStreamTo(host, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SocketException("connect() interrupted");
        } catch (Exception e) {
            throw new SocketException(e);
        }
    }

    @Override
    protected void bind(InetAddress host, int port) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void listen(int backlog) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void accept(SocketImpl s) {
        throw new UnsupportedOperationException();
    }

    private synchronized Stream getStream() throws IOException {
        if (stream == null) {
            throw new IOException("Not connected");
        }
        return stream;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return getStream().getInputStream();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return getStream().getOutputStream();
    }

    @Override
    protected int available() throws IOException {
        return getStream().getInputStream().available();
    }

    @Override
    protected synchronized void close() throws IOException {
        stream.close();
        stream = null;
    }

    @Override
    protected void sendUrgentData(int data) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void shutdownInput() {
    }

    @Override
    protected void shutdownOutput() {
    }
}