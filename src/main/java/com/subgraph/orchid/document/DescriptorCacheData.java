package com.subgraph.orchid.document;

import com.subgraph.orchid.data.HexDigest;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DescriptorCacheData<T extends Descriptor> {
    /**
     * 7 days
     */
    private final static long EXPIRY_PERIOD = TimeUnit.DAYS.toMillis(7);

    private final Map<HexDigest, T> descriptorMap;
    private final List<T> allDescriptors;

    public DescriptorCacheData() {
        this.descriptorMap = new HashMap<>();
        this.allDescriptors = new ArrayList<>();
    }

    public synchronized T findByDigest(HexDigest digest) {
        return descriptorMap.get(digest);
    }

    public synchronized List<T> getAllDescriptors() {
        return new ArrayList<>(allDescriptors);
    }

    public synchronized boolean addDescriptor(@NotNull T d) {
        if (descriptorMap.containsKey(d.getDescriptorDigest())) {
            return false;
        }
        descriptorMap.put(d.getDescriptorDigest(), d);
        allDescriptors.add(d);
        return true;
    }

    public synchronized void clear() {
        descriptorMap.clear();
        allDescriptors.clear();
    }

    public synchronized int cleanExpired() {
        Set<T> expired = getExpiredSet();
        if (expired.isEmpty()) {
            return 0;
        }

        clear();
        int dropped = 0;
        for (T d : allDescriptors) {
            if (expired.contains(d)) {
                dropped += d.getBodyLength();
            } else {
                addDescriptor(d);
            }
        }

        return dropped;
    }

    private @NotNull Set<T> getExpiredSet() {
        long now = System.currentTimeMillis();
        Set<T> expired = new HashSet<>();
        for (T d : allDescriptors) {
            if (isExpired(d, now)) {
                expired.add(d);
            }
        }
        return expired;
    }

    private boolean isExpired(@NotNull T d, long now) {
        return d.getLastListed() != 0 && d.getLastListed() < (now - EXPIRY_PERIOD);
    }
}