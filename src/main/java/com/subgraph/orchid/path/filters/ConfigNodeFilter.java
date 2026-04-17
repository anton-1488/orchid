package com.subgraph.orchid.path.filters;

import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements configuration options:
 * <p>
 * ExcludeNodes, ExcludeExitNodes, ExitNodes, EntryNodes
 */
public class ConfigNodeFilter implements RouterFilter {
    private final List<RouterFilter> filterList;

    private ConfigNodeFilter(List<RouterFilter> filterList) {
        this.filterList = filterList;
    }

    @Contract("_ -> new")
    public static @NotNull ConfigNodeFilter createFromStrings(@NotNull List<String> stringList) {
        List<RouterFilter> filters = new ArrayList<>();
        for (String s : stringList) {
            RouterFilter f = FilterFactory.createFilterFor(s);
            if (f != null) {
                filters.add(f);
            }
        }
        return new ConfigNodeFilter(filters);
    }

    @Override
    public boolean filter(Router router) {
        for (RouterFilter f : filterList) {
            if (f.filter(router)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return filterList.isEmpty();
    }
}