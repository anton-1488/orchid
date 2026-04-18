package com.subgraph.orchid.path;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.path.filters.RouterFilter;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CircuitNodeChooser {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CircuitNodeChooser.class);

    public enum WeightRule {WEIGHT_FOR_DIR, WEIGHT_FOR_EXIT, WEIGHT_FOR_MID, WEIGHT_FOR_GUARD, NO_WEIGHTING}

    private final Directory directory;
    private final TorConfigNodeFilter configNodeFilter;


    public CircuitNodeChooser(TorConfig config, Directory directory) {
        this.directory = directory;
        this.configNodeFilter = new TorConfigNodeFilter(config);
    }

    /**
     *
     * @return The chosen exit router or 'null' if no suitable router is available
     */
    public Router chooseExitNode(List<Router> candidates) {
        List<Router> filteredCandidates = configNodeFilter.filterExitCandidates(candidates);
        return chooseByBandwidth(filteredCandidates, WeightRule.WEIGHT_FOR_EXIT);
    }

    public Router chooseDirectory() {
        RouterFilter filter = router -> router.getDirectoryPort() != 0;
        List<Router> candidates = getFilteredRouters(filter, false);
        Router choice = chooseByBandwidth(candidates, WeightRule.WEIGHT_FOR_DIR);
        if (choice == null) {
            return directory.getRandomDirectoryAuthority();
        } else {
            return choice;
        }
    }

    /**
     *
     * @return The chosen router or 'null' if no suitable router is available.
     */
    public Router chooseRandomNode(WeightRule rule, RouterFilter routerFilter) {
        List<Router> candidates = getFilteredRouters(routerFilter, true);
        return chooseByBandwidth(candidates, rule);
    }

    private @NotNull List<Router> getFilteredRouters(RouterFilter rf, boolean needDescriptor) {
        List<Router> routers = new ArrayList<>();
        for (Router r : getUsableRouters(needDescriptor)) {
            if (rf.filter(r)) {
                routers.add(r);
            }
        }
        return routers;
    }

    public List<Router> getUsableRouters(boolean needDescriptor) {
        List<Router> routers = new ArrayList<>();
        for (Router r : directory.getAllRouters()) {
            if (r.isRunning() && r.isValid() && !r.isHibernating() && !(needDescriptor && r.getCurrentDescriptor() == null)) {
                routers.add(r);
            }
        }
        return routers;
    }

    private Router chooseByBandwidth(List<Router> candidates, WeightRule rule) {
        Router choice = chooseNodeByBandwidthWeights(candidates, rule);
        if (choice != null) {
            return choice;
        } else {
            return chooseNodeByBandwidth(candidates, rule);
        }
    }

    private @Nullable Router chooseNodeByBandwidthWeights(List<Router> candidates, WeightRule rule) {
        ConsensusDocument consensus = directory.getCurrentConsensusDocument();
        if (consensus == null) {
            return null;
        }
        BandwidthWeightedRouters bwr = computeWeightedBandwidths(candidates, consensus, rule);
        return Objects.requireNonNull(bwr).chooseRandomRouterByWeight();
    }


    private @Nullable BandwidthWeightedRouters computeWeightedBandwidths(List<Router> candidates, ConsensusDocument consensus, WeightRule rule) {
        CircuitNodeChooserWeightParameters wp = CircuitNodeChooserWeightParameters.create(consensus, rule);
        if (!wp.isValid()) {
            log.warn("Got invalid bandwidth weights. Falling back to old selection method");
            return null;
        }
        BandwidthWeightedRouters weightedRouters = new BandwidthWeightedRouters();
        for (Router r : candidates) {
            double wbw = wp.calculateWeightedBandwidth(r);
            weightedRouters.addRouter(r, wbw);
        }
        return weightedRouters;
    }

    private @Nullable Router chooseNodeByBandwidth(@NotNull List<Router> routers, WeightRule rule) {
        BandwidthWeightedRouters bwr = new BandwidthWeightedRouters();
        for (Router r : routers) {
            long bw = getRouterBandwidthBytes(r);
            if (bw == -1) {
                bwr.addRouterUnknown(r);
            } else {
                bwr.addRouter(r, bw);
            }
        }

        bwr.fixUnknownValues();
        if (bwr.isTotalBandwidthZero()) {
            if (routers.isEmpty()) {
                return null;
            }
            int idx = TorRandom.nextInt(routers.size());
            return routers.get(idx);
        }
        computeFinalWeights(bwr, rule);
        return bwr.chooseRandomRouterByWeight();
    }

    private void computeFinalWeights(@NotNull BandwidthWeightedRouters bwr, WeightRule rule) {
        final double exitWeight = calculateWeight(rule == WeightRule.WEIGHT_FOR_EXIT,
                bwr.getTotalExitBandwidth(), bwr.getTotalBandwidth());
        final double guardWeight = calculateWeight(rule == WeightRule.WEIGHT_FOR_GUARD,
                bwr.getTotalGuardBandwidth(), bwr.getTotalBandwidth());

        bwr.adjustWeights(exitWeight, guardWeight);
    }

    private double calculateWeight(boolean matchesRule, double totalByType, double total) {
        if (matchesRule || totalByType < BandwidthWeightedRouters.EPSILON) {
            return 1.0;
        }
        final double result = 1.0 - (total / (3.0 * totalByType));
        return Math.max(result, 0.0);
    }

    private long getRouterBandwidthBytes(@NotNull Router r) {
        if (!r.hasBandwidth()) {
            return -1;
        } else {
            return kbToBytes(r.getEstimatedBandwidth());
        }
    }

    private long kbToBytes(long bw) {
        return (bw > (Long.MAX_VALUE / 1000) ? Long.MAX_VALUE : bw * 1000);
    }
}