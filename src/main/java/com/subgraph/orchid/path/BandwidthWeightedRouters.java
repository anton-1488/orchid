package com.subgraph.orchid.path;

import java.util.ArrayList;
import java.util.List;

import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.crypto.TorRandom;
import org.jetbrains.annotations.NotNull;

public class BandwidthWeightedRouters {
    public static final long MAX_SCALE = Long.MAX_VALUE / 4;
    public static final double EPSILON = 0.1;

    private static class WeightedRouter {
        private final Router router;
        private boolean isUnknown;
        private double weightedBandwidth;
        private long scaledBandwidth;

        WeightedRouter(Router router, double bw) {
            this.router = router;
            this.weightedBandwidth = bw;
        }

        void scaleBandwidth(double scaleFactor) {
            scaledBandwidth = Math.round(weightedBandwidth * scaleFactor);
        }
    }

    private final List<WeightedRouter> weightedRouters = new ArrayList<>();
    private double totalExitBw;
    private double totalNonExitBw;
    private double totalGuardBw;
    private boolean isScaled;
    private int unknownCount;

    public void addRouter(Router router, double weightedBandwidth) {
        weightedRouters.add(new WeightedRouter(router, weightedBandwidth));
        adjustTotals(router, weightedBandwidth);
        isScaled = false;
    }

    public boolean isTotalBandwidthZero() {
        return getTotalBandwidth() < EPSILON;
    }

    public double getTotalBandwidth() {
        return totalExitBw + totalNonExitBw;
    }

    public double getTotalGuardBandwidth() {
        return totalGuardBw;
    }

    public double getTotalExitBandwidth() {
        return totalExitBw;
    }

    public void addRouterUnknown(Router router) {
        WeightedRouter wr = new WeightedRouter(router, 0);
        wr.isUnknown = true;
        weightedRouters.add(wr);
        unknownCount += 1;
    }

    public int getRouterCount() {
        return weightedRouters.size();
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public void fixUnknownValues() {
        if (unknownCount == 0) {
            return;
        }
        if (isTotalBandwidthZero()) {
            fixUnknownValues(40000, 20000);
        } else {
            int knownCount = weightedRouters.size() - unknownCount;
            long average = (long) (getTotalBandwidth() / knownCount);
            fixUnknownValues(average, average);
        }
    }

    public Router chooseRandomRouterByWeight() {
        long total = getScaledTotal();
        if (total == 0) {
            if (weightedRouters.isEmpty()) {
                return null;
            }
            int idx = TorRandom.nextInt(weightedRouters.size());
            return weightedRouters.get(idx).router;
        }
        return chooseFirstElementAboveRandom(TorRandom.nextLong(total));
    }

    public void adjustWeights(double exitWeight, double guardWeight) {
        for (WeightedRouter wr : weightedRouters) {
            Router r = wr.router;
            if (r.isExit() && r.isPossibleGuard()) {
                wr.weightedBandwidth *= (exitWeight * guardWeight);
            } else if (r.isPossibleGuard()) {
                wr.weightedBandwidth *= guardWeight;
            } else if (r.isExit()) {
                wr.weightedBandwidth *= exitWeight;
            }
        }
        scaleRouterWeights();
    }

    private Router chooseFirstElementAboveRandom(long randomValue) {
        long sum = 0;
        Router chosen = null;
        for (WeightedRouter wr : weightedRouters) {
            sum += wr.scaledBandwidth;
            if (sum > randomValue) {
                chosen = wr.router;
                randomValue = Long.MAX_VALUE; // Don't return early to avoid leaking timing information about choice
            }
        }
        if (chosen == null) {
            return weightedRouters.getLast().router;
        }
        return chosen;
    }

    private double getWeightedTotal() {
        double total = 0.0;
        for (WeightedRouter wr : weightedRouters) {
            total += wr.weightedBandwidth;
        }
        return total;
    }

    private void scaleRouterWeights() {
        double scaleFactor = MAX_SCALE / getWeightedTotal();
        for (WeightedRouter wr : weightedRouters) {
            wr.scaleBandwidth(scaleFactor);
        }
        isScaled = true;
    }

    private long getScaledTotal() {
        if (!isScaled) {
            scaleRouterWeights();
        }
        long total = 0;
        for (WeightedRouter wr : weightedRouters) {
            total += wr.scaledBandwidth;
        }
        return total;
    }

    private void adjustTotals(@NotNull Router router, double bw) {
        if (router.isExit()) {
            totalExitBw += bw;
        } else {
            totalNonExitBw += bw;
        }
        if (router.isPossibleGuard()) {
            totalGuardBw += bw;
        }
    }

    private void fixUnknownValues(long fastBw, long slowBw) {
        for (WeightedRouter wr : weightedRouters) {
            if (wr.isUnknown) {
                long bw = wr.router.isFast() ? fastBw : slowBw;
                wr.weightedBandwidth = bw;
                wr.isUnknown = false;
                adjustTotals(wr.router, bw);
            }
        }
        unknownCount = 0;
        isScaled = false;
    }
}