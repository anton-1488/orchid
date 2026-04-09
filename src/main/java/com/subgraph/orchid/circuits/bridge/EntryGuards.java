package com.subgraph.orchid.circuits.bridge;

import com.subgraph.orchid.*;
import com.subgraph.orchid.circuits.path.CircuitNodeChooser;
import com.subgraph.orchid.circuits.path.CircuitNodeChooser.WeightRule;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.routers.Router;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class EntryGuards {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EntryGuards.class);
    private final ExecutorService executor = Globals.VIRTUAL_EXECUTOR;
    private final static int MIN_USABLE_GUARDS = 2;
    private final static int NUM_ENTRY_GUARDS = 3;

    private final TorConfig config;
    private final CircuitNodeChooser nodeChooser;
    private final ConnectionCache connectionCache;
    private final Directory directory;
    private final Set<GuardEntry> pendingProbes;
    private final Bridges bridges;

    public EntryGuards(TorConfig config, ConnectionCache connectionCache, DirectoryDownloader directoryDownloader, Directory directory) {
        this.config = config;
        this.nodeChooser = new CircuitNodeChooser(config, directory);
        this.connectionCache = connectionCache;
        this.directory = directory;
        this.pendingProbes = new HashSet<>();
        this.bridges = new Bridges(config, directoryDownloader);
    }

    public boolean isUsingBridges() {
        return config.getUseBridges();
    }

    public Router chooseRandomGuard(Set<Router> excluded) {
        if (config.getUseBridges()) {
            return bridges.chooseRandomBridge(excluded);
        }

        /*
         * path-spec 5.
         * When choosing the first hop of a circuit, Tor chooses at random from among the first
         * NumEntryGuards (default 3) usable guards on the list.  If there are not at least 2
         * usable guards on the list, Tor adds routers until there are, or until there are no
         * more usable routers to add.
         */
        final List<Router> usableGuards = getMinimumUsableGuards(excluded);
        final int n = Math.min(usableGuards.size(), NUM_ENTRY_GUARDS);
        return usableGuards.get(TorRandom.nextInt(n));
    }

    private synchronized List<Router> getMinimumUsableGuards(Set<Router> excluded) {
        testStatusOfAllGuards();
        while (true) {
            List<Router> usableGuards = getUsableGuardRouters(excluded);
            if (usableGuards.size() >= MIN_USABLE_GUARDS) {
                return usableGuards;
            } else {
                maybeChooseNew(usableGuards.size(), getExcludedForChooseNew(excluded, usableGuards));
            }
            try {
                wait(5000);
            } catch (InterruptedException e) {
                log.warn("Error to wait: {}", e.getMessage());
            }
        }
    }

    public synchronized void probeConnectionSucceeded(GuardEntry entry) {
        pendingProbes.remove(entry);
        if (entry.isAdded()) {
            retestProbeSucceeded(entry);
        } else {
            initialProbeSucceeded(entry);
        }
    }

    public synchronized void probeConnectionFailed(GuardEntry entry) {
        pendingProbes.remove(entry);
        if (entry.isAdded()) {
            retestProbeFailed(entry);
        }
        notifyAll();
    }

    /**
     * all methods below called holding 'lock'
     */

    private void retestProbeSucceeded(GuardEntry entry) {
        entry.clearDownSince();
    }

    private void initialProbeSucceeded(GuardEntry entry) {
        log.debug("Probe connection to {} succeeded. Adding it as a new entry guard.", entry.getRouterForEntry());
        directory.addGuardEntry(entry);
        retestAllUnreachable();
    }

    private void retestProbeFailed(GuardEntry entry) {
        entry.markAsDown();
    }

    /**
     * path-spec 5.
     * <p>
     * Additionally, Tor retries unreachable guards the first time it adds a new
     * guard to the list, since it is possible that the old guards were only marked
     * as unreachable because the network was unreachable or down.
     */
    private void retestAllUnreachable() {
        for (GuardEntry e : directory.getGuardEntries()) {
            if (e.getDownSince() != null) {
                launchEntryProbe(e);
            }
        }
    }

    private void testStatusOfAllGuards() {
        for (GuardEntry entry : directory.getGuardEntries()) {
            if (isPermanentlyUnlisted(entry) || isExpired(entry)) {
                directory.removeGuardEntry(entry);
            } else if (needsUnreachableTest(entry)) {
                launchEntryProbe(entry);
            }
        }
    }

    private List<Router> getUsableGuardRouters(Set<Router> excluded) {
        List<Router> usableRouters = new ArrayList<>();
        for (GuardEntry entry : directory.getGuardEntries()) {
            addRouterIfUsableAndNotExcluded(entry, excluded, usableRouters);
        }
        return usableRouters;
    }

    private void addRouterIfUsableAndNotExcluded(GuardEntry entry, Set<Router> excluded, List<Router> routers) {
        if (entry.testCurrentlyUsable() && entry.getDownSince() == null) {
            Router r = entry.getRouterForEntry();
            if (r != null && !excluded.contains(r)) {
                routers.add(r);
            }
        }
    }

    private Set<Router> getExcludedForChooseNew(Set<Router> excluded, List<Router> usable) {
        Set<Router> set = new HashSet<>();
        set.addAll(excluded);
        set.addAll(usable);
        addPendingInitialConnections(set);
        return set;
    }

    private void addPendingInitialConnections(Set<Router> routerSet) {
        for (GuardEntry entry : pendingProbes) {
            if (!entry.isAdded()) {
                Router r = entry.getRouterForEntry();
                if (r != null) {
                    routerSet.add(r);
                }
            }
        }
    }

    private void maybeChooseNew(int usableSize, Set<Router> excluded) {
        int sz = usableSize + countPendingInitialProbes();
        while (sz < MIN_USABLE_GUARDS) {
            Router newGuard = chooseNewGuard(excluded);
            if (newGuard == null) {
                log.warn("Need to add entry guards but no suitable guard routers are available");
                return;
            }
            log.debug("Testing {} as a new guard since we only have {} usable guards", newGuard, usableSize);
            GuardEntry entry = directory.createGuardEntryFor(newGuard);
            launchEntryProbe(entry);
            sz += 1;
        }
    }

    private int countPendingInitialProbes() {
        int count = 0;
        for (GuardEntry entry : pendingProbes) {
            if (!entry.isAdded()) {
                count += 1;
            }
        }
        return count;
    }

    private Router chooseNewGuard(final Set<Router> excluded) {
        return nodeChooser.chooseRandomNode(WeightRule.WEIGHT_FOR_GUARD, router -> router.isValid() && router.isPossibleGuard() && router.isRunning() && !excluded.contains(router));
    }

    private void launchEntryProbe(GuardEntry entry) {
        if (!entry.testCurrentlyUsable() || pendingProbes.contains(entry)) {
            return;
        }
        pendingProbes.add(entry);
        executor.execute(new GuardProbeTask(connectionCache, this, entry));
    }

    /**
     * path-spec 5.
     * <p>
     * If the guard is excluded because of its status in the networkstatuses for
     * over 30 days, Tor removes it from the list entirely, preserving order.
     */
    private boolean isPermanentlyUnlisted(GuardEntry entry) {
        Date unlistedSince = entry.getUnlistedSince();
        if (unlistedSince == null || pendingProbes.contains(entry)) {
            return false;
        }
        long unlistedTime = System.currentTimeMillis() - unlistedSince.getTime();
        return unlistedTime > RetestInterval.THIRTY_DAYS.getTime();
    }

    /**
     * Expire guards after 60 days since creation time.
     */
    private boolean isExpired(GuardEntry entry) {
        Date createdAt = entry.getCreatedTime();
        long createdAgo = System.currentTimeMillis() - createdAt.getTime();
        return createdAgo > RetestInterval.SIXTY_DAYS.getTime();
    }

    private boolean needsUnreachableTest(GuardEntry entry) {
        final Date downSince = entry.getDownSince();
        if (downSince == null || !entry.testCurrentlyUsable()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long timeDown = now - downSince.getTime();
        Date lastConnect = entry.getLastConnectAttempt();
        long timeSinceLastRetest = (lastConnect == null) ? timeDown : (now - lastConnect.getTime());

        return timeSinceLastRetest > RetestInterval.getRetestInterval(timeDown).getTime();
    }
}