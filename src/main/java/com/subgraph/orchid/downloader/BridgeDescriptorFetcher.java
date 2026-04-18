package com.subgraph.orchid.downloader;

import com.subgraph.orchid.downloader.request.TorRequest;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.router.RouterDescriptor;

import java.nio.ByteBuffer;

public class BridgeDescriptorFetcher extends DocumentFetcher<RouterDescriptor> {
    @Override
    public TorRequest getRequest() {
        return TorRequest.BRIDGE_DESCRIPTION;
    }

    @Override
    DocumentParser<RouterDescriptor> createParser(ByteBuffer response) {
        return PARSER_FACTORY.createRouterDescriptorParser(response, true);
    }
}