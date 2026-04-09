package com.subgraph.orchid.circuits.bridge;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;

import com.subgraph.orchid.Descriptor;
import com.subgraph.orchid.directory.router.RouterDescriptor;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.geoip.CountryCodeService;

public class BridgeRouterImpl implements BridgeRouter {
    private final InetAddress address;
    private final int port;

    private HexDigest identity;
    private RouterDescriptor descriptor;

    private volatile String cachedCountryCode;

    BridgeRouterImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public HexDigest getIdentity() {
        return identity;
    }

    @Override
    public void setIdentity(HexDigest identity) {
        this.identity = identity;
    }

    @Override
    public void setDescriptor(RouterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public RouterDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getNickname() {
        return toString();
    }

    @Override
    public String getCountryCode() {
        return CountryCodeService.getInstance().getCountryCodeForAddress(getAddress());
    }

    @Override
    public int getOnionPort() {
        return port;
    }

    @Override
    public int getDirectoryPort() {
        return 0;
    }

    @Override
    public TorPublicKey getIdentityKey() {
        return descriptor != null ? descriptor.getIdentityKey() : null;
    }

    @Override
    public HexDigest getIdentityHash() {
        return identity;
    }

    @Override
    public boolean isDescriptorDownloadable() {
        return false;
    }

    @Override
    public String getVersion() {
        return descriptor != null ? descriptor.getVersion() : "";
    }

    @Override
    public Descriptor getCurrentDescriptor() {
        return descriptor;
    }

    @Override
    public HexDigest getDescriptorDigest() {
        return descriptor != null ? descriptor.getDescriptorDigest() : null;
    }

    @Override
    public HexDigest getMicrodescriptorDigest() {
        return null;
    }

    @Override
    public TorPublicKey getOnionKey() {
        return descriptor != null ? descriptor.getOnionKey() : null;
    }

    @Override
    public byte[] getNTorOnionKey() {
        return descriptor != null ? descriptor.getNTorOnionKey() : null;
    }

    @Override
    public boolean hasBandwidth() {
        return false;
    }

    @Override
    public int getEstimatedBandwidth() {
        return 0;
    }

    @Override
    public int getMeasuredBandwidth() {
        return 0;
    }

    @Override
    public Set<String> getFamilyMembers() {
        return descriptor != null ? descriptor.getFamilyMembers() : Collections.emptySet();
    }

    @Override
    public int getAverageBandwidth() {
        return 0;
    }

    @Override
    public int getBurstBandwidth() {
        return 0;
    }

    @Override
    public int getObservedBandwidth() {
        return 0;
    }

    @Override
    public boolean isHibernating() {
        return descriptor != null && descriptor.isHibernating();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isBadExit() {
        return false;
    }

    @Override
    public boolean isPossibleGuard() {
        return true;
    }

    @Override
    public boolean isExit() {
        return false;
    }

    @Override
    public boolean isFast() {
        return true;
    }

    @Override
    public boolean isStable() {
        return true;
    }

    @Override
    public boolean isHSDirectory() {
        return false;
    }

    @Override
    public boolean exitPolicyAccepts(InetAddress address, int port) {
        return false;
    }

    @Override
    public boolean exitPolicyAccepts(int port) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BridgeRouterImpl other)) return false;
        return port == other.port && address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return 31 * address.hashCode() + port;
    }

    public String toString() {
        return String.format("[Bridge %s:%d]", address, port);
    }
}
