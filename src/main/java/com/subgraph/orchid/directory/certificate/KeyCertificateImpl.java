package com.subgraph.orchid.directory.certificate;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.data.HexDigest;

public class KeyCertificateImpl implements KeyCertificate {
    private InetAddress directoryAddress;
    private int directoryPort;
    private HexDigest fingerprint;
    private TorPublicKey identityKey;
    private Instant keyPublished;
    private Instant keyExpires;
    private TorPublicKey signingKey;
    private String rawDocumentData;

    private boolean hasValidSignature = false;

    public void setDirectoryPort(int port) {
        this.directoryPort = port;
    }

    public void setDirectoryAddress(InetAddress address) {
        this.directoryAddress = address;
    }

    public void setAuthorityFingerprint(HexDigest fingerprint) {
        this.fingerprint = fingerprint;
    }

    public void setAuthorityIdentityKey(TorPublicKey key) {
        this.identityKey = key;
    }

    public void setAuthoritySigningKey(TorPublicKey key) {
        this.signingKey = key;
    }

    public void setKeyPublishedTime(Instant time) {
        this.keyPublished = time;
    }

    public void setKeyExpiryTime(Instant time) {
        this.keyExpires = time;
    }

    public void setValidSignature() {
        hasValidSignature = true;
    }

    public void setRawDocumentData(String rawData) {
        rawDocumentData = rawData;
    }

    @Override
    public boolean isValidDocument() {
        return hasValidSignature && (fingerprint != null) && (identityKey != null) && (keyPublished != null) && (keyExpires != null) && (signingKey != null);
    }

    @Override
    public InetAddress getDirectoryAddress() {
        return directoryAddress;
    }

    @Override
    public int getDirectoryPort() {
        return directoryPort;
    }

    @Override
    public HexDigest getAuthorityFingerprint() {
        return fingerprint;
    }

    @Override
    public TorPublicKey getAuthorityIdentityKey() {
        return identityKey;
    }

    @Override
    public TorPublicKey getAuthoritySigningKey() {
        return signingKey;
    }

    @Override
    public Instant getKeyPublishedTime() {
        return keyPublished;
    }

    @Override
    public Instant getKeyExpiryTime() {
        return keyExpires;
    }

    @Override
    public boolean isExpired() {
        return keyExpires != null && Instant.now().isAfter(keyExpires);
    }

    @Override
    public String getRawDocumentData() {
        return rawDocumentData;
    }

    @Override
    public ByteBuffer getRawDocumentBytes() {
        return getRawDocumentData() == null ? ByteBuffer.allocate(0) : ByteBuffer.wrap(getRawDocumentData().getBytes(Tor.getDefaultCharset()));
    }

    @Override
    public String toString() {
        return "KeyCertificateImpl{" +
                "directoryAddress=" + directoryAddress +
                ", directoryPort=" + directoryPort +
                ", fingerprint=" + fingerprint +
                ", identityKey=" + identityKey +
                ", keyPublished=" + keyPublished +
                ", keyExpires=" + keyExpires +
                ", signingKey=" + signingKey +
                ", rawDocumentData='" + rawDocumentData + '\'' +
                ", hasValidSignature=" + hasValidSignature +
                '}';
    }
}