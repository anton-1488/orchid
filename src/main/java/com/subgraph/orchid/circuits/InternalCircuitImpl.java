package com.subgraph.orchid.circuits;

import com.subgraph.orchid.Stream;
import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.path.CircuitPathChooser;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.stream.StreamImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class InternalCircuitImpl extends CircuitImpl implements InternalCircuit, DirectoryCircuit, HiddenServiceCircuit {
    public enum InternalType {
        UNUSED,
        HS_INTRODUCTION,
        HS_DIRECTORY,
        HS_CIRCUIT
    }

    private InternalType type;
    private final boolean ntorEnabled;

    public InternalCircuitImpl(CircuitManagerImpl circuitManager, List<Router> prechosenPath) {
        super(circuitManager, prechosenPath);
        this.type = InternalType.UNUSED;
        this.ntorEnabled = circuitManager.isNtorEnabled();
    }

    protected InternalCircuitImpl(CircuitManagerImpl circuitManager) {
        this(circuitManager, null);
    }

    @Override
    public List<Router> choosePathForCircuit(@NotNull CircuitPathChooser pathChooser) throws PathSelectionFailedException {
        return pathChooser.chooseInternalPath();
    }

    @Override
    public Circuit cannibalizeToIntroductionPoint(Router target) {
        cannibalizeTo(target);
        type = InternalType.HS_INTRODUCTION;
        return this;
    }

    private void cannibalizeTo(Router target) {
        if (type != InternalType.UNUSED) {
            throw new IllegalStateException("Cannot cannibalize internal circuit with type " + type);
        }
        CircuitExtender extender = new CircuitExtender(this, ntorEnabled);
        extender.extendTo(target);
    }

    @Override
    public Stream openDirectoryStream(long timeout, boolean autoclose) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        if (type != InternalType.HS_DIRECTORY) {
            throw new IllegalStateException("Cannot open directory stream on internal circuit with type " + type);
        }
        StreamImpl stream = createNewStream();
        try {
            stream.openDirectory(timeout);
            return stream;
        } catch (Exception e) {
            removeStream(stream);
            return processStreamOpenException(e);
        }
    }

    @Override
    public DirectoryCircuit cannibalizeToDirectory(Router target) {
        cannibalizeTo(target);
        type = InternalType.HS_DIRECTORY;
        return this;
    }

    @Override
    public HiddenServiceCircuit connectHiddenService(CircuitNode node) {
        if (type != InternalType.UNUSED) {
            throw new IllegalStateException("Cannot connect hidden service from internal circuit type " + type);
        }
        appendNode(node);
        type = InternalType.HS_CIRCUIT;
        return this;
    }

    @Override
    public Stream openStream(int port, long timeout) throws InterruptedException, TimeoutException, StreamConnectFailedException {
        if (type != InternalType.HS_CIRCUIT) {
            throw new IllegalStateException("Cannot open stream to hidden service from internal circuit type " + type);
        }
        StreamImpl stream = createNewStream();
        try {
            stream.openExit("", port, timeout);
            return stream;
        } catch (Exception e) {
            removeStream(stream);
            return processStreamOpenException(e);
        }
    }
}