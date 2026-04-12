package com.subgraph.orchid.sockets.sslengine;

@FunctionalInterface
public interface HandshakeCallbackHandler {
	void handshakeCompleted();
}