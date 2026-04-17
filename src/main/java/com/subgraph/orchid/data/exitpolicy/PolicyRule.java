package com.subgraph.orchid.data.exitpolicy;

import com.subgraph.orchid.exceptions.TorParsingException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public class PolicyRule {
    public static final String WILDCARD = "*";

    @Contract("_ -> new")
    public static @NotNull PolicyRule createAcceptFromString(String rule) {
        return createRule(rule, true);
    }

    @Contract("_ -> new")
    public static @NotNull PolicyRule createRejectFromString(String rule) {
        return createRule(rule, false);
    }

    @Contract("_, _ -> new")
    private static @NotNull PolicyRule createRule(@NotNull String rule, boolean isAccept) {
        String[] args = rule.split(":");
        if (args.length != 2) {
            throw new TorParsingException("Could not parse exit policy rule: " + rule);
        }
        return new PolicyRule(parseNetwork(args[0]), parsePortRange(args[1]), isAccept);
    }

    private static Network parseNetwork(@NotNull String network) {
        if (network.equals(WILDCARD)) {
            return Network.ALL_ADDRESSES;
        } else {
            return Network.createFromString(network);
        }
    }

    private static PortRange parsePortRange(@NotNull String portRange) {
        if (portRange.equals(WILDCARD)) {
            return PortRange.ALL_PORTS;
        } else {
            return PortRange.createFromString(portRange);
        }
    }

    private final boolean isAcceptRule;
    private final Network network;
    private final PortRange portRange;

    private PolicyRule(Network network, PortRange portRange, boolean isAccept) {
        this.network = network;
        this.portRange = portRange;
        this.isAcceptRule = isAccept;
    }

    public boolean matchesPort(int port) {
        if (!network.equals(Network.ALL_ADDRESSES)) {
            return false;
        }
        return portRange.rangeContains(port);
    }

    public boolean matchesDestination(InetAddress address, int port) {
        if (!network.contains(address)) {
            return false;
        }
        return portRange.rangeContains(port);
    }

    public boolean isAcceptRule() {
        return isAcceptRule;
    }

    @Override
    public String toString() {
        String keyword = isAcceptRule ? "accept" : "reject";
        return keyword + " " + network + ":" + portRange;
    }
}