package com.subgraph.orchid.data.exitpolicy;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * Used by router status entries in consensus documents
 */
public class ExitPorts {
    public static @NotNull ExitPorts createAcceptExitPorts(String ports) {
        ExitPorts exitPorts = new ExitPorts(true);
        exitPorts.parsePortRanges(ports);
        return exitPorts;
    }

    public static @NotNull ExitPorts createRejectExitPorts(String ports) {
        ExitPorts exitPorts = new ExitPorts(false);
        exitPorts.parsePortRanges(ports);
        return exitPorts;
    }

    private final List<PortRange> ranges = new ArrayList<>();
    private final boolean areAcceptPorts;

    private ExitPorts(boolean acceptPorts) {
        this.areAcceptPorts = acceptPorts;
    }

    public boolean areAcceptPorts() {
        return areAcceptPorts;
    }

    public boolean acceptsPort(int port) {
        if (areAcceptPorts) {
            return contains(port);
        } else {
            return !contains(port);
        }
    }

    public boolean contains(int port) {
        for (PortRange r : ranges) {
            if (r.rangeContains(port)) {
                return true;
            }
        }
        return false;
    }

    private void parsePortRanges(@NotNull String portRanges) {
        String[] args = portRanges.split(",");
        for (String arg : args) {
            ranges.add(PortRange.createFromString(arg));
        }
    }
}