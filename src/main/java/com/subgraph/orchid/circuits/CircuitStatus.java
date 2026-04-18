package com.subgraph.orchid.circuits;

import com.subgraph.orchid.crypto.TorRandom;

public class CircuitStatus {
    public enum CircuitState {
        UNCONNECTED("Unconnected"),
        BUILDING("Building"),
        FAILED("Failed"),
        OPEN("Open"),
        DESTROYED("Destroyed");
        private final String name;

        CircuitState(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private long timestampCreated;
    private long timestampDirty;
    private int currentStreamId;
    private volatile CircuitState state = CircuitState.UNCONNECTED;

    public CircuitStatus() {
        initializeCurrentStreamId();
    }

    private void initializeCurrentStreamId() {
        currentStreamId = TorRandom.nextInt(0xFFFF) + 1;
    }

    public synchronized void updateCreatedTimestamp() {
        timestampCreated = System.currentTimeMillis();
        timestampDirty = 0;
    }

    public synchronized void updateDirtyTimestamp() {
        if (timestampDirty == 0 && state != CircuitState.BUILDING) {
            timestampDirty = System.currentTimeMillis();
        }
    }

    public synchronized long getMillisecondsElapsedSinceCreated() {
        return millisecondsElapsedSince(timestampCreated);
    }

    public synchronized long getMillisecondsDirty() {
        return millisecondsElapsedSince(timestampDirty);
    }

    private static long millisecondsElapsedSince(long then) {
        if (then == 0) {
            return 0;
        }
        long now = System.currentTimeMillis();
        return now - then;
    }

    public synchronized boolean isDirty() {
        return timestampDirty != 0;
    }

    public void setStateBuilding() {
        state = CircuitState.BUILDING;
    }

    public void setStateFailed() {
        state = CircuitState.FAILED;
    }

    public void setStateOpen() {
        state = CircuitState.OPEN;
    }

    public void setStateDestroyed() {
        state = CircuitState.DESTROYED;
    }

    public boolean isBuilding() {
        return state == CircuitState.BUILDING;
    }

    public boolean isConnected() {
        return state == CircuitState.OPEN;
    }

    public boolean isUnconnected() {
        return state == CircuitState.UNCONNECTED;
    }

    public synchronized int nextStreamId() {
        currentStreamId++;
        if (currentStreamId > 0xFFFF) {
            currentStreamId = 1;
        }
        return currentStreamId;
    }
}