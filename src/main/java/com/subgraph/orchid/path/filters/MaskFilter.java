package com.subgraph.orchid.path.filters;

import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public class MaskFilter implements RouterFilter {
    private final byte[] networkBytes;
    private final byte[] maskBytes;

    public MaskFilter(@NotNull InetAddress network, int bits) {
        this.networkBytes = network.getAddress();
        this.maskBytes = createMaskBytes(bits);
        for (int i = 0; i < networkBytes.length; i++) {
            networkBytes[i] &= maskBytes[i];
        }
    }

    @Contract(pure = true)
    private static byte @NotNull [] createMaskBytes(int bits) {
        byte[] mask = new byte[4];
        for (int i = 0; i < bits; i++) {
            mask[i / 8] |= (byte) (1 << (7 - (i % 8)));
        }
        return mask;
    }

    @Override
    public boolean filter(@NotNull Router router) {
        byte[] routerBytes = router.getAddress().getAddress();
        for (int i = 0; i < 4; i++) {
            if ((routerBytes[i] & maskBytes[i]) != networkBytes[i]) {
                return false;
            }
        }
        return true;
    }
}