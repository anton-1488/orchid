package com.subgraph.orchid.circuits;

import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.router.Router;

public interface InternalCircuit extends Circuit {
    DirectoryCircuit cannibalizeToDirectory(Router target);

    Circuit cannibalizeToIntroductionPoint(Router target);

    HiddenServiceCircuit connectHiddenService(CircuitNode node);
}