package com.subgraph.orchid.config;

import com.subgraph.orchid.data.HexDigest;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record TorConfig(Path dataDirectory, int socksPort, long circuitBuildTimeoutMillis,
                        long circuitStreamTimeoutMillis, int numEntryGuards, boolean useEntryGuards, boolean useMicroDescriptors,
                        List<String> excludeNodes, List<String> excludeExitNodes, List<String> exitNodes,
                        List<String> entryNodes, boolean strictNodes, boolean useBridges,
                        List<TorConfigBridgeLine> bridges) {

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

        Builder useMicroDesciptors(boolean use);

        Builder excludeNodes(String... nodes);

        Builder excludeNodes(List<String> nodes);

        Builder excludeExitNodes(String... nodes);

        Builder exitNodes(String... nodes);

        Builder entryNodes(String... nodes);

        Builder strictNodes(boolean strict);

        Builder useBridges(boolean use);

        Builder bridges(TorConfigBridgeLine... bridges);

        Builder addBridge(InetAddress address, int port);

        Builder addBridge(InetAddress address, int port, HexDigest fingerprint);

        TorConfig build();
    }
}