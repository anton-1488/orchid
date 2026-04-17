package com.subgraph.orchid.directory;

import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.router.RouterImpl;
import com.subgraph.orchid.router.RouterStatus;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryServerImpl extends RouterImpl implements DirectoryServer {
    private final List<KeyCertificate> certificates = new CopyOnWriteArrayList<>();

    private boolean isHiddenServiceAuthority = false;
    private boolean isBridgeAuthority = false;
    private boolean isExtraInfoCache = false;
    private int port;
    private HexDigest v3Ident;

    public DirectoryServerImpl(RouterStatus status) {
        super(null, status);
    }

    public void setHiddenServiceAuthority() {
        isHiddenServiceAuthority = true;
    }

    public void unsetHiddenServiceAuthority() {
        isHiddenServiceAuthority = false;
    }

    public void setBridgeAuthority() {
        isBridgeAuthority = true;
    }

    public void setExtraInfoCache() {
        isExtraInfoCache = true;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setV3Ident(HexDigest fingerprint) {
        this.v3Ident = fingerprint;
    }

    public boolean isTrustedAuthority() {
        return true;
    }

    /**
     * Return true if this DirectoryServer entry has
     * complete and valid information.
     */
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean exitPolicyAccepts(InetAddress address, int port) {
        return false;
    }

    @Override
    public boolean isV3Authority() {
        return hasFlag("Authority") && v3Ident != null;
    }

    @Override
    public boolean isHiddenServiceAuthority() {
        return isHiddenServiceAuthority;
    }

    public boolean isBridgeAuthority() {
        return isBridgeAuthority;
    }

    @Override
    public boolean isExtraInfoCache() {
        return isExtraInfoCache;
    }

    @Override
    public HexDigest getV3Identity() {
        return v3Ident;
    }

    @Override
    public KeyCertificate getCertificateByFingerprint(HexDigest fingerprint) {
        for (KeyCertificate kc : getCertificates()) {
            if (kc.getAuthoritySigningKey().getFingerprint().equals(fingerprint)) {
                return kc;
            }
        }
        return null;
    }

    @Override
    public synchronized List<KeyCertificate> getCertificates() {
        purgeExpiredCertificates();
        purgeOldCertificates();
        return List.copyOf(certificates);
    }

    private void purgeExpiredCertificates() {
        certificates.removeIf(KeyCertificate::isExpired);
    }

    private void purgeOldCertificates() {
        if (certificates.size() < 2) {
            return;
        }
        KeyCertificate newest = getNewestCertificate();
        certificates.removeIf(elem -> elem != newest && isMoreThan48HoursOlder(newest, elem));
    }

    private KeyCertificate getNewestCertificate() {
        KeyCertificate newest = null;
        for (KeyCertificate kc : certificates) {
            if (newest == null || getPublishedMilliseconds(newest) > getPublishedMilliseconds(kc)) {
                newest = kc;
            }
        }
        return newest;
    }

    private boolean isMoreThan48HoursOlder(KeyCertificate newer, KeyCertificate older) {
        long milliseconds = 48 * 60 * 60 * 1000;
        return (getPublishedMilliseconds(newer) - getPublishedMilliseconds(older)) > milliseconds;
    }

    private long getPublishedMilliseconds(@NotNull KeyCertificate certificate) {
        return certificate.getKeyPublishedTime().toEpochMilli();
    }

    @Override
    public void addCertificate(@NotNull KeyCertificate certificate) {
        if (!certificate.getAuthorityFingerprint().equals(v3Ident)) {
            throw new IllegalArgumentException("This certificate does not appear to belong to this directory authority");
        }
        certificates.add(certificate);
    }

    @Override
    public String toString() {
        return "DirectoryServerImpl{" +
                "certificates=" + certificates +
                ", isHiddenServiceAuthority=" + isHiddenServiceAuthority +
                ", isBridgeAuthority=" + isBridgeAuthority +
                ", isExtraInfoCache=" + isExtraInfoCache +
                ", port=" + port +
                ", v3Ident=" + v3Ident +
                ", status=" + status +
                '}';
    }
}