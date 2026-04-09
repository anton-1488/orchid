package com.subgraph.orchid.config;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.IPv4Address;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TorConfig {
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
    private boolean useBridges = false;
    private List<TorConfigBridgeLine> bridges = List.of();

    public TorConfig(Path dataDirectory, int socksPort, long circuitBuildTimeoutMillis, long circuitStreamTimeoutMillis, int numEntryGuards, boolean useEntryGuards, List<String> excludeNodes, List<String> excludeExitNodes, List<String> exitNodes, List<String> entryNodes, boolean strictNodes, boolean useBridges, List<TorConfigBridgeLine> bridges) {
        this.dataDirectory = dataDirectory;
        this.socksPort = socksPort;
        this.circuitBuildTimeoutMillis = circuitBuildTimeoutMillis;
        this.circuitStreamTimeoutMillis = circuitStreamTimeoutMillis;
        this.numEntryGuards = numEntryGuards;
        this.useEntryGuards = useEntryGuards;
        this.excludeNodes = excludeNodes;
        this.excludeExitNodes = excludeExitNodes;
        this.exitNodes = exitNodes;
        this.entryNodes = entryNodes;
        this.strictNodes = strictNodes;
        this.useBridges = useBridges;
        this.bridges = bridges;
    }

    public TorConfig() {
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public int getSocksPort() {
        return socksPort;
    }

    public void setSocksPort(int socksPort) {
        this.socksPort = socksPort;
    }

    public long getCircuitBuildTimeoutMillis() {
        return circuitBuildTimeoutMillis;
    }

    public void setCircuitBuildTimeoutMillis(long circuitBuildTimeoutMillis) {
        this.circuitBuildTimeoutMillis = circuitBuildTimeoutMillis;
    }

    public long getCircuitStreamTimeoutMillis() {
        return circuitStreamTimeoutMillis;
    }

    public void setCircuitStreamTimeoutMillis(long circuitStreamTimeoutMillis) {
        this.circuitStreamTimeoutMillis = circuitStreamTimeoutMillis;
    }

    public int getNumEntryGuards() {
        return numEntryGuards;
    }

    public void setNumEntryGuards(int numEntryGuards) {
        this.numEntryGuards = numEntryGuards;
    }

    public boolean isUseEntryGuards() {
        return useEntryGuards;
    }

    public void setUseEntryGuards(boolean useEntryGuards) {
        this.useEntryGuards = useEntryGuards;
    }

    public List<String> getExcludeNodes() {
        return excludeNodes;
    }

    public void setExcludeNodes(List<String> excludeNodes) {
        this.excludeNodes = excludeNodes;
    }

    public List<String> getExcludeExitNodes() {
        return excludeExitNodes;
    }

    public void setExcludeExitNodes(List<String> excludeExitNodes) {
        this.excludeExitNodes = excludeExitNodes;
    }

    public List<String> getExitNodes() {
        return exitNodes;
    }

    public void setExitNodes(List<String> exitNodes) {
        this.exitNodes = exitNodes;
    }

    public List<String> getEntryNodes() {
        return entryNodes;
    }

    public void setEntryNodes(List<String> entryNodes) {
        this.entryNodes = entryNodes;
    }

    public boolean isStrictNodes() {
        return strictNodes;
    }

    public void setStrictNodes(boolean strictNodes) {
        this.strictNodes = strictNodes;
    }

    public boolean isUseBridges() {
        return useBridges;
    }

    public void setUseBridges(boolean useBridges) {
        this.useBridges = useBridges;
    }

    public List<TorConfigBridgeLine> getBridges() {
        return bridges;
    }

    public void setBridges(List<TorConfigBridgeLine> bridges) {
        this.bridges = bridges;
    }

    // Builder
    static Builder builder() {
        return new TorConfigBuilder();
    }

    public interface Builder {
        Builder dataDirectory(Path path);

        Builder dataDirectory(String path);

        Builder socksPort(int port);

        Builder circuitBuildTimeout(long time, TimeUnit unit);

        Builder circuitStreamTimeout(long time, TimeUnit unit);

        Builder numEntryGuards(int num);

        Builder useEntryGuards(boolean use);

        Builder excludeNodes(String... nodes);

        Builder excludeNodes(List<String> nodes);

        Builder excludeExitNodes(String... nodes);

        Builder exitNodes(String... nodes);

        Builder entryNodes(String... nodes);

        Builder strictNodes(boolean strict);

        Builder useBridges(boolean use);

        Builder bridges(TorConfigBridgeLine... bridges);

        Builder addBridge(IPv4Address address, int port);

        Builder addBridge(IPv4Address address, int port, HexDigest fingerprint);

        TorConfig build();
    }
}