package com.subgraph.orchid.directory;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.exitpolicy.ExitPorts;
import com.subgraph.orchid.router.RouterStatus;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class DirectoryAuthorityStatus implements RouterStatus {
    private String nickname;
    private HexDigest identity;
    private InetAddress address;
    private int routerPort;
    private int directoryPort;
    private final Set<String> flags = new HashSet<>();
    private HexDigest v3Ident;

    public void setHiddenServiceAuthority() {
        addFlag("HSDir");
    }

    public void unsetHiddenServiceAuthority() {
        flags.remove("HSDir");
    }

    public void setBridgeAuthority() {
    }

    public void setNickname(String name) {
        nickname = name;
    }

    public void setIdentity(HexDigest identity) {
        this.identity = identity;
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

    public void setV3Ident(HexDigest v3Ident) {
        this.v3Ident = v3Ident;
    }

    public DirectoryAuthorityStatus() {
        addFlag("Authority");
        addFlag("V2Dir");
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public HexDigest getDescriptorDigest() {
        return null;
    }

    @Override
    public int getDirectoryPort() {
        return directoryPort;
    }

    @Override
    public int getEstimatedBandwidth() {
        return 0;
    }

    @Override
    public ExitPorts getExitPorts() {
        return null;
    }

    @Override
    public HexDigest getIdentity() {
        return identity;
    }

    @Override
    public boolean hasBandwidth() {
        return false;
    }

    @Override
    public int getMeasuredBandwidth() {
        return 0;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public Instant getPublicationTime() {
        return null;
    }

    @Override
    public int getRouterPort() {
        return routerPort;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public HexDigest getMicrodescriptorDigest() {
        return null;
    }

    public HexDigest getV3Ident() {
        return v3Ident;
    }
}