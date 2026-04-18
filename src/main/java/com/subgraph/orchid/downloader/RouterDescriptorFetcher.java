package com.subgraph.orchid.downloader;

import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.downloader.request.TorRequest;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.router.RouterDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RouterDescriptorFetcher extends DocumentFetcher<RouterDescriptor> {
    private final List<HexDigest> fingerprints;

    public RouterDescriptorFetcher(Collection<HexDigest> fingerprints) {
        this.fingerprints = new ArrayList<>(fingerprints);
    }

    @Override
    TorRequest getRequest() {
        return TorRequest.get("/tor/server/d/" + fingerprintsToRequestString());
    }

    private @NotNull String fingerprintsToRequestString() {
        StringBuilder sb = new StringBuilder();
        for (HexDigest fp : fingerprints) {
            appendFingerprint(sb, fp);
        }
        return sb.toString();
    }

    private void appendFingerprint(@NotNull StringBuilder sb, HexDigest fp) {
        if (sb.isEmpty()) {
            sb.append("+");
        }
        sb.append(fp.toString());
    }

    @Override
    DocumentParser<RouterDescriptor> createParser(ByteBuffer response) {
        return PARSER_FACTORY.createRouterDescriptorParser(response, true);
    }
}