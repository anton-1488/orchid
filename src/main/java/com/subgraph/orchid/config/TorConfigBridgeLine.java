package com.subgraph.orchid.config;

import com.subgraph.orchid.data.HexDigest;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public record TorConfigBridgeLine(InetAddress address, int port, HexDigest fingerprint) {
    @Override
    @NotNull
    public String toString() {
        return String.format("[TOR_CONFIG] - BridgeLine: address: %s, port: %d, fingeprint: %s", address, port, fingerprint.toBase32());
    }
}