package com.subgraph.orchid.router;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.exitpolicy.ExitPorts;

import java.net.InetAddress;
import java.time.Instant;

public interface RouterStatus {
    String getNickname();

    HexDigest getIdentity();

    HexDigest getDescriptorDigest();

    HexDigest getMicrodescriptorDigest();

    Instant getPublicationTime();

    InetAddress getAddress();

    int getRouterPort();

    boolean isDirectory();

    int getDirectoryPort();

    boolean hasFlag(String flag);

    String getVersion();

    boolean hasBandwidth();

    int getEstimatedBandwidth();

    int getMeasuredBandwidth();

    ExitPorts getExitPorts();
}