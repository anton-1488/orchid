package com.subgraph.orchid.data;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.exceptions.TorException;
import org.bouncycastle.util.encoders.Base32;
import org.bouncycastle.util.encoders.Base64;

import java.util.Arrays;
import java.util.HexFormat;

/**
 * This class represents both digests and fingerprints that appear in directory
 * documents.  The names fingerprint and digest are used interchangeably in
 * the specification but generally a fingerprint is a message digest (ie: SHA1)
 * over the DER ASN.1 encoding of a public key.  A digest is usually
 * a message digest over a set of fields in a directory document.
 * <p>
 * Digests always appear as a 40 character hex string:
 * <p>
 * 0EA20CAA3CE696E561BC08B15E00106700E8F682
 * <p>
 * Fingerprints may either appear as a single hex string as above or sometimes in
 * a more easily human-parsed spaced format:
 * <p>
 * 1E0F 5874 2268 E82F C600 D81D 9064 07C5 7CC2 C3A7
 *
 */
public class HexDigest {
    private static final HexFormat HEX = HexFormat.of().withLowerCase();

    public static HexDigest createFromBase32String(String b32) {
        return new HexDigest(Base32.decode(b32));
    }

    public static HexDigest createFromString(String fingerprint) {
        try {
            String clean = fingerprint.replaceAll("\\s+", "");
            byte[] digestData = HEX.parseHex(clean);
            return new HexDigest(digestData);
        } catch (IllegalArgumentException e) {
            throw new TorException("Invalid hex string: " + fingerprint, e);
        }
    }

    public static HexDigest createFromDigestBytes(byte[] data) {
        return new HexDigest(data);
    }

    public static HexDigest createDigestForData(byte[] data) {
        final TorMessageDigest digest = new TorMessageDigest();
        digest.update(data);
        return new HexDigest(digest.getDigestBytes());
    }

    private final byte[] digestBytes;
    private final boolean isDigest256;

    private HexDigest(byte[] data) {
        if (data.length != TorMessageDigest.TOR_DIGEST_SIZE && data.length != TorMessageDigest.TOR_DIGEST256_SIZE) {
            throw new TorException("Digest data is not the correct length " + data.length + " != (" + TorMessageDigest.TOR_DIGEST_SIZE + " or " + TorMessageDigest.TOR_DIGEST256_SIZE + ")");
        }
        digestBytes = data.clone();
        isDigest256 = digestBytes.length == TorMessageDigest.TOR_DIGEST256_SIZE;
    }

    public boolean isDigest256() {
        return isDigest256;
    }

    public byte[] getRawBytes() {
        return digestBytes.clone();
    }

    public String toString() {
        return HEX.formatHex(digestBytes);
    }

    /**
     * Return a spaced fingerprint representation of this HexDigest.
     * <p>
     * ex:
     * 1E0F 5874 2268 E82F C600 D81D 9064 07C5 7CC2 C3A7
     * <p>
     *
     * @return A string representation of this HexDigest in the spaced fingerprint format.
     */
    public String toSpacedString() {
        return toString().replaceAll("(.{4})(?!$)", "$1 ").toUpperCase();
    }

    public String toBase32() {
        return new String(Base32.encode(digestBytes));
    }

    public String toBase64(boolean stripTrailingEquals) {
        String b64 = new String(Base64.encode(digestBytes), Tor.getDefaultCharset());

        if (stripTrailingEquals) {
            return stripTrailingEquals(b64);
        } else {
            return b64;
        }
    }

    private static String stripTrailingEquals(String s) {
        return s.replaceAll("=+$", "");
    }

    public boolean equals(Object o) {
        if (!(o instanceof HexDigest other)) return false;
        return Arrays.equals(other.digestBytes, this.digestBytes);
    }

    public int hashCode() {
        return Arrays.hashCode(digestBytes);
    }
}