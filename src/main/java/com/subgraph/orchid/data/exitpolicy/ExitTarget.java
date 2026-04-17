package com.subgraph.orchid.data.exitpolicy;

import java.net.InetAddress;

public interface ExitTarget {
    boolean isAddressTarget();

    InetAddress getAddress();

    String getHostname();

    int getPort();
}