package com.subgraph.orchid.circuits.bridge;

import com.subgraph.orchid.directory.router.Router;
import com.subgraph.orchid.directory.router.RouterDescriptor;
import com.subgraph.orchid.data.HexDigest;

public interface BridgeRouter extends Router {
	void setIdentity(HexDigest identity);
	HexDigest getIdentity();

	void setDescriptor(RouterDescriptor descriptor);
	RouterDescriptor getDescriptor();
}