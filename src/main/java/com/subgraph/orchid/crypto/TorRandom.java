package com.subgraph.orchid.crypto;

import java.security.SecureRandom;

public class TorRandom {
    private static final SecureRandom random = new SecureRandom();

    private TorRandom() {
        throw new UnsupportedOperationException();
    }

    public static byte[] getBytes(int length) {
        byte[] bs = new byte[length];
        random.nextBytes(bs);
        return bs;
    }

    public static long nextLong() {
        return random.nextLong(0, Long.MAX_VALUE);
    }
    public static long nextLong(long bound) {
        return random.nextLong(bound);
    }

    public static int nextInt(int n) {
        return random.nextInt(n);
    }

    public static int nextInt() {
        return random.nextInt(0, Integer.MAX_VALUE);
    }

    public static boolean nextBoolean() {
        return random.nextBoolean();
    }

    public static double nextDouble() {
        return random.nextDouble(0, Double.MAX_VALUE);
    }
}
