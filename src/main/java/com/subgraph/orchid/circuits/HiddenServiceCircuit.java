package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;

import java.util.concurrent.TimeoutException;

public interface HiddenServiceCircuit extends Circuit {
    Stream openStream(int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException;
}