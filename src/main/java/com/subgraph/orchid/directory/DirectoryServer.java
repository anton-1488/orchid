package com.subgraph.orchid.directory;

import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.router.Router;

import java.util.List;

/**
 * Represents a directory authority server or a directory cache.
 */
public interface DirectoryServer extends Router {
    int getDirectoryPort();

    boolean isV3Authority();

    HexDigest getV3Identity();

    boolean isHiddenServiceAuthority();

    boolean isBridgeAuthority();

    boolean isExtraInfoCache();

    KeyCertificate getCertificateByFingerprint(HexDigest fingerprint);

    List<KeyCertificate> getCertificates();

    void addCertificate(KeyCertificate certificate);
}