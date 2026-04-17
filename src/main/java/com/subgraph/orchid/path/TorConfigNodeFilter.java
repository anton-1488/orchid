package com.subgraph.orchid.path;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.path.filters.ConfigNodeFilter;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorConfigNodeFilter {
    /*
     * Even though these are exactly the configuration file variable names, they are only
     * used here as keys into a Map<String,ConfigNodeFilter>
     */
    private final static String EXCLUDE_NODES_FILTER = "ExcludeNodes";
    private final static String EXCLUDE_EXIT_NODES_FILTER = "ExcludeExitNodes";
    private final static String ENTRY_NODES_FILTER = "EntryNodes";
    private final static String EXIT_NODES_FILTER = "ExitNodes";

    private final Map<String, ConfigNodeFilter> filters;

    TorConfigNodeFilter(TorConfig config) {
        this.filters = new HashMap<>();
        addFilter(filters, EXCLUDE_NODES_FILTER, config.excludeNodes());
        addFilter(filters, EXCLUDE_EXIT_NODES_FILTER, config.excludeExitNodes());
        addFilter(filters, ENTRY_NODES_FILTER, config.entryNodes());
        addFilter(filters, EXIT_NODES_FILTER, config.exitNodes());
    }

    private static void addFilter(Map<String, ConfigNodeFilter> filters, String name, List<String> filterStrings) {
        if (filterStrings == null || filterStrings.isEmpty()) {
            return;
        }
        filters.put(name, ConfigNodeFilter.createFromStrings(filterStrings));
    }

    public List<Router> filterExitCandidates(@NotNull List<Router> candidates) {
        List<Router> filtered = new ArrayList<>();
        for (Router r : candidates) {
            if (isExitNodeIncluded(r)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    public boolean isExitNodeIncluded(Router exitRouter) {
        return isIncludedByFilter(exitRouter) && !(isExcludedByFilter(exitRouter, EXCLUDE_EXIT_NODES_FILTER) || isExcludedByFilter(exitRouter, EXCLUDE_NODES_FILTER));
    }

    public boolean isIncludedByFilter(Router r) {
        ConfigNodeFilter f = filters.get(TorConfigNodeFilter.EXIT_NODES_FILTER);
        if (f == null || f.isEmpty()) {
            return true;
        }
        return f.filter(r);
    }

    public boolean isExcludedByFilter(Router r, String filterName) {
        ConfigNodeFilter f = filters.get(filterName);
        if (f == null || f.isEmpty()) {
            return false;
        }
        return f.filter(r);
    }
}