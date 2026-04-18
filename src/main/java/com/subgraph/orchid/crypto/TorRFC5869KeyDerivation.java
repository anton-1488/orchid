package com.subgraph.orchid.crypto;

import com.subgraph.orchid.Tor;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class TorRFC5869KeyDerivation {
    private final static String PROTOID = "ntor-curve25519-sha256-1";
    private final static String M_EXPAND = PROTOID + ":key_expand";
    private final static byte[] M_EXPAND_BYTES = M_EXPAND.getBytes(Tor.getDefaultCharset());

    private final byte[] seed;

    public TorRFC5869KeyDerivation(byte @NotNull [] seed) {
        this.seed = new byte[seed.length];
        System.arraycopy(seed, 0, this.seed, 0, seed.length);
    }

    public void deriveKeys(byte @NotNull [] keyMaterialOut, byte @NotNull [] verifyHashOut) {
        ByteBuffer keyData = deriveKeys(keyMaterialOut.length + verifyHashOut.length);
        keyData.get(keyMaterialOut);
        keyData.get(verifyHashOut);
    }

    public ByteBuffer deriveKeys(int length) {
        int round = 1;
        ByteBuffer bb = makeBuffer(length);
        byte[] macOutput = null;
        while (bb.hasRemaining()) {
            macOutput = expandRound(round, macOutput);
            if (macOutput.length > bb.remaining()) {
                bb.put(macOutput, 0, bb.remaining());
            } else {
                bb.put(macOutput);
            }
            round += 1;
        }
        bb.flip();
        return bb;
    }

    private byte[] expandRound(int round, byte[] priorMac) {
        ByteBuffer bb;
        if (round == 1) {
            bb = makeBuffer(M_EXPAND_BYTES.length + 1);
        } else {
            bb = makeBuffer(M_EXPAND_BYTES.length + TorMessageDigest.TOR_DIGEST256_SIZE + 1);
            bb.put(priorMac);
        }
        bb.put(M_EXPAND_BYTES);
        bb.put((byte) round);

        Mac mac = createMacInstance();
        return mac.doFinal(bb.array());
    }

    private @NotNull ByteBuffer makeBuffer(int len) {
        byte[] bs = new byte[len];
        return ByteBuffer.wrap(bs);
    }

    private @NotNull Mac createMacInstance() {
        SecretKeySpec keyspec = new SecretKeySpec(seed, "HmacSHA256");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Could not create HmacSHA256 instance: " + e);
        }
    }
}