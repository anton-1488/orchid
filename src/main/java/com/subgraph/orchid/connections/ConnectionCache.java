package com.subgraph.orchid.connections;


import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.exceptions.TorException;

public interface ConnectionCache extends AutoCloseable {
	/**
	 * Returns a completed connection to the specified router.  If an open connection 
	 * to the requested router already exists it is returned, otherwise a new connection
	 * is opened. 
	 * 
	 * @param router The router to which a connection is requested.
	 * @param isDirectoryConnection Is this going to be used as a directory connection.
	 * @return a completed connection to the specified router.
	 * @throws TorException if any error occurent.
	 */
	Connection getConnectionTo(Router router, boolean isDirectoryConnection) throws TorException;

	boolean isClosed();
}
