package com.subgraph.orchid;

import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;

import java.util.concurrent.TimeoutException;

public interface DirectoryCircuit extends Circuit {
	/**
	 * Open an anonymous connection to the directory service running on the
	 * final node in this circuit.
	 * 
	 * @param timeout in milliseconds
	 * @param autoclose if set to true, closing stream also marks this circuit for close
	 * 
	 * @return The status response returned by trying to open the stream.
	 */
	Stream openDirectoryStream(long timeout, boolean autoclose) throws InterruptedException, TimeoutException, StreamConnectFailedException;
}
