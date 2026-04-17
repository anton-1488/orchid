package com.subgraph.orchid.bridge;

import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.data.HexDigest;

public interface BridgeRouter extends Router {
	void setIdentity(HexDigest identity);
	HexDigest getIdentity();

	void setDescriptor(RouterDescriptor descriptor);
	RouterDescriptor getDescriptor();
}