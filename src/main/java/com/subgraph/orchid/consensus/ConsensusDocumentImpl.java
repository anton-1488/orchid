package com.subgraph.orchid.consensus;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.VoteAuthorityEntry;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.crypto.TorSignature.DigestAlgorithm;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.TrustedAuthorities;
import com.subgraph.orchid.directory.DirectoryServer;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.router.RouterStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConsensusDocumentImpl implements ConsensusDocument {
    private static final Logger log = LoggerFactory.getLogger(ConsensusDocumentImpl.class);

    public enum SignatureVerifyStatus {STATUS_UNVERIFIED, STATUS_NEED_CERTS, STATUS_VERIFIED}

    private final static String BW_WEIGHT_SCALE_PARAM = "bwweightscale";
    private final static int BW_WEIGHT_SCALE_DEFAULT = 10000;
    private final static int BW_WEIGHT_SCALE_MIN = 1;
    private final static int BW_WEIGHT_SCALE_MAX = Integer.MAX_VALUE;
    private final static String CIRCWINDOW_PARAM = "circwindow";
    private final static int CIRCWINDOW_DEFAULT = 1000;
    private final static int CIRCWINDOW_MIN = 100;
    private final static int CIRCWINDOW_MAX = 1000;
    private final static String USE_NTOR_HANDSHAKE_PARAM = "UseNTorHandshake";

    private final Set<RequiredCertificate> requiredCertificates = new HashSet<>();


    private int consensusMethod;
    private ConsensusFlavor flavor;
    private Instant validAfter;
    private Instant freshUntil;
    private Instant validUntil;
    private int distDelaySeconds;
    private int voteDelaySeconds;
    private final Set<String> clientVersions = new HashSet<>();
    private final Set<String> serverVersions = new HashSet<>();
    private final Set<String> knownFlags = new HashSet<>();
    private HexDigest signingHash;
    private HexDigest signingHash256;
    private final Map<HexDigest, VoteAuthorityEntry> voteAuthorityEntries = new ConcurrentHashMap<>();
    private final List<RouterStatus> routerStatusEntries = new ArrayList<>();
    private final Map<String, Integer> bandwidthWeights = new ConcurrentHashMap<>();
    private final Map<String, Integer> parameters = new ConcurrentHashMap<>();
    private int signatureCount;
    private boolean isFirstCallToVerifySignatures = true;
    private String rawDocumentData;

    public ConsensusDocumentImpl() {
    }

    public void setConsensusFlavor(ConsensusFlavor flavor) {
        this.flavor = flavor;
    }

    public void setConsensusMethod(int method) {
        consensusMethod = method;
    }

    public void setValidAfter(Instant ts) {
        validAfter = ts;
    }

    public void setFreshUntil(Instant ts) {
        freshUntil = ts;
    }

    public void setValidUntil(Instant ts) {
        validUntil = ts;
    }

    public void setDistDelaySeconds(int seconds) {
        distDelaySeconds = seconds;
    }

    public void setVoteDelaySeconds(int seconds) {
        voteDelaySeconds = seconds;
    }

    public void addClientVersion(String version) {
        clientVersions.add(version);
    }

    public void addServerVersion(String version) {
        serverVersions.add(version);
    }

    public void addParameter(String name, int value) {
        parameters.put(name, value);
    }

    public void addBandwidthWeight(String name, int value) {
        bandwidthWeights.put(name, value);
    }

    public void addSignature(@NotNull DirectorySignature signature) {
        VoteAuthorityEntry voteAuthority = voteAuthorityEntries.get(signature.identityDigest());
        if (voteAuthority == null) {
            log.warn("Consensus contains signature for source not declared in authority section: {}", signature.identityDigest());
            return;
        }
        List<DirectorySignature> signatures = voteAuthority.getSignatures();
        DigestAlgorithm newSignatureAlgorithm = signature.signature().getDigestAlgorithm();
        for (DirectorySignature sig : signatures) {
            DigestAlgorithm algo = sig.signature().getDigestAlgorithm();
            if (algo.equals(newSignatureAlgorithm)) {
                log.warn("Consensus contains two or more signatures for same source with same algorithm");
                return;
            }
        }
        signatureCount += 1;
        signatures.add(signature);
    }

    public void setSigningHash(HexDigest hash) {
        signingHash = hash;
    }

    public void setSigningHash256(HexDigest hash) {
        signingHash256 = hash;
    }

    public void setRawDocumentData(String rawData) {
        rawDocumentData = rawData;
    }

    public void addKnownFlag(String flag) {
        knownFlags.add(flag);
    }

    public void addVoteAuthorityEntry(VoteAuthorityEntry entry) {
        voteAuthorityEntries.put(entry.getIdentity(), entry);
    }

    public void addRouterStatusEntry(RouterStatusImpl entry) {
        routerStatusEntries.add(entry);
    }

    @Override
    public ConsensusFlavor getFlavor() {
        return flavor;
    }

    @Override
    public Instant getValidAfterTime() {
        return validAfter;
    }

    @Override
    public Instant getFreshUntilTime() {
        return freshUntil;
    }

    @Override
    public Instant getValidUntilTime() {
        return validUntil;
    }

    @Override
    public int getConsensusMethod() {
        return consensusMethod;
    }

    @Override
    public int getVoteSeconds() {
        return voteDelaySeconds;
    }

    @Override
    public int getDistSeconds() {
        return distDelaySeconds;
    }

    @Override
    public Set<String> getClientVersions() {
        return new HashSet<>(clientVersions);
    }

    @Override
    public Set<String> getServerVersions() {
        return new HashSet<>(serverVersions);
    }

    @Override
    public Set<RequiredCertificate> getRequiredCertificates() {
        return new HashSet<>(requiredCertificates);
    }

    @Override
    public boolean isLive() {
        if (validUntil == null) {
            return false;
        } else {
            return !validUntil.isBefore(Instant.now());
        }
    }

    @Override
    public List<RouterStatus> getRouterStatusEntries() {
        return List.copyOf(routerStatusEntries);
    }

    @Override
    public String getRawDocumentData() {
        return rawDocumentData;
    }

    @Override
    public ByteBuffer getRawDocumentBytes() {
        if (getRawDocumentData() == null) {
            return ByteBuffer.allocate(0);
        } else {
            return ByteBuffer.wrap(getRawDocumentData().getBytes(Tor.getDefaultCharset()));
        }
    }

    @Override
    public boolean isValidDocument() {
        return (validAfter != null) && (freshUntil != null) && (validUntil != null) && (voteDelaySeconds > 0) && (distDelaySeconds > 0) && (signingHash != null) && (signatureCount > 0);
    }

    @Override
    public HexDigest getSigningHash() {
        return signingHash;
    }

    @Override
    public HexDigest getSigningHash256() {
        return signingHash256;
    }

    @Override
    public int getCircWindowParameter() {
        return getParameterValue(CIRCWINDOW_PARAM, CIRCWINDOW_DEFAULT, CIRCWINDOW_MIN, CIRCWINDOW_MAX);
    }

    @Override
    public int getWeightScaleParameter() {
        return getParameterValue(BW_WEIGHT_SCALE_PARAM, BW_WEIGHT_SCALE_DEFAULT, BW_WEIGHT_SCALE_MIN, BW_WEIGHT_SCALE_MAX);
    }

    @Override
    public int getBandwidthWeight(String tag) {
        return bandwidthWeights.getOrDefault(tag, -1);
    }

    @Override
    public boolean getUseNTorHandshake() {
        return getBooleanParameterValue(USE_NTOR_HANDSHAKE_PARAM, false);
    }

    @Override
    public synchronized SignatureStatus verifySignatures() {
        boolean firstCall = isFirstCallToVerifySignatures;
        isFirstCallToVerifySignatures = false;
        requiredCertificates.clear();
        int verifiedCount = 0;
        int certsNeededCount = 0;
        int v3Count = TrustedAuthorities.getInstance().getV3AuthorityServerCount();
        int required = (v3Count / 2) + 1;

        for (VoteAuthorityEntry entry : voteAuthorityEntries.values()) {
            switch (verifySingleAuthority(entry)) {
                case STATUS_FAILED:
                    break;
                case STATUS_NEED_CERTS:
                    certsNeededCount += 1;
                    break;
                case STATUS_VERIFIED:
                    verifiedCount += 1;
                    break;
            }
        }

        if (verifiedCount >= required) {
            return SignatureStatus.STATUS_VERIFIED;
        } else if (verifiedCount + certsNeededCount >= required) {
            if (firstCall) {
                log.info("Certificates need to be retrieved to verify consensus");
            }
            return SignatureStatus.STATUS_NEED_CERTS;
        } else {
            return SignatureStatus.STATUS_FAILED;
        }
    }

    private SignatureStatus verifySingleAuthority(@NotNull VoteAuthorityEntry authority) {
        boolean certsNeeded = false;
        boolean validSignature = false;

        for (DirectorySignature s : authority.getSignatures()) {
            DirectoryServer trusted = TrustedAuthorities.getInstance().getAuthorityServerByIdentity(s.identityDigest());
            if (trusted == null) {
                log.warn("Consensus signed by unrecognized directory authority: {}", s.identityDigest());
                return SignatureStatus.STATUS_FAILED;
            } else {
                switch (verifySignatureForTrustedAuthority(trusted, s)) {
                    case STATUS_NEED_CERTS:
                        certsNeeded = true;
                        break;
                    case STATUS_VERIFIED:
                        validSignature = true;
                        break;
                }
            }
        }

        if (validSignature) {
            return SignatureStatus.STATUS_VERIFIED;
        } else if (certsNeeded) {
            return SignatureStatus.STATUS_NEED_CERTS;
        } else {
            return SignatureStatus.STATUS_FAILED;
        }
    }

    private SignatureStatus verifySignatureForTrustedAuthority(@NotNull DirectoryServer trustedAuthority, @NotNull DirectorySignature signature) {
        KeyCertificate certificate = trustedAuthority.getCertificateByFingerprint(signature.signingKeyDigest());
        if (certificate == null) {
            log.debug("Missing certificate for signing key: {}", signature.signingKeyDigest());
            addRequiredCertificateForSignature(signature);
            return SignatureStatus.STATUS_NEED_CERTS;
        }
        if (certificate.isExpired()) {
            return SignatureStatus.STATUS_FAILED;
        }

        TorPublicKey signingKey = certificate.getAuthoritySigningKey();
        HexDigest d = (signature.useSha256()) ? signingHash256 : signingHash;
        if (!signingKey.verifySignature(signature.signature(), d)) {
            log.warn("Signature failed on consensus for signing key: {}", signature.signingKeyDigest());
            return SignatureStatus.STATUS_FAILED;
        }
        return SignatureStatus.STATUS_VERIFIED;
    }

    private void addRequiredCertificateForSignature(@NotNull DirectorySignature signature) {
        requiredCertificates.add(new RequiredCertificateImpl(signature.identityDigest(), signature.signingKeyDigest()));
    }

    private int getParameterValue(String name, int defaultValue, int minValue, int maxValue) {
        if (!parameters.containsKey(name)) {
            return defaultValue;
        }
        final int value = parameters.get(name);
        if (value < minValue) {
            return minValue;
        } else return Math.min(value, maxValue);
    }

    private boolean getBooleanParameterValue(String name, boolean defaultValue) {
        if (!parameters.containsKey(name)) {
            return defaultValue;
        }
        final int value = parameters.get(name);
        return value != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConsensusDocumentImpl other)) return false;
        return other.getSigningHash().equals(signingHash);
    }

    @Override
    public int hashCode() {
        return (signingHash == null) ? 0 : signingHash.hashCode();
    }

    @Override
    public String toString() {
        return "ConsensusDocumentImpl{" +
                "requiredCertificates=" + requiredCertificates +
                ", consensusMethod=" + consensusMethod +
                ", clientVersions=" + clientVersions +
                ", serverVersions=" + serverVersions +
                ", knownFlags=" + knownFlags +
                ", signingHash=" + signingHash +
                ", parameters=" + parameters +
                ", signatureCount=" + signatureCount +
                '}';
    }
}