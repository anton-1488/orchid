package com.subgraph.orchid.consensus;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.exitpolicy.ExitPorts;
import com.subgraph.orchid.router.RouterStatus;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class RouterStatusImpl implements RouterStatus {
    private String nickname;
    private HexDigest identity;
    private HexDigest digest;
    private HexDigest microdescriptorDigest;
    private Instant publicationTime;
    private InetAddress address;
    private int routerPort;
    private int directoryPort;
    private final Set<String> flags = new HashSet<>();
    private String version;
    private int bandwidthEstimate;
    private int bandwidthMeasured;
    private boolean hasBandwidth;
    private ExitPorts exitPorts;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setIdentity(HexDigest identity) {
        this.identity = identity;
    }

    public void setDigest(HexDigest digest) {
        this.digest = digest;
    }

    public void setMicrodescriptorDigest(HexDigest digest) {
        this.microdescriptorDigest = digest;
    }

    public void setPublicationTime(Instant timestamp) {
        this.publicationTime = timestamp;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setRouterPort(int port) {
        this.routerPort = port;
    }

    public void setDirectoryPort(int port) {
        this.directoryPort = port;
    }

    public void addFlag(String flag) {
        this.flags.add(flag);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setEstimatedBandwidth(int bandwidth) {
        this.bandwidthEstimate = bandwidth;
        hasBandwidth = true;
    }

    public void setMeasuredBandwidth(int bandwidth) {
        this.bandwidthMeasured = bandwidth;
    }

    public void setAcceptedPorts(String portList) {
        this.exitPorts = ExitPorts.createAcceptExitPorts(portList);
    }

    public void setRejectedPorts(String portList) {
        this.exitPorts = ExitPorts.createRejectExitPorts(portList);
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public HexDigest getIdentity() {
        return identity;
    }

    @Override
    public HexDigest getDescriptorDigest() {
        return digest;
    }

    @Override
    public HexDigest getMicrodescriptorDigest() {
        return microdescriptorDigest;
    }

    @Override
    public Instant getPublicationTime() {
        return publicationTime;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int getRouterPort() {
        return routerPort;
    }

    @Override
    public boolean isDirectory() {
        return directoryPort != 0;
    }

    @Override
    public int getDirectoryPort() {
        return directoryPort;
    }

    @Override
    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean hasBandwidth() {
        return hasBandwidth;
    }

    @Override
    public int getEstimatedBandwidth() {
        return bandwidthEstimate;
    }

    @Override
    public int getMeasuredBandwidth() {
        return bandwidthMeasured;
    }

    @Override
    public ExitPorts getExitPorts() {
        return exitPorts;
    }

    @Override
    public String toString() {
        return String.format("Router: (%s)", String.join(" ", nickname, identity.toString(), digest.toString(), address.toString(), String.valueOf(routerPort), String.valueOf(directoryPort), version, exitPorts.toString()));
    }
}