package com.subgraph.orchid.circuits.path;

import com.subgraph.orchid.routers.Router;

public interface RouterFilter {
	boolean filter(Router router);
}
