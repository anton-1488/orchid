package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.StreamImpl;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class DirectoryCircuitImpl extends CircuitImpl implements DirectoryCircuit {
    protected DirectoryCircuitImpl(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        super(circuitManager, prechosenPath);
    }

    @Override
    public Stream openDirectoryStream(long timeout, boolean autoclose) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        StreamImpl stream = createNewStream(autoclose);
        try {
            stream.openDirectory(timeout);
            return stream;
        } catch (Exception e) {
            removeStream(stream);
            throw e;
        }
    }

    @Override
    protected List<Router> choosePathForCircuit(CircuitPathChooser pathChooser) throws PathSelectionFailedException {
        if (prechosenPath != null) {
            return prechosenPath;
        }
        return pathChooser.chooseDirectoryPath();
    }
}