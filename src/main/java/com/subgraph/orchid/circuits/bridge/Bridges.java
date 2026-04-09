package com.subgraph.orchid.circuits.bridge;

import com.subgraph.orchid.DirectoryDownloader;
import com.subgraph.orchid.Globals;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.config.TorConfigBridgeLine;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import com.subgraph.orchid.directory.router.Router;
import com.subgraph.orchid.directory.router.RouterDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class Bridges {
    private static final Logger log = LoggerFactory.getLogger(Bridges.class);
    private final ExecutorService loader = Globals.VIRTUAL_EXECUTOR;

    private final TorConfig config;
    private final DirectoryDownloader directoryDownloader;
    private final Set<BridgeRouterImpl> bridgeRouters = ConcurrentHashMap.newKeySet();
    private final CompletableFuture<Void> initFuture = new CompletableFuture<>();

    public Bridges(TorConfig config, DirectoryDownloader directoryDownloader) {
        this.config = config;
        this.directoryDownloader = directoryDownloader;
        initializeBridges();
    }

    public BridgeRouter chooseRandomBridge(Set<Router> excluded) {
        try {
            initFuture.get();
        } catch (Exception e) {
            log.warn("Bridges not available", e);
            return null;
        }
        List<BridgeRouter> candidates = bridgeRouters.stream().filter(br -> !excluded.contains(br)).map(br -> (BridgeRouter) br).toList();
        return candidates.isEmpty() ? null : candidates.get(TorRandom.nextInt(candidates.size()));
    }

    private void initializeBridges() {
        List<CompletableFuture<Void>> futures = config.getBridges().stream()
                .map(line -> CompletableFuture.runAsync(
                        () -> downloadBridge(createBridgeFromLine(line)),
                        loader
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, err) -> {
                    if (err != null) {
                        initFuture.completeExceptionally(err);
                    } else {
                        log.info("{} bridges initialized", bridgeRouters.size());
                        initFuture.complete(null);
                    }
                });
    }

    private void downloadBridge(BridgeRouterImpl bridge) {
        try {
            RouterDescriptor descriptor = directoryDownloader.downloadBridgeDescriptor(bridge);
            if (descriptor != null) {
                bridge.setDescriptor(descriptor);
                bridgeRouters.add(bridge);
            }
        } catch (DirectoryRequestFailedException e) {
            log.warn("Failed to download bridge {}", bridge, e);
        }
    }

    private BridgeRouterImpl createBridgeFromLine(TorConfigBridgeLine line) {
        BridgeRouterImpl bridge = new BridgeRouterImpl(line.getAddress(), line.getPort());
        if (line.getFingerprint() != null) {
            bridge.setIdentity(line.getFingerprint());
        }
        return bridge;
    }
}