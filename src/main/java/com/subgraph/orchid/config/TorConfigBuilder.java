package com.subgraph.orchid.config;

import com.subgraph.orchid.data.HexDigest;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.subgraph.orchid.config.TorConfig.Builder;

public class TorConfigBuilder implements Builder {
    private Path dataDirectory = Path.of("tor-data");
    private int socksPort = 9050;
    private long circuitBuildTimeoutMillis = TimeUnit.SECONDS.toMillis(60);
    private long circuitStreamTimeoutMillis = TimeUnit.SECONDS.toMillis(60);
    private int numEntryGuards = 3;
    private boolean useEntryGuards = true;
    private List<String> excludeNodes = List.of();
    private List<String> excludeExitNodes = List.of();
    private List<String> exitNodes = List.of();
    private List<String> entryNodes = List.of();
    private boolean strictNodes = false;
    private boolean useMicroDescriptors = true;
    private boolean useBridges = false;
    private List<TorConfigBridgeLine> bridges = List.of();

    @Override
    public Builder dataDirectory(Path path) {
        this.dataDirectory = path;
        return this;
    }

    @Override
    public Builder dataDirectory(String path) {
        this.dataDirectory = Path.of(path);
        return this;
    }

    @Override
    public Builder socksPort(int port) {
        this.socksPort = port;
        return this;
    }

    @Override
    public Builder circuitBuildTimeout(long time, TimeUnit unit) {
        this.circuitBuildTimeoutMillis = unit.toMillis(time);
        return this;
    }

    @Override
    public Builder circuitStreamTimeout(long time, TimeUnit unit) {
        this.circuitStreamTimeoutMillis = unit.toMillis(time);
        return this;
    }

    @Override
    public Builder numEntryGuards(int num) {
        this.numEntryGuards = num;
        return this;
    }

    @Override
    public Builder useEntryGuards(boolean use) {
        this.useEntryGuards = use;
        return this;
    }

    @Override
    public Builder useMicroDesciptors(boolean use) {
        this.useMicroDescriptors = use;
        return this;
    }

    @Override
    public Builder excludeNodes(String... nodes) {
        this.excludeNodes = List.of(nodes);
        return this;
    }

    @Override
    public Builder excludeNodes(List<String> nodes) {
        this.excludeNodes = List.copyOf(nodes);
        return this;
    }

    @Override
    public Builder excludeExitNodes(String... nodes) {
        this.excludeExitNodes = List.of(nodes);
        return this;
    }

    @Override
    public Builder exitNodes(String... nodes) {
        this.exitNodes = List.of(nodes);
        return this;
    }

    @Override
    public Builder entryNodes(String... nodes) {
        this.entryNodes = List.of(nodes);
        return this;
    }

    @Override
    public Builder strictNodes(boolean strict) {
        this.strictNodes = strict;
        return this;
    }

    @Override
    public Builder useBridges(boolean use) {
        this.useBridges = use;
        return this;
    }

    @Override
    public Builder bridges(TorConfigBridgeLine... bridges) {
        this.bridges = List.of(bridges);
        return this;
    }

    @Override
    public Builder addBridge(InetAddress address, int port) {
        List<TorConfigBridgeLine> newBridges = new ArrayList<>(bridges);
        newBridges.add(new TorConfigBridgeLine(address, port, null));
        this.bridges = List.copyOf(newBridges);
        return this;
    }

    @Override
    public Builder addBridge(InetAddress address, int port, HexDigest fingerprint) {
        List<TorConfigBridgeLine> newBridges = new ArrayList<>(bridges);
        newBridges.add(new TorConfigBridgeLine(address, port, fingerprint));
        this.bridges = List.copyOf(newBridges);
        return this;
    }

    @Override
    public TorConfig build() {
        return new TorConfig(
                dataDirectory,
                socksPort,
                circuitBuildTimeoutMillis,
                circuitStreamTimeoutMillis,
                numEntryGuards,
                useEntryGuards,
                useMicroDescriptors,
                excludeNodes,
                excludeExitNodes,
                exitNodes,
                entryNodes,
                strictNodes,
                useBridges,
                bridges
        );
    }
}