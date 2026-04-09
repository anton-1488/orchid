package com.subgraph.orchid.circuits.bridge;

import com.subgraph.orchid.ConnectionCache;
import com.subgraph.orchid.GuardEntry;
import com.subgraph.orchid.exceptions.ConnectionIOException;
import com.subgraph.orchid.routers.Router;
import org.slf4j.LoggerFactory;

public class GuardProbeTask implements Runnable {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GuardProbeTask.class);
    private final ConnectionCache connectionCache;
    private final EntryGuards entryGuards;
    private final GuardEntry entry;

    public GuardProbeTask(ConnectionCache connectionCache, EntryGuards entryGuards, GuardEntry entry) {
        this.connectionCache = connectionCache;
        this.entryGuards = entryGuards;
        this.entry = entry;
    }

    public void run() {
        Router router = entry.getRouterForEntry();
        if (router == null) {
            entryGuards.probeConnectionFailed(entry);
            return;
        }

        try {
            connectionCache.getConnectionTo(router, false);
            entryGuards.probeConnectionSucceeded(entry);
            return;
        } catch (ConnectionIOException e) {
            log.error("IO exception probing entry guard {}: ", router, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Unexpected exception probing entry guard {}:", router, e);
        }
        entryGuards.probeConnectionFailed(entry);
    }
}