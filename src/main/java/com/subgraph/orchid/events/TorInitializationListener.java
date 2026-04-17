package com.subgraph.orchid.events;

import com.subgraph.orchid.BootstrapStatus;

@FunctionalInterface
public interface TorInitializationListener {
	void initializationProgress(BootstrapStatus status);
}
