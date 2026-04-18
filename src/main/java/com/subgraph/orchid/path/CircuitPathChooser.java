package com.subgraph.orchid.path;

import com.subgraph.orchid.bridge.EntryGuards;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.data.exitpolicy.ExitTarget;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.exceptions.PathSelectionFailedException;
import com.subgraph.orchid.path.CircuitNodeChooser.WeightRule;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.*;


public class CircuitPathChooser {
    @Contract("_, _ -> new")
    public static @NotNull CircuitPathChooser create(TorConfig config, Directory directory) {
        return new CircuitPathChooser(config, directory, new CircuitNodeChooser(config, directory));
    }

    private final Directory directory;
    private final CircuitNodeChooser nodeChooser;
    private EntryGuards entryGuards;
    private boolean useEntryGuards;

    private CircuitPathChooser(TorConfig config, Directory directory, CircuitNodeChooser nodeChooser) {
        this.directory = directory;
        this.nodeChooser = nodeChooser;
        this.entryGuards = null;
        this.useEntryGuards = false;
    }

    public void enableEntryGuards(EntryGuards entryGuards) {
        this.entryGuards = entryGuards;
        this.useEntryGuards = true;
    }

    public List<Router> chooseDirectoryPath() {
        if (useEntryGuards && entryGuards.isUsingBridges()) {
            Set<Router> empty = new HashSet<>();
            Router bridge = entryGuards.chooseRandomGuard(empty);
            if (bridge == null) {
                throw new IllegalStateException("Failed to choose bridge for directory request");
            }
            return List.of(bridge);
        }
        Router dir = nodeChooser.chooseDirectory();
        return List.of(dir);
    }

    public List<Router> chooseInternalPath() throws PathSelectionFailedException {
        Set<Router> excluded = new HashSet<>();
        Router finalRouter = chooseMiddleNode(excluded);
        return choosePathWithFinal(finalRouter);
    }

    public List<Router> choosePathWithExit(Router exitRouter) throws PathSelectionFailedException {
        return choosePathWithFinal(exitRouter);
    }

    public List<Router> choosePathWithFinal(Router finalRouter) throws PathSelectionFailedException {
        Set<Router> excluded = new HashSet<>();
        excludeChosenRouterAndRelated(finalRouter, excluded);

        Router middleRouter = chooseMiddleNode(excluded);
        if (middleRouter == null) {
            throw new PathSelectionFailedException("Failed to select suitable middle node");
        }

        excludeChosenRouterAndRelated(middleRouter, excluded);
        Router entryRouter = chooseEntryNode(excluded);

        if (entryRouter == null) {
            throw new PathSelectionFailedException("Failed to select suitable entry node");
        }
        return Arrays.asList(entryRouter, middleRouter, finalRouter);
    }

    public Router chooseEntryNode(final Set<Router> excludedRouters) {
        if (useEntryGuards) {
            return entryGuards.chooseRandomGuard(excludedRouters);
        }
        return nodeChooser.chooseRandomNode(WeightRule.WEIGHT_FOR_GUARD, router -> router.isPossibleGuard() && !excludedRouters.contains(router));
    }

    public Router chooseMiddleNode(final Set<Router> excludedRouters) {
        return nodeChooser.chooseRandomNode(WeightRule.WEIGHT_FOR_MID, router -> router.isFast() && !excludedRouters.contains(router));
    }

    public Router chooseExitNodeForTargets(List<ExitTarget> targets) {
        List<Router> routers = filterForExitTargets(getUsableExitRouters(), targets);
        return nodeChooser.chooseExitNode(routers);
    }

    private @NotNull List<Router> getUsableExitRouters() {
        List<Router> result = new ArrayList<>();
        for (Router r : nodeChooser.getUsableRouters(true)) {
            if (r.isExit() && !r.isBadExit()) {
                result.add(r);
            }
        }
        return result;
    }

    private void excludeChosenRouterAndRelated(Router router, @NotNull Set<Router> excludedRouters) {
        excludedRouters.add(router);
        for (Router r : directory.getAllRouters()) {
            if (areInSameSlash16(router, r)) {
                excludedRouters.add(r);
            }
        }

        for (String s : router.getFamilyMembers()) {
            Router r = directory.getRouterByName(s);
            if (r != null) {
                if (isFamilyMember(r.getFamilyMembers(), router)) {
                    excludedRouters.add(r);
                }
            }
        }
    }

    private boolean isFamilyMember(@NotNull Collection<String> familyMemberNames, Router r) {
        for (String s : familyMemberNames) {
            Router member = directory.getRouterByName(s);
            if (member != null && member.equals(r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Are routers r1 and r2 in the same /16 network
     *
     * @param r1 router 1
     * @param r2 router 2
     * @return Are routers r1 and r2 in the same /16 network
     */
    private boolean areInSameSlash16(@NotNull Router r1, @NotNull Router r2) {
        InetAddress a1 = r1.getAddress();
        InetAddress a2 = r2.getAddress();

        byte[] bytes1 = a1.getAddress();
        byte[] bytes2 = a2.getAddress();

        // Compare first 2 bytes (16 bits) of IPv4 address
        return bytes1[0] == bytes2[0] && bytes1[1] == bytes2[1];
    }

    private List<Router> filterForExitTargets(List<Router> routers, @NotNull List<ExitTarget> exitTargets) {
        int bestSupport = 0;
        if (exitTargets.isEmpty()) {
            return routers;
        }

        int[] nSupport = new int[routers.size()];
        for (int i = 0; i < routers.size(); i++) {
            Router r = routers.get(i);
            nSupport[i] = countTargetSupport(r, exitTargets);
            if (nSupport[i] > bestSupport) {
                bestSupport = nSupport[i];
            }
        }

        if (bestSupport == 0) {
            return routers;
        }

        List<Router> results = new ArrayList<>();
        for (int i = 0; i < routers.size(); i++) {
            if (nSupport[i] == bestSupport) {
                results.add(routers.get(i));
            }
        }
        return results;
    }

    private int countTargetSupport(Router router, @NotNull List<ExitTarget> targets) {
        int count = 0;
        for (ExitTarget t : targets) {
            if (routerSupportsTarget(router, t)) {
                count += 1;
            }
        }
        return count;
    }

    private boolean routerSupportsTarget(Router router, @NotNull ExitTarget target) {
        if (target.isAddressTarget()) {
            return router.exitPolicyAccepts(target.getAddress(), target.port());
        } else {
            return router.exitPolicyAccepts(target.port());
        }
    }
}