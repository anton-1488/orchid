package com.subgraph.orchid;

import com.subgraph.orchid.circuits.Circuit;
import com.subgraph.orchid.routers.Router;

public interface InternalCircuit extends Circuit {
	DirectoryCircuit cannibalizeToDirectory(Router target);
	Circuit cannibalizeToIntroductionPoint(Router target);
	HiddenServiceCircuit connectHiddenService(CircuitNode node);
}
