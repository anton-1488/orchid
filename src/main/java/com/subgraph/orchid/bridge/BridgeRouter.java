package com.subgraph.orchid.bridge;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.router.RouterDescriptor;

public interface BridgeRouter extends Router {
    void setIdentity(HexDigest identity);

    HexDigest getIdentity();

    void setDescriptor(RouterDescriptor descriptor);

    RouterDescriptor getDescriptor();
}