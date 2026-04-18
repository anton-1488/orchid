package com.subgraph.orchid;

import com.subgraph.orchid.router.Router;

import java.time.Instant;

public interface GuardEntry {
    boolean isAdded();

    void markAsDown();

    void clearDownSince();

    String getNickname();

    String getIdentity();

    String writeToString();

    String getVersion();

    Instant getCreatedTime();

    Instant getDownSince();

    Instant getLastConnectAttempt();

    Instant getUnlistedSince();

    boolean testCurrentlyUsable();

    Router getRouterForEntry();
}