package com.subgraph.orchid.path.filters;

import com.subgraph.orchid.router.Router;

@FunctionalInterface
public interface RouterFilter {
    boolean filter(Router router);
}