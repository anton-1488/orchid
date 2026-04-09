package com.subgraph.orchid.circuits.bridge;

import com.subgraph.orchid.routers.Router;
import com.subgraph.orchid.routers.RouterDescriptor;
import com.subgraph.orchid.data.HexDigest;

public interface BridgeRouter extends Router {
	void setIdentity(HexDigest identity);
	HexDigest getIdentity();

	void setDescriptor(RouterDescriptor descriptor);
	RouterDescriptor getDescriptor();
}