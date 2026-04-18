package com.subgraph.orchid.events;

import com.subgraph.orchid.BootstrapStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TorInitializationTracker {
    private static final Logger log = LoggerFactory.getLogger(TorInitializationTracker.class);
    private final List<TorInitializationListener> listeners = new CopyOnWriteArrayList<>();
    private BootstrapStatus bootstrapState = BootstrapStatus.STARTING;

    public void addListener(TorInitializationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TorInitializationListener listener) {
        listeners.remove(listener);
    }

    public BootstrapStatus getBootstrapState() {
        return bootstrapState;
    }

    public synchronized void start() {
        bootstrapState = BootstrapStatus.STARTING;
        notifyListeners(BootstrapStatus.STARTING);
    }

    public void notifyEvent(@NotNull BootstrapStatus status) {
        if (status.getPercent() <= bootstrapState.getPercent() || status.getPercent() > 100) {
            log.warn("Invalid status code {}", status.getPercent());
            return;
        }
        bootstrapState = status;
        notifyListeners(status);
    }

    private void notifyListeners(BootstrapStatus status) {
        for (TorInitializationListener listener : listeners) {
            try {
                listener.initializationProgress(status);
            } catch (Exception e) {
                log.error("Exception occurred in TorInitializationListener callback: {}", e.getMessage(), e);
            }
        }
    }
}