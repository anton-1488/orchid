package com.subgraph.orchid.downloader;

import java.nio.ByteBuffer;

import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.parsing.DocumentParser;

public class BridgeDescriptorFetcher extends DocumentFetcher<RouterDescriptor>{

	@Override
	String getRequestPath() {
		return "/tor/server/authority";
	}

	@Override
	DocumentParser<RouterDescriptor> createParser(ByteBuffer response) {
		return PARSER_FACTORY.createRouterDescriptorParser(response, true);
	}
}
