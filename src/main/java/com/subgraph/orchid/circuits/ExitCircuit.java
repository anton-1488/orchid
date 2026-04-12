package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;

import java.net.InetAddress;
import java.util.concurrent.TimeoutException;

public interface ExitCircuit extends Circuit {

    /**
     * Open an exit stream from the final node in this circuit to the
     * specified target address and port.
     *
     * @param address The network address of the exit target.
     * @param port    The port of the exit target.
     * @return The status response returned by trying to open the stream.
     */
    Stream openExitStream(InetAddress address, int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException;

    /**
     * Open an exit stream from the final node in this circuit to the
     * specified target hostname and port.
     *
     * @param hostname The network hostname of the exit target.
     * @param port     The port of the exit target.
     * @return The status response returned by trying to open the stream.
     */
    Stream openExitStream(String hostname, int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException;

    boolean canHandleExitToPort(int port);

}
