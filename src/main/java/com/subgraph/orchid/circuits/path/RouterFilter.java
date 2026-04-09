package com.subgraph.orchid.circuits.path;

import com.subgraph.orchid.directory.router.Router;

public interface RouterFilter {
	boolean filter(Router router);
}
