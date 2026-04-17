package com.subgraph.orchid.path;

import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

class CircuitNodeChooserWeightParameters {
    private final static int VAR_WG = 0;
    private final static int VAR_WM = 1;
    private final static int VAR_WE = 2;
    private final static int VAR_WD = 3;
    private final static int VAR_WGB = 4;
    private final static int VAR_WMB = 5;
    private final static int VAR_WEB = 6;
    private final static int VAR_WDB = 7;
    private final static int VAR_COUNT = 8;

    private final static String ZERO = "zero";
    private final static String ONE = "one";

    @Contract("_, _ -> new")
    public static @NotNull CircuitNodeChooserWeightParameters create(@NotNull ConsensusDocument consensus, CircuitNodeChooser.WeightRule rule) {
        double[] vars = new double[VAR_COUNT];
        long scale = consensus.getWeightScaleParameter();
        String[] tags = getTagsForWeightRule(rule);
        if (!populateVars(consensus, scale, tags, vars)) {
            return new CircuitNodeChooserWeightParameters(new double[VAR_COUNT], false);
        } else {
            return new CircuitNodeChooserWeightParameters(vars, true);
        }
    }

    private static boolean populateVars(ConsensusDocument consensus, long scale, String[] tags, double[] vars) {
        for (int i = 0; i < VAR_COUNT; i++) {
            vars[i] = tagToVarValue(consensus, scale, tags[i]);
            if (vars[i] < 0.0) {
                return false;
            } else {
                vars[i] /= scale;
            }
        }
        return true;
    }

    private static double tagToVarValue(ConsensusDocument consensus, long scale, @NotNull String tag) {
        if (tag.equals(ZERO)) {
            return 0.0;
        } else if (tag.equals(ONE)) {
            return 1.0;
        } else {
            return consensus.getBandwidthWeight(tag);
        }
    }

    @Contract(pure = true)
    private static String @NotNull [] getTagsForWeightRule(CircuitNodeChooser.@NotNull WeightRule rule) {
        return switch (rule) {
            case WEIGHT_FOR_GUARD -> new String[]{
                    "Wgg", "Wgm", ZERO, "Wgd",
                    "Wgb", "Wmb", "Web", "Wdb"};
            case WEIGHT_FOR_MID -> new String[]{
                    "Wmg", "Wmm", "Wme", "Wmd",
                    "Wgb", "Wmb", "Web", "Wdb"};
            case WEIGHT_FOR_EXIT -> new String[]{
                    "Wee", "Wem", "Wed", "Weg",
                    "Wgb", "Wmb", "Web", "Wdb"};
            case WEIGHT_FOR_DIR -> new String[]{
                    "Wbe", "Wbm", "Wbd", "Wbg",
                    ONE, ONE, ONE, ONE};
            case NO_WEIGHTING -> new String[]{
                    ONE, ONE, ONE, ONE,
                    ONE, ONE, ONE, ONE};
        };
    }

    private final double[] vars;
    private final boolean isValid;

    private CircuitNodeChooserWeightParameters(double[] vars, boolean isValid) {
        this.vars = vars;
        this.isValid = isValid;
    }

    public boolean isValid() {
        return isValid;
    }

    public double getWg() {
        return vars[VAR_WG];
    }

    public double getWm() {
        return vars[VAR_WM];
    }

    public double getWe() {
        return vars[VAR_WE];
    }

    public double getWd() {
        return vars[VAR_WD];
    }

    public double getWgb() {
        return vars[VAR_WGB];
    }

    public double getWmb() {
        return vars[VAR_WMB];
    }

    public double getWeb() {
        return vars[VAR_WEB];
    }

    public double getWdb() {
        return vars[VAR_WDB];
    }

    public double calculateWeightedBandwidth(@NotNull Router router) {
        long bw = kbToBytes(router.getEstimatedBandwidth());
        double w = calculateWeight(router.isExit() && !router.isBadExit(), router.isPossibleGuard(), router.getDirectoryPort() != 0);
        return (w * bw) + 0.5;
    }

    public long kbToBytes(long kb) {
        return (kb > (Long.MAX_VALUE / 1000) ? Long.MAX_VALUE : kb * 1000);
    }

    private double calculateWeight(boolean isExit, boolean isGuard, boolean isDir) {
        if (isGuard && isExit) {
            return (isDir) ? getWdb() * getWd() : getWd();
        } else if (isGuard) {
            return (isDir) ? getWgb() * getWg() : getWg();
        } else if (isExit) {
            return (isDir) ? getWeb() * getWe() : getWe();
        } else {
            return (isDir) ? getWmb() * getWm() : getWm();
        }
    }
}