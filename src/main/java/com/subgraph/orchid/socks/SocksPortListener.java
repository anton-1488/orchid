package com.subgraph.orchid.socks;

public interface SocksPortListener {
	void addListeningPort(int port);
	void stop();
}
