package com.subgraph.orchid.connections;

import com.subgraph.orchid.circuits.bridge.BridgeRouter;
import com.subgraph.orchid.circuits.cells.Cell;
import com.subgraph.orchid.circuits.cells.enums.AddressType;
import com.subgraph.orchid.circuits.cells.enums.CellCommand;
import com.subgraph.orchid.circuits.cells.impls.CellImpl;
import com.subgraph.orchid.circuits.cells.io.CellReader;
import com.subgraph.orchid.circuits.cells.io.CellWriter;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.directory.router.Router;
import com.subgraph.orchid.exceptions.ConnectionHandshakeException;
import com.subgraph.orchid.exceptions.ConnectionIOException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class ConnectionHandshake {
    private static final Logger log = LoggerFactory.getLogger(ConnectionHandshake.class);
    protected final ConnectionImpl connection;
    protected final SSLSocket socket;

    protected final List<Short> remoteVersions;
    private int remoteTimestamp;
    private InetAddress myAddress;
    private final List<InetAddress> remoteAddresses;

    public ConnectionHandshake(ConnectionImpl connection, SSLSocket socket) {
        this.connection = connection;
        this.socket = socket;
        this.remoteVersions = new ArrayList<>();
        this.remoteAddresses = new ArrayList<>();
    }

    public static ConnectionHandshake createHandshake(TorConfig config, ConnectionImpl connection, SSLSocket socket) {
        return new ConnectionHandshakeV3(connection, socket); // Temp impl
    }

    public abstract void runHandshake() throws ConnectionHandshakeException;

    public int getRemoteTimestamp() {
        return remoteTimestamp;
    }

    public InetAddress getMyAddress() {
        return myAddress;
    }

    protected Cell expectCell(CellCommand... expectedTypes) throws ConnectionHandshakeException {
        try {
            Cell c = connection.readConnectionControlCell();
            for (CellCommand type : expectedTypes) {
                if (c.getCommand() == type) {
                    return c;
                }
            }
            throw new ConnectionHandshakeException("Cell not found");
        } catch (ConnectionIOException e) {
            throw new ConnectionHandshakeException("Connection exception while performing handshake " + e);
        }
    }

    protected void sendVersions(short... versions) throws ConnectionIOException {
        Cell cell = new CellImpl(0, CellCommand.VERSIONS, versions.length * 2);
        CellWriter writer = cell.getCellWriter();
        for (short v : versions) {
            writer.putShort(v);
        }
        connection.sendCell(cell);
    }

    protected void receiveVersions() throws ConnectionHandshakeException {
        Cell cell = expectCell(CellCommand.VERSIONS);
        CellReader reader = cell.getCellReader();
        while (reader.remaining() >= 2) {
            remoteVersions.add(reader.getShort());
        }
    }

    protected void sendNetinfo() throws ConnectionIOException {
        Cell cell = new CellImpl(0, CellCommand.NETINFO);
        putTimestamp(cell);
        putIPv4Address(cell, connection.getRouter().getAddress());
        putMyAddresses(cell);
        connection.sendCell(cell);
    }

    private void putTimestamp(Cell cell) {
        cell.getCellWriter().putInt((int) Instant.now().getEpochSecond());
    }

    private void putIPv4Address(Cell cell, InetAddress address) {
        byte[] data = address.getAddress();
        CellWriter writer = cell.getCellWriter();
        writer.putByte(AddressType.IPv4.getByteValue());
        writer.putByte((byte) data.length);
        writer.putByteArray(data);
    }

    private void putMyAddresses(Cell cell) {
        try {
            cell.getCellWriter().putByte((byte) 1);
            putIPv4Address(cell, InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            log.error("Cann't find any host: ", e);
        }
    }

    protected void recvNetinfo() throws ConnectionHandshakeException {
        processNetInfo(expectCell(CellCommand.NETINFO));
    }

    protected void processNetInfo(Cell netinfoCell) {
        try {
            CellReader reader = netinfoCell.getCellReader();
            remoteTimestamp = reader.getInt();
            myAddress = readAddress(reader);

            int addressCount = reader.getByte();
            for (int i = 0; i < addressCount; i++) {
                remoteAddresses.add(readAddress(reader));
            }
        } catch (Exception e) {
            throw new ConnectionHandshakeException(e);
        }
    }

    private @NotNull InetAddress readAddress(CellReader cell) throws UnknownHostException {
        AddressType type = AddressType.fromValue(cell.getByte());
        int len = cell.getByte();

        if (type == AddressType.IPv4 && len == 4) {
            byte[] address = new byte[4];
            cell.getByteArray(address);
            return InetAddress.getByAddress(address);
        } else if (type == AddressType.IPv6 && len == 16) {
            byte[] address = new byte[16];
            cell.getByteArray(address);
            return InetAddress.getByAddress(address);
        }

        // skip unknown address types
        byte[] buffer = new byte[len];
        cell.getByteArray(buffer);
        log.warn("Skipping unknown address type: {} (length: {})", type, len);

        throw new UnknownHostException("Unsupported address type: " + type);
    }

    protected void verifyIdentityKey(PublicKey publicKey) throws ConnectionHandshakeException {
        if (!(publicKey instanceof RSAPublicKey)) {
            throw new ConnectionHandshakeException("Identity certificate public key is not an RSA key as expected");
        }

        TorPublicKey identityKey = new TorPublicKey((RSAPublicKey) publicKey);
        Router router = connection.getRouter();
        if ((router instanceof BridgeRouter bridge) && (router.getIdentityHash() == null)) {
            log.info("Setting Bridge fingerprint from connection handshake for {}", router);
            bridge.setIdentity(identityKey.getFingerprint());
        } else if (!identityKey.getFingerprint().equals(router.getIdentityHash())) {
            throw new ConnectionHandshakeException("Router identity does not match certificate key");
        }
    }
}