package com.subgraph.orchid.crypto;

import com.subgraph.orchid.data.HexDigest;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class TorNTorKeyAgreement implements TorKeyAgreement {
    public static final int CURVE25519_PUBKEY_LEN = 32;
    private static final int CURVE25519_OUTPUT_LEN = 32;
    private static final int DIGEST256_LEN = 32;
    private static final int DIGEST_LEN = 20;
    private static final int NTOR_ONIONSKIN_LEN = 2 * CURVE25519_PUBKEY_LEN + DIGEST_LEN;
    private static final String PROTOID = "ntor-curve25519-sha256-1";
    private static final String SERVER_STR = "Server";
    private static final int SECRET_INPUT_LEN = CURVE25519_PUBKEY_LEN * 3 + CURVE25519_OUTPUT_LEN * 2 + DIGEST_LEN + PROTOID.length();
    private static final int AUTH_INPUT_LEN = DIGEST256_LEN + DIGEST_LEN + (CURVE25519_PUBKEY_LEN * 3) + PROTOID.length() + SERVER_STR.length();
    private static final Charset cs = StandardCharsets.ISO_8859_1;

    private final HexDigest peerIdentity;
    private final byte[] peerNTorOnionKey;  /* pubkey_B */
    private final byte[] secretKey_x;
    private final byte[] publicKey_X;
    private boolean isBad;

    public TorNTorKeyAgreement(HexDigest peerIdentity, byte[] peerNTorOnionKey) {
        this.peerIdentity = peerIdentity;
        this.peerNTorOnionKey = peerNTorOnionKey;
        this.secretKey_x = generateSecretKey();
        this.publicKey_X = getPublicKeyForPrivate(secretKey_x);
    }

    @Override
    public byte[] createOnionSkin() {
        ByteBuffer buffer = makeBuffer(NTOR_ONIONSKIN_LEN);
        buffer.put(peerIdentity.getRawBytes());
        buffer.put(peerNTorOnionKey);
        buffer.put(publicKey_X);
        return buffer.array();
    }

    private @NotNull ByteBuffer makeBuffer(int sz) {
        byte[] array = new byte[sz];
        return ByteBuffer.wrap(array);
    }

    public byte[] generateSecretKey() {
        byte[] key = TorRandom.getBytes(32);
        key[0] &= (byte) 248;
        key[31] &= 127;
        key[31] |= 64;
        return key;
    }

    public byte[] getPublicKeyForPrivate(byte[] secretKey) {
        byte[] pub = new byte[32];
        Curve25519.crypto_scalarmult_base(pub, secretKey);
        return pub;
    }

    @Override
    public boolean deriveKeysFromHandshakeResponse(byte[] handshakeResponse, byte[] keyMaterialOut, byte[] verifyHashOut) {
        isBad = false;

        ByteBuffer hr = ByteBuffer.wrap(handshakeResponse);
        byte[] serverPub = new byte[CURVE25519_PUBKEY_LEN];
        byte[] authCandidate = new byte[DIGEST256_LEN];
        hr.get(serverPub);
        hr.get(authCandidate);

        byte[] secretInput = buildSecretInput(serverPub);
        byte[] verify = tweak("verify", secretInput);
        byte[] authInput = buildAuthInput(verify, serverPub);
        byte[] auth = tweak("mac", authInput);
        isBad |= !Arrays.equals(auth, authCandidate);
        byte[] seed = tweak("key_extract", secretInput);

        TorRFC5869KeyDerivation kdf = new TorRFC5869KeyDerivation(seed);
        kdf.deriveKeys(keyMaterialOut, verifyHashOut);

        return !isBad;
    }

    public byte[] getNtorCreateMagic() {
        return "ntorNTORntorNTOR".getBytes(cs);
    }

    private byte @NotNull [] buildSecretInput(byte[] serverPublic_Y) {
        ByteBuffer bb = makeBuffer(SECRET_INPUT_LEN);
        bb.put(scalarMult(serverPublic_Y));
        bb.put(scalarMult(peerNTorOnionKey));
        bb.put(peerIdentity.getRawBytes());
        bb.put(peerNTorOnionKey);
        bb.put(publicKey_X);
        bb.put(serverPublic_Y);
        bb.put(PROTOID.getBytes());
        return bb.array();
    }

    private byte @NotNull [] buildAuthInput(byte[] verify, byte[] serverPublic_Y) {
        ByteBuffer bb = makeBuffer(AUTH_INPUT_LEN);
        bb.put(verify);
        bb.put(peerIdentity.getRawBytes());
        bb.put(peerNTorOnionKey);
        bb.put(serverPublic_Y);
        bb.put(publicKey_X);
        bb.put(PROTOID.getBytes(cs));
        bb.put(SERVER_STR.getBytes(cs));
        return bb.array();
    }

    private byte @NotNull [] scalarMult(byte[] peerValue) {
        byte[] out = new byte[CURVE25519_OUTPUT_LEN];
        Curve25519.crypto_scalarmult(out, secretKey_x, peerValue);
        isBad |= isAllZero(out);
        return out;
    }

    public boolean isAllZero(byte @NotNull [] bs) {
        boolean result = true;
        for (byte b : bs) {
            result &= (b == 0);
        }
        return result;
    }

    public byte[] tweak(String suffix, byte[] input) {
        return hmac256(input, getStringConstant(suffix));
    }

    public static byte[] hmac256(byte[] input, byte[] key) {
        SecretKeySpec keyspec = new SecretKeySpec(key, "HmacSHA256");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyspec);
            return mac.doFinal(input);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to create HmacSHA256 instance: " + e);
        }
    }

    public byte[] getStringConstant(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return PROTOID.getBytes(cs);
        } else {
            return (PROTOID + ":" + suffix).getBytes(cs);
        }
    }
}