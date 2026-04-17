package com.subgraph.orchid.events;

import com.subgraph.orchid.Globals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class EventManager {
    private static final ExecutorService eventExecutor = Globals.VIRTUAL_EXECUTOR;
    private final List<EventHandler> handlers;

    public EventManager() {
        handlers = new CopyOnWriteArrayList<>();
    }

    public void addListener(EventHandler listener) {
        handlers.add(Objects.requireNonNull(listener));
    }

    public void removeListener(EventHandler listener) {
        handlers.remove(Objects.requireNonNull(listener));
    }

    public void fireEvent(Event event) {
        Objects.requireNonNull(event);

        for (EventHandler handler : handlers) {
            eventExecutor.execute(() -> handler.handleEvent(event));
        }
    }
}