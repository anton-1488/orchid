package com.subgraph.orchid;

import com.subgraph.orchid.circuits.Circuit;

public interface InternalCircuit extends Circuit {
	DirectoryCircuit cannibalizeToDirectory(Router target);
	Circuit cannibalizeToIntroductionPoint(Router target);
	HiddenServiceCircuit connectHiddenService(CircuitNode node);
}
