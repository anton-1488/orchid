package com.subgraph.orchid.socks;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.exceptions.SocksRequestException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public abstract class SocksRequest {
    private static final Logger log = LoggerFactory.getLogger(SocksRequest.class);

    protected final TorConfig config;
    protected final Socket socket;
    protected final SocketChannel channel;

    protected String hostname;
    protected int port;

    protected SocksRequest(TorConfig config, @NotNull Socket socket) {
        this.config = config;
        this.socket = socket;
        this.channel = socket.getChannel();
    }

    public abstract void readRequest() throws SocksRequestException;

    public abstract int getCommandCode();

    public abstract boolean isConnectRequest();

    public abstract void sendError(boolean isUnsupportedCommand) throws SocksRequestException;

    public abstract void sendSuccess() throws SocksRequestException;

    public abstract void sendConnectionRefused() throws SocksRequestException;

    public int getPort() {
        return port;
    }

    public boolean hasHostname() {
        return hostname != null;
    }

    public String getHostname() {
        return hostname;
    }

    public String getTarget() {
        return (hostname != null ? hostname : "[IP]") + ":" + port;
    }

    protected void setPortData(byte @NotNull [] data) throws SocksRequestException {
        if (data.length != 2) throw new SocksRequestException("Invalid port data");
        port = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
    }

    protected void setHostname(String name) {
        this.hostname = name;
    }

    // ByteBuffer-based I/O
    protected int readByte() throws SocksRequestException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        try {
            channel.read(buf);
            buf.flip();
            return buf.get() & 0xFF;
        } catch (Exception e) {
            throw new SocksRequestException("Failed to read byte", e);
        }
    }

    protected void readAll(byte[] buffer) throws SocksRequestException {
        readAll(buffer, 0, buffer.length);
    }

    protected void readAll(byte[] buffer, int offset, int length) throws SocksRequestException {
        ByteBuffer buf = ByteBuffer.wrap(buffer, offset, length);
        try {
            while (buf.hasRemaining()) {
                channel.read(buf);
            }
        } catch (Exception e) {
            throw new SocksRequestException("Failed to read data", e);
        }
    }

    protected void socketWrite(byte[] buffer) throws SocksRequestException {
        try {
            socket.getOutputStream().write(buffer);
        } catch (Exception e) {
            throw new SocksRequestException("Failed to write to socket", e);
        }
    }
}