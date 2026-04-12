package com.subgraph.orchid.crypto;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.exceptions.TorException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.StringReader;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * This class wraps the RSA public keys used in the Tor protocol.
 */
public class TorPublicKey {
    private final String pemBuffer;
    private RSAPublicKey key;

    private byte[] rawKeyBytes = null;
    private HexDigest keyFingerprint = null;

    public TorPublicKey(String pemBuffer) {
        this.pemBuffer = pemBuffer;
        this.key = null;
    }

    public TorPublicKey(RSAPublicKey key) {
        this.pemBuffer = null;
        this.key = key;
    }

    private synchronized RSAPublicKey getKey() {
        if (key == null && pemBuffer != null) {
            try (PEMParser parser = new PEMParser(new StringReader(pemBuffer))) {
                Object obj = parser.readObject();
                if (obj instanceof PEMKeyPair keyPair) {
                    PublicKey pubKey = new JcaPEMKeyConverter().getKeyPair(keyPair).getPublic();
                    key = (RSAPublicKey) pubKey;
                }
            } catch (Exception e) {
                throw new TorException("Failed to parse PEM", e);
            }
        }
        return key;
    }

    public synchronized byte[] getRawBytes() {
        if (rawKeyBytes == null) {
            rawKeyBytes = getKey().getEncoded();
        }
        return rawKeyBytes.clone();
    }

    public synchronized HexDigest getFingerprint() {
        if (keyFingerprint == null) {
            keyFingerprint = HexDigest.createDigestForData(getRawBytes());
        }
        return keyFingerprint;
    }

    public boolean verifySignature(TorSignature signature, HexDigest digest) {
        return verifySignatureFromDigestBytes(signature, digest.getRawBytes());
    }

    public boolean verifySignature(TorSignature signature, TorMessageDigest digest) {
        return verifySignatureFromDigestBytes(signature, digest.getDigestBytes());
    }

    public boolean verifySignatureFromDigestBytes(TorSignature signature, byte[] digestBytes) {
        Cipher cipher = createCipherInstance();
        try {
            byte[] decrypted = cipher.doFinal(signature.getSignatureBytes());
            return Arrays.equals(decrypted, digestBytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new TorException(e);
        }
    }

    private Cipher createCipherInstance() {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            return cipher;
        } catch (Exception e) {
            throw new TorException(e);
        }
    }

    public RSAPublicKey getRSAPublicKey() {
        return getKey();
    }

    @Override
    public String toString() {
        return "Tor Public Key: " + getFingerprint();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TorPublicKey other)) {
            return false;
        }
        return other.getFingerprint().equals(getFingerprint());
    }

    @Override
    public int hashCode() {
        return getFingerprint().hashCode();
    }
}