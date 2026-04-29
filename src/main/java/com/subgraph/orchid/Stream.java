package com.subgraph.orchid;

import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.circuits.CircuitNode;

import java.io.InputStream;
import java.io.OutputStream;

public interface Stream extends AutoCloseable {
    /**
     * Returns the {@link Circuit} this stream belongs to.
     *
     * @return The {@link Circuit} this stream belongs to.
     */
    Circuit getCircuit();

    /**
     * Returns the stream id value of this stream.
     *
     * @return The stream id value of this stream.
     */
    int getStreamId();

    /**
     * Retruns the target node
     *
     * @return target node
     */
    CircuitNode getTargetNode();

    /**
     * Returns an {@link InputStream} for receiving data from this stream.
     *
     * @return An {@link InputStream} for treceiving data from this stream.
     */
    InputStream getInputStream();

    /**
     * Returns an {@link OutputStream} for sending data on this stream.
     *
     * @return An {@link OutputStream} for sending data from on stream.
     */
    OutputStream getOutputStream();
}