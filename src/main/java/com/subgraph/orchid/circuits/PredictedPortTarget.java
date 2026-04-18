package com.subgraph.orchid.circuits;

import com.subgraph.orchid.data.InetAddressUtils;
import com.subgraph.orchid.data.exitpolicy.ExitTarget;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public record PredictedPortTarget(int port) implements ExitTarget {
    @Override
    public boolean isAddressTarget() {
        return false;
    }

    @Override
    @Contract(" -> new")
    public InetAddress getAddress() {
        return InetAddressUtils.createAddressFromString("0.0.0.0");
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getHostname() {
        return "";
    }
}