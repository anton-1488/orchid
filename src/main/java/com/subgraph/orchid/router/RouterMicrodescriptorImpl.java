package com.subgraph.orchid.router;

import com.subgraph.orchid.Tor;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.exitpolicy.ExitPorts;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RouterMicrodescriptorImpl implements RouterMicrodescriptor {
    private InetAddress address;
    private int routerPort;
    private TorPublicKey onionKey;
    private byte[] ntorOnionKey;
    private Set<String> familyMembers = Collections.emptySet();
    private ExitPorts acceptPorts;
    private ExitPorts rejectPorts;
    private String rawDocumentData;
    private HexDigest descriptorDigest;
    private long lastListed;
    private CacheLocation cacheLocation = CacheLocation.NOT_CACHED;

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setRouterPort(int port) {
        this.routerPort = port;
    }

    public void setOnionKey(TorPublicKey onionKey) {
        this.onionKey = onionKey;
    }

    public void setNtorOnionKey(byte[] ntorOnionKey) {
        this.ntorOnionKey = ntorOnionKey;
    }

    public void addFamilyMember(String familyMember) {
        if (familyMembers.isEmpty()) {
            familyMembers = new HashSet<>();
        }
        familyMembers.add(familyMember);
    }

    public void addAcceptPorts(String portlist) {
        acceptPorts = ExitPorts.createAcceptExitPorts(portlist);
    }

    public void addRejectPorts(String portlist) {
        rejectPorts = ExitPorts.createRejectExitPorts(portlist);
    }

    public void setRawDocumentData(String rawData) {
        this.rawDocumentData = rawData;
    }

    public void setDescriptorDigest(HexDigest descriptorDigest) {
        this.descriptorDigest = descriptorDigest;
    }

    @Override
    public void setLastListed(long ts) {
        this.lastListed = ts;
    }

    @Override
    public boolean isValidDocument() {
        return (descriptorDigest != null) && (onionKey != null);
    }

    @Override
    public String getRawDocumentData() {
        return rawDocumentData;
    }

    @Override
    public TorPublicKey getOnionKey() {
        return onionKey;
    }

    @Override
    public byte[] getNTorOnionKey() {
        return ntorOnionKey;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int getRouterPort() {
        return routerPort;
    }

    public Set<String> getFamilyMembers() {
        return new HashSet<>(familyMembers);
    }

    @Override
    public boolean exitPolicyAccepts(InetAddress address, int port) {
        return exitPolicyAccepts(port);
    }

    @Override
    public boolean exitPolicyAccepts(int port) {
        if (acceptPorts == null) {
            return false;
        }
        if (rejectPorts != null && !rejectPorts.acceptsPort(port)) {
            return false;
        }
        return acceptPorts.acceptsPort(port);
    }

    @Override
    public HexDigest getDescriptorDigest() {
        return descriptorDigest;
    }

    @Override
    public long getLastListed() {
        return lastListed;
    }

    @Override
    public void setCacheLocation(CacheLocation location) {
        this.cacheLocation = location;
    }

    @Override
    public CacheLocation getCacheLocation() {
        return cacheLocation;
    }

    @Override
    public int getBodyLength() {
        return rawDocumentData.length();
    }

    @Override
    public String getVersion() {
        //FIXME: What version?
        return "3";
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
    public boolean equals(Object o) {
        if (!(o instanceof RouterMicrodescriptorImpl other)) {
            return false;
        }
        if (other.getDescriptorDigest() == null || descriptorDigest == null) {
            return false;
        }

        return other.getDescriptorDigest().equals(descriptorDigest);
    }

    @Override
    public int hashCode() {
        if (descriptorDigest == null) {
            return 0;
        }
        return descriptorDigest.hashCode();
    }

    @Override
    public String toString() {
        return "RouterMicrodescriptorImpl{" +
                "address=" + address +
                ", routerPort=" + routerPort +
                ", familyMembers=" + familyMembers +
                ", acceptPorts=" + acceptPorts +
                ", rejectPorts=" + rejectPorts +
                ", descriptorDigest=" + descriptorDigest +
                ", lastListed=" + lastListed +
                '}';
    }
}