package com.subgraph.orchid.events;

@FunctionalInterface
public interface EventHandler {
    void handleEvent(Event event);
}