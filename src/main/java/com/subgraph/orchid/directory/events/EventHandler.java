package com.subgraph.orchid.directory.events;

@FunctionalInterface
public interface EventHandler {
	void handleEvent(Event event);
}