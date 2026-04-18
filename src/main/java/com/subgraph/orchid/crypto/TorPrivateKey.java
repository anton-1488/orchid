package com.subgraph.orchid.crypto;

import com.subgraph.orchid.exceptions.TorException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class TorPrivateKey {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Contract(" -> new")
    public static @NotNull TorPrivateKey generateNewKeypair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, RANDOM);
            KeyPair pair = generator.generateKeyPair();
            return new TorPrivateKey((RSAPrivateKey) pair.getPrivate(), (RSAPublicKey) pair.getPublic());
        } catch (Exception e) {
            throw new TorException(e);
        }
    }

    private final TorPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    TorPrivateKey(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = new TorPublicKey(publicKey);
    }

    public TorPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPublicKey getRSAPublicKey() {
        return publicKey.getRSAPublicKey();
    }

    public RSAPrivateKey getRSAPrivateKey() {
        return privateKey;
    }
}