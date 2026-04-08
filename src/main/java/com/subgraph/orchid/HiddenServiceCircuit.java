package com.subgraph.orchid;

import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;

import java.util.concurrent.TimeoutException;


public interface HiddenServiceCircuit extends Circuit {
	Stream openStream(int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException;
}
