package com.subgraph.orchid.crypto;

import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

import java.security.SecureRandom;

public final class Curve25519 {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Curve25519() {
    }

    /**
     * Генерация секретного ключа (32 байта)
     */
    public static byte[] generateSecretKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        key[0] &= (byte) 248;
        key[31] &= 127;
        key[31] |= 64;
        return key;
    }

    /**
     * Получение публичного ключа из секретного
     */
    public static byte[] getPublicKey(byte[] secretKey) {
        X25519PrivateKeyParameters privateKey = new X25519PrivateKeyParameters(secretKey, 0);
        X25519PublicKeyParameters publicKey = privateKey.generatePublicKey();
        return publicKey.getEncoded();
    }

    /**
     * ECDH: общий секрет из своего секретного и чужого публичного ключа
     */
    public static byte[] scalarMult(byte[] secretKey, byte[] peerPublicKey) {
        X25519PrivateKeyParameters privateKey = new X25519PrivateKeyParameters(secretKey, 0);
        X25519PublicKeyParameters publicKey = new X25519PublicKeyParameters(peerPublicKey, 0);

        X25519Agreement agreement = new X25519Agreement();
        agreement.init(privateKey);

        byte[] sharedSecret = new byte[32];
        agreement.calculateAgreement(publicKey, sharedSecret, 0);
        return sharedSecret;
    }

    /**
     * Обертка для совместимости со старым кодом
     */
    public static void crypto_scalarmult_base(byte[] out, byte[] secretKey) {
        byte[] pubKey = getPublicKey(secretKey);
        System.arraycopy(pubKey, 0, out, 0, 32);
    }

    /**
     * Обертка для совместимости со старым кодом
     */
    public static void crypto_scalarmult(byte[] out, byte[] secretKey, byte[] peerPublicKey) {
        byte[] shared = scalarMult(secretKey, peerPublicKey);
        System.arraycopy(shared, 0, out, 0, 32);
    }
}