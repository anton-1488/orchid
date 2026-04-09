package com.subgraph.orchid.config;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.IPv4Address;
import org.jetbrains.annotations.NotNull;

public record TorConfigBridgeLine(IPv4Address address, int port, HexDigest fingerprint) {
	@Override
	@NotNull
	public String toString() {
		return String.format("[TOR_CONFIG] - BridgeLine: address: %s, port: %d, fingeprint: %s", address, port, fingerprint.toBase32());
	}
}