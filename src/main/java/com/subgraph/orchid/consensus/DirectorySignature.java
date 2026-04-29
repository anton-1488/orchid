package com.subgraph.orchid.consensus;

import com.subgraph.orchid.crypto.TorSignature;
import com.subgraph.orchid.data.HexDigest;

public record DirectorySignature(HexDigest identityDigest, HexDigest signingKeyDigest, TorSignature signature, boolean useSha256) {
}