package com.subgraph.orchid.data.exitpolicy;


import com.subgraph.orchid.data.InetAddressUtils;
import com.subgraph.orchid.exceptions.TorParsingException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public class Network {
    public static final Network ALL_ADDRESSES = new Network(InetAddressUtils.createAddressFromString("0.0.0.0"), 0, "*");

    @Contract("_ -> new")
    public static @NotNull Network createFromString(@NotNull String networkString) {
        String[] parts = networkString.split("/");
        InetAddress network = InetAddressUtils.createAddressFromString(parts[0]);
        if (parts.length == 1) {
            return new Network(network, 32, networkString);
        }

        if (parts.length != 2) {
            throw new TorParsingException("Invalid network CIDR notation: " + networkString);
        }

        try {
            int maskBits = Integer.parseInt(parts[1]);
            return new Network(network, maskBits, networkString);
        } catch (NumberFormatException e) {
            throw new TorParsingException("Invalid netblock mask bit value: " + parts[1]);
        }
    }

    private final InetAddress network;
    private final int maskValue;
    private final String originalString;

    private Network(InetAddress network, int bits, String originalString) {
        this.network = network;
        this.maskValue = createMask(bits);
        this.originalString = originalString;
    }

    private static int createMask(int maskBits) {
        return maskBits == 0 ? 0 : (1 << 31) >> (maskBits - 1);
    }

    public boolean contains(@NotNull InetAddress address) {
        byte[] addrBytes = address.getAddress();
        byte[] netBytes = network.getAddress();

        int maskInt = maskValue;
        byte[] maskBytes = new byte[4];
        maskBytes[0] = (byte) ((maskInt >> 24) & 0xFF);
        maskBytes[1] = (byte) ((maskInt >> 16) & 0xFF);
        maskBytes[2] = (byte) ((maskInt >> 8) & 0xFF);
        maskBytes[3] = (byte) (maskInt & 0xFF);
        for (int i = 0; i < 4; i++) {
            if ((addrBytes[i] & maskBytes[i]) != (netBytes[i] & maskBytes[i])) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return originalString;
    }
}