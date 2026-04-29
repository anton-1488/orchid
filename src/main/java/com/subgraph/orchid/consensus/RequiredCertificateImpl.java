package com.subgraph.orchid.consensus;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.document.ConsensusDocument;

public class RequiredCertificateImpl implements ConsensusDocument.RequiredCertificate {
    private final HexDigest identity;
    private final HexDigest signingKey;
    private int downloadFailureCount;

    public RequiredCertificateImpl(HexDigest identity, HexDigest signingKey) {
        this.identity = identity;
        this.signingKey = signingKey;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((identity == null) ? 0 : identity.hashCode());
        result = prime * result + ((signingKey == null) ? 0 : signingKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        RequiredCertificateImpl other = (RequiredCertificateImpl) obj;
        if (identity == null) {
            if (other.identity != null) {
                return false;
            }
        } else if (!identity.equals(other.identity))
            return false;
        if (signingKey == null) {
            return other.signingKey == null;
        } else return signingKey.equals(other.signingKey);
    }

    @Override
    public void incrementDownloadFailureCount() {
        downloadFailureCount += 1;
    }

    @Override
    public int getDownloadFailureCount() {
        return downloadFailureCount;
    }

    @Override
    public HexDigest getAuthorityIdentity() {
        return identity;
    }

    @Override
    public HexDigest getSigningKey() {
        return signingKey;
    }
}