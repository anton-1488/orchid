package com.subgraph.orchid;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.directory.consensus.DirectorySignature;

import java.net.InetAddress;
import java.util.List;

public interface VoteAuthorityEntry {
    String getNickname();

    HexDigest getIdentity();

    String getHostname();

    InetAddress getAddress();

    int getDirectoryPort();

    int getRouterPort();

    String getContact();

    HexDigest getVoteDigest();

    List<DirectorySignature> getSignatures();
}