package com.subgraph.orchid.downloader;

import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.downloader.request.TorRequest;
import com.subgraph.orchid.parsing.DocumentParser;

import java.nio.ByteBuffer;

public class ConsensusFetcher extends DocumentFetcher<ConsensusDocument> {
    private final static String CONSENSUS_BASE_PATH = "/tor/status-vote/current/";
    private final boolean useMicrodescriptors;

    public ConsensusFetcher(boolean useMicrodescriptors) {
        this.useMicrodescriptors = useMicrodescriptors;
    }

    @Override
    public TorRequest getRequest() {
        return TorRequest.get(CONSENSUS_BASE_PATH + ((useMicrodescriptors) ? ("consensus-microdesc") : ("consensus")));
    }

    @Override
    DocumentParser<ConsensusDocument> createParser(ByteBuffer response) {
        return PARSER_FACTORY.createConsensusDocumentParser(response);
    }
}