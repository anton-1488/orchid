package com.subgraph.orchid.circuits;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CircuitPredictor {
    private final static Integer INTERNAL_CIRCUIT_PORT_VALUE = 0;
    private final static long TIMEOUT_MS = TimeUnit.HOURS.toMillis(1);
    private final Map<Integer, Long> portsSeen = new ConcurrentHashMap<>();

    public CircuitPredictor() {
        addExitPortRequest(80);
        addInternalRequest();
    }

    public void addExitPortRequest(int port) {
        portsSeen.put(port, System.currentTimeMillis());
    }

    public void addInternalRequest() {
        addExitPortRequest(INTERNAL_CIRCUIT_PORT_VALUE);
    }

    @Contract(pure = true)
    private boolean isEntryExpired(@NotNull Entry<Integer, Long> e, long now) {
        return (now - e.getValue()) > TIMEOUT_MS;
    }

    private void removeExpiredPorts() {
        long now = System.currentTimeMillis();
        portsSeen.entrySet().removeIf(integerLongEntry -> isEntryExpired(integerLongEntry, now));
    }

    public synchronized boolean isInternalPredicted() {
        removeExpiredPorts();
        return portsSeen.containsKey(INTERNAL_CIRCUIT_PORT_VALUE);
    }

    public synchronized Set<Integer> getPredictedPorts() {
        removeExpiredPorts();
        Set<Integer> result = new HashSet<>(portsSeen.keySet());
        result.remove(INTERNAL_CIRCUIT_PORT_VALUE);
        return result;
    }

    public List<PredictedPortTarget> getPredictedPortTargets() {
        List<PredictedPortTarget> targets = new ArrayList<>();
        for (int p : getPredictedPorts()) {
            targets.add(new PredictedPortTarget(p));
        }
        return targets;
    }
}